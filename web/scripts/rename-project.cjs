#!/usr/bin/env node

/**
 * 项目一键改名工具。
 *
 * 支持项目身份、数据库名、端口、Java 根包名和 Spring Boot 启动类迁移。
 * 写入范围保持在白名单内，避免全仓库盲目替换。
 */

const fs = require('fs');
const path = require('path');

const webDir = path.resolve(__dirname, '..');
const rootDir = path.resolve(webDir, '..');
const javaSourceRoots = ['server/src/main/java', 'server/src/test/java'];
const textFiles = [
  'README.md',
  'web/README.md',
  'server/README.md',
  'server/pom.xml',
  'server/src/main/resources/application.yml',
  'server/src/main/resources/application-dev.yml',
  'server/application-prod.yml.example',
];
const envFiles = ['.env.development', '.env.production'];

function usage() {
  console.log(`
用法:
  npm run rename-project -- --slug <project-slug> --title <项目名称> --java-package <新Java包名> --main-class <新启动类名> [选项]

示例:
  npm run rename-project -- --slug hospital-revisit --title 医院随访管理 --db hospital_revisit --java-package com.hosp.revi --main-class HospReviApplication --backend-port 8081 --frontend-port 7053 --dry-run
  npm run rename-project -- --slug property-manage --title 物业管理平台 --java-package com.acme.property --main-class PropertyManageApplication

必填:
  --slug                 新项目英文标识，kebab-case，例如 hospital-revisit
  --title                新项目显示名称，例如 医院随访管理

Java 改名:
  --java-package         新 Java 根包名，例如 com.hosp.revi
  --main-class           新 Spring Boot 启动类名，例如 HospReviApplication
  --from-java-package    当前 Java 根包名，默认 com.zhoolg.manage
  --from-main-class      当前启动类名，默认 ManageServerApplication

常用选项:
  --short-title          应用简称，默认等于 --title
  --description          应用描述，默认等于 "<title>后台管理系统"
  --db                   MySQL 数据库名，默认由 slug 转 snake_case
  --artifact             后端 Maven artifactId，默认 "<slug>-server"
  --server-name          Spring application name，默认等于 artifact
  --frontend-port        Vite 端口，例如 7053
  --backend-port         Spring Boot 端口，例如 8081
  --repository           package.json repository.url
  --from-slug            当前项目 slug，默认读取 package.json name
  --from-db              当前数据库名，默认 svelte_manage_web
  --from-artifact        当前后端 artifactId，默认 "<from-slug>-server"
  --dry-run              只预览，不写入
  --help                 显示帮助
`);
}

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (!arg.startsWith('--')) throw new Error(`未知参数: ${arg}`);
    const key = arg.slice(2);
    if (key === 'dry-run' || key === 'help') {
      args[key] = true;
      continue;
    }
    const value = argv[++i];
    if (!value || value.startsWith('--')) throw new Error(`参数 --${key} 缺少值`);
    args[key] = value;
  }
  return args;
}

function abs(relativePath) {
  return path.join(rootDir, relativePath);
}

function webRelative(relativePath) {
  return path.posix.join('web', relativePath);
}

function exists(relativePath) {
  return fs.existsSync(abs(relativePath));
}

function readText(relativePath) {
  return fs.readFileSync(abs(relativePath), 'utf8');
}

function writeText(relativePath, content, dryRun) {
  if (!dryRun) fs.writeFileSync(abs(relativePath), content, 'utf8');
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function writeJson(relativePath, value, dryRun) {
  writeText(relativePath, JSON.stringify(value, null, 2) + '\n', dryRun);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function replaceAll(content, from, to) {
  if (!from || from === to) return content;
  return content.replace(new RegExp(escapeRegExp(from), 'g'), to);
}

function dottedToPath(packageName) {
  return packageName.split('.').join('/');
}

function toSnakeCase(value) {
  return value.replace(/-/g, '_');
}

function setEnvValue(content, key, value) {
  const line = `${key}=${value}`;
  const pattern = new RegExp(`^${escapeRegExp(key)}=.*$`, 'm');
  if (pattern.test(content)) return content.replace(pattern, line);
  return `${content}${content.endsWith('\n') ? '' : '\n'}${line}\n`;
}

function listFiles(relativeDir, predicate = () => true) {
  const root = abs(relativeDir);
  if (!fs.existsSync(root)) return [];
  const result = [];
  const walk = dir => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(fullPath);
      } else if (predicate(fullPath)) {
        result.push(path.relative(rootDir, fullPath).replace(/\\/g, '/'));
      }
    }
  };
  walk(root);
  return result;
}

function validateConfig(config) {
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(config.slug)) {
    throw new Error('--slug 必须是 kebab-case，只能包含小写字母、数字和连字符');
  }
  if (!config.title.trim()) throw new Error('--title 不能为空');
  if (!/^[A-Za-z0-9_]+$/.test(config.db)) throw new Error('--db 只能包含字母、数字和下划线');
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(config.artifact))
    throw new Error('--artifact 必须是 kebab-case');
  if (config.javaPackage && !/^[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+$/.test(config.javaPackage)) {
    throw new Error('--java-package 必须是合法 Java 包名，例如 com.hosp.revi');
  }
  if (config.mainClass && !/^[A-Z][A-Za-z0-9_]*$/.test(config.mainClass)) {
    throw new Error('--main-class 必须是合法 Java 类名，且建议首字母大写');
  }
  if (config.frontendPort && !/^\d{2,5}$/.test(config.frontendPort))
    throw new Error('--frontend-port 必须是端口数字');
  if (config.backendPort && !/^\d{2,5}$/.test(config.backendPort))
    throw new Error('--backend-port 必须是端口数字');
}

function buildConfig(args) {
  if (args.help) {
    usage();
    process.exit(0);
  }

  const packageJson = readJson(webRelative('package.json'));
  const fromSlug = args['from-slug'] || packageJson.name || 'svelte-manage-web';
  if (!args.slug || !args.title) {
    usage();
    throw new Error('缺少必填参数 --slug 或 --title');
  }

  const artifact = args.artifact || `${args.slug}-server`;
  const config = {
    slug: args.slug,
    title: args.title,
    shortTitle: args['short-title'] || args.title,
    description: args.description || `${args.title}后台管理系统`,
    copyrightOwner: args['copyright-owner'] || args.title,
    db: args.db || toSnakeCase(args.slug),
    artifact,
    serverName: args['server-name'] || artifact,
    repository: args.repository || '',
    frontendPort: args['frontend-port'] || '',
    backendPort: args['backend-port'] || '',
    fromSlug,
    fromDb: args['from-db'] || 'svelte_manage_web',
    fromArtifact: args['from-artifact'] || `${fromSlug}-server`,
    fromJavaPackage: args['from-java-package'] || 'com.zhoolg.manage',
    javaPackage: args['java-package'] || '',
    fromMainClass: args['from-main-class'] || 'ManageServerApplication',
    mainClass: args['main-class'] || '',
    dryRun: Boolean(args['dry-run']),
  };
  validateConfig(config);
  return config;
}

function updatePackageJson(config, changed) {
  const relativePath = webRelative('package.json');
  const json = readJson(relativePath);
  const before = JSON.stringify(json);

  json.name = config.slug;
  if (config.repository.trim()) {
    json.repository = { type: 'git', url: config.repository.trim() };
  } else if (json.repository?.url) {
    json.repository.url = replaceAll(json.repository.url, config.fromSlug, config.slug);
  }

  if (JSON.stringify(json) !== before) {
    writeJson(relativePath, json, config.dryRun);
    changed.push(relativePath);
  }
}

function updatePackageLock(config, changed) {
  const relativePath = webRelative('package-lock.json');
  if (!exists(relativePath)) return;
  const json = readJson(relativePath);
  const before = JSON.stringify(json);

  json.name = config.slug;
  if (json.packages?.['']) json.packages[''].name = config.slug;

  if (JSON.stringify(json) !== before) {
    writeJson(relativePath, json, config.dryRun);
    changed.push(relativePath);
  }
}

function updateTextFiles(config, changed) {
  const fromPackagePath = dottedToPath(config.fromJavaPackage);
  const toPackagePath = config.javaPackage ? dottedToPath(config.javaPackage) : fromPackagePath;

  for (const relativePath of textFiles) {
    if (!exists(relativePath)) continue;
    let content = readText(relativePath);
    const before = content;

    content = replaceAll(content, config.fromArtifact, config.artifact);
    content = replaceAll(content, config.fromSlug, config.slug);
    content = replaceAll(content, config.fromDb, config.db);
    if (config.javaPackage) {
      content = replaceAll(content, config.fromJavaPackage, config.javaPackage);
      content = replaceAll(content, fromPackagePath, toPackagePath);
    }
    if (config.mainClass) {
      content = replaceAll(content, config.fromMainClass, config.mainClass);
    }

    if (relativePath === 'server/pom.xml') {
      content = content.replace(
        /<description>REST API and metadata-driven backend for .*?<\/description>/,
        `<description>REST API and metadata-driven backend for ${config.slug}</description>`
      );
    }
    if (relativePath === 'server/src/main/resources/application.yml') {
      content = content.replace(/(^\s*name:\s*).+$/m, `$1${config.serverName}`);
      if (config.backendPort)
        content = content.replace(/(^\s*port:\s*)\d+$/m, `$1${config.backendPort}`);
    }

    if (content !== before) {
      writeText(relativePath, content, config.dryRun);
      changed.push(relativePath);
    }
  }
}

function updateEnvFiles(config, changed) {
  for (const envPath of envFiles) {
    const relativePath = webRelative(envPath);
    if (!exists(relativePath)) continue;
    let content = readText(relativePath);
    const before = content;

    content = setEnvValue(content, 'VITE_APP_TITLE', config.title);
    content = setEnvValue(content, 'VITE_APP_SHORT_TITLE', config.shortTitle);
    content = setEnvValue(content, 'VITE_APP_DESCRIPTION', config.description);
    content = setEnvValue(content, 'VITE_APP_COPYRIGHT_OWNER', config.copyrightOwner);
    if (config.frontendPort) content = setEnvValue(content, 'VITE_PORT', config.frontendPort);
    if (envPath === '.env.development' && config.backendPort) {
      content = setEnvValue(
        content,
        'VITE_APP_TARGET_URL',
        `http://localhost:${config.backendPort}`
      );
    }

    if (content !== before) {
      writeText(relativePath, content, config.dryRun);
      changed.push(relativePath);
    }
  }
}

function updateJavaSources(config, changed) {
  if (!config.javaPackage && !config.mainClass) return;

  const fromPackagePath = dottedToPath(config.fromJavaPackage);
  const toPackagePath = config.javaPackage ? dottedToPath(config.javaPackage) : fromPackagePath;

  for (const root of javaSourceRoots) {
    for (const relativePath of listFiles(root, file => file.endsWith('.java'))) {
      let content = readText(relativePath);
      const before = content;

      if (config.javaPackage) {
        content = replaceAll(content, config.fromJavaPackage, config.javaPackage);
        content = replaceAll(content, fromPackagePath, toPackagePath);
      }
      if (config.mainClass) {
        content = replaceAll(content, config.fromMainClass, config.mainClass);
      }

      if (content !== before) {
        writeText(relativePath, content, config.dryRun);
        changed.push(relativePath);
      }
    }
  }
}

function ensureMoveIsSafe(source, target) {
  const from = path.resolve(source);
  const to = path.resolve(target);
  if (from === to) return false;
  if (to.startsWith(from + path.sep) || from.startsWith(to + path.sep)) {
    throw new Error(`拒绝嵌套移动 Java 包目录: ${from} -> ${to}`);
  }
  if (!fs.existsSync(from)) return false;
  if (fs.existsSync(to)) {
    throw new Error(`目标 Java 包目录已存在，请先处理后重试: ${to}`);
  }
  return true;
}

function removeEmptyParents(startDir, stopDir) {
  let current = startDir;
  const stop = path.resolve(stopDir);
  while (path.resolve(current).startsWith(stop) && path.resolve(current) !== stop) {
    if (!fs.existsSync(current) || fs.readdirSync(current).length > 0) break;
    fs.rmdirSync(current);
    current = path.dirname(current);
  }
}

function moveJavaPackageDirectories(config, changed) {
  if (!config.javaPackage || config.javaPackage === config.fromJavaPackage) return;

  for (const root of javaSourceRoots) {
    const source = abs(path.join(root, dottedToPath(config.fromJavaPackage)));
    const target = abs(path.join(root, dottedToPath(config.javaPackage)));
    if (!ensureMoveIsSafe(source, target)) continue;

    changed.push(
      `${path.relative(rootDir, source).replace(/\\/g, '/')} -> ${path.relative(rootDir, target).replace(/\\/g, '/')}`
    );
    if (config.dryRun) continue;

    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.renameSync(source, target);
    removeEmptyParents(path.dirname(source), abs(root));
  }
}

function renameMainClassFiles(config, changed) {
  if (!config.mainClass || config.mainClass === config.fromMainClass) return;

  const sourcePackageName =
    config.dryRun && config.javaPackage
      ? config.fromJavaPackage
      : config.javaPackage || config.fromJavaPackage;
  const targetPackageName = config.javaPackage || config.fromJavaPackage;
  const sourcePackagePath = dottedToPath(sourcePackageName);
  const targetPackagePath = dottedToPath(targetPackageName);

  for (const root of javaSourceRoots) {
    const sourceDir = abs(path.join(root, sourcePackagePath));
    if (!fs.existsSync(sourceDir)) continue;

    for (const entry of fs.readdirSync(sourceDir, { withFileTypes: true })) {
      if (
        !entry.isFile() ||
        !entry.name.endsWith('.java') ||
        !entry.name.startsWith(config.fromMainClass)
      ) {
        continue;
      }
      const nextName = entry.name.replace(config.fromMainClass, config.mainClass);
      const source = path.join(sourceDir, entry.name);
      const target = abs(path.join(root, targetPackagePath, nextName));
      if (!config.dryRun && fs.existsSync(target))
        throw new Error(`目标启动类文件已存在: ${target}`);

      changed.push(
        `${path.relative(rootDir, source).replace(/\\/g, '/')} -> ${path.relative(rootDir, target).replace(/\\/g, '/')}`
      );
      if (!config.dryRun) fs.renameSync(source, target);
    }
  }
}

function main() {
  try {
    const args = parseArgs(process.argv.slice(2));
    const config = buildConfig(args);
    const changed = [];

    updatePackageJson(config, changed);
    updatePackageLock(config, changed);
    updateTextFiles(config, changed);
    updateEnvFiles(config, changed);
    updateJavaSources(config, changed);
    moveJavaPackageDirectories(config, changed);
    renameMainClassFiles(config, changed);

    console.log(`\n${config.dryRun ? '预览完成，未写入文件' : '改名完成'}`);
    console.log(`项目标识: ${config.fromSlug} -> ${config.slug}`);
    console.log(`显示名称: ${config.title}`);
    console.log(`数据库名: ${config.fromDb} -> ${config.db}`);
    console.log(`后端 artifact: ${config.fromArtifact} -> ${config.artifact}`);
    if (config.javaPackage)
      console.log(`Java 包名: ${config.fromJavaPackage} -> ${config.javaPackage}`);
    if (config.mainClass) console.log(`启动类名: ${config.fromMainClass} -> ${config.mainClass}`);
    if (config.frontendPort) console.log(`前端端口: ${config.frontendPort}`);
    if (config.backendPort) console.log(`后端端口: ${config.backendPort}`);

    console.log('\n影响文件/目录:');
    if (changed.length === 0) {
      console.log('  无变更');
    } else {
      for (const file of changed) console.log(`  - ${file}`);
    }

    if (!config.dryRun) {
      console.log('\n建议验证:');
      console.log('  cd web');
      console.log('  npm run check');
      console.log('  npm run server:build');
    }
  } catch (error) {
    console.error(`\n改名失败: ${error.message}`);
    console.error('运行 npm run rename-project -- --help 查看用法。');
    process.exit(1);
  }
}

main();
