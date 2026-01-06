#!/usr/bin/env node

/**
 * 交互式模块删除工具
 * 用法: npm run remove-module
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function question(query) {
  return new Promise(resolve => rl.question(query, resolve));
}

// 将 kebab-case 转换为 camelCase
function toCamelCase(str) {
  return str.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
}

// 将 kebab-case 转换为 PascalCase
function toPascalCase(str) {
  const camel = toCamelCase(str);
  return camel.charAt(0).toUpperCase() + camel.slice(1);
}

// 检查文件是否存在
function fileExists(filePath) {
  return fs.existsSync(filePath);
}

// 删除文件
function deleteFile(filePath) {
  if (fileExists(filePath)) {
    fs.unlinkSync(filePath);
    return true;
  }
  return false;
}

// 扫描 App.svelte 查找组件引用
function findComponentReferences(componentName, filePath) {
  if (!fileExists(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const references = [];

  // 查找 import 语句
  const importRegex = new RegExp(`import\\s+${componentName}\\s+from`, 'g');
  if (importRegex.test(content)) {
    references.push('import');
  }

  // 查找组件使用
  const usageRegex = new RegExp(`customPage\\s*===\\s*['"]${componentName}['"]`, 'g');
  if (usageRegex.test(content)) {
    references.push('usage');
  }

  return references;
}

// 扫描 menu-structure.config.ts 查找菜单引用
function findMenuReferences(moduleId, filePath) {
  if (!fileExists(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const references = [];

  // 查找作为一级菜单
  const topLevelRegex = new RegExp(`{\\s*id:\\s*['"]${moduleId}['"]`, 'g');
  if (topLevelRegex.test(content)) {
    references.push('top-level');
  }

  // 查找作为子菜单
  const childRegex = new RegExp(`['"]${moduleId}['"]`, 'g');
  const matches = content.match(childRegex);
  if (matches && matches.length > 0) {
    references.push('child');
  }

  return references;
}

/**
 * 从菜单结构中删除模块引用
 * @param {string} moduleId - 模块ID
 * @param {string} filePath - 菜单配置文件路径
 * @returns {Object} 删除结果
 */
function removeMenuReferences(moduleId, filePath) {
  if (!fileExists(filePath)) {
    return { updated: false, removedTopLevel: false, removedChildren: false, error: '菜单文件不存在' };
  }

  let content = fs.readFileSync(filePath, 'utf-8');
  const originalContent = content;
  let removedTopLevel = false;
  let removedChildren = false;

  // 1. 删除一级菜单对象块（包括可能的注释和多种格式）
  // 匹配: { id: 'module-name' }, 或 { id: 'module-name', children: [...] },
  const topLevelPattern = new RegExp(
    `\\n?\\s*\\/\\/[^\\n]*\\n?\\s*\\{[^}]*\\bid\\s*:\\s*['"]${moduleId}['"][^}]*\\}\\s*,?`,
    'g'
  );

  content = content.replace(topLevelPattern, (match) => {
    removedTopLevel = true;
    console.log(`  删除一级菜单: { id: '${moduleId}' }`);
    return '';
  });

  // 2. 从 children 数组中删除模块ID
  const childrenPattern = /(children\s*:\s*\[)([^\]]*?)(\])/g;

  content = content.replace(childrenPattern, (match, start, body, end) => {
    // 解析 children 数组内容
    const items = body
      .split(',')
      .map(item => item.trim())
      .filter(item => item.length > 0);

    // 过滤掉目标模块ID
    const filtered = items.filter(item => {
      const cleaned = item.replace(/^['"]|['"]$/g, '');
      return cleaned !== moduleId;
    });

    // 如果有变化,说明删除了
    if (filtered.length < items.length) {
      removedChildren = true;
      console.log(`  从 children 数组删除: '${moduleId}'`);
    }

    // 重新构建 children 数组
    if (filtered.length === 0) {
      return `children: []`;
    }

    // 保持原有的缩进风格
    const indentMatch = match.match(/(\n\s+)/);
    const indent = indentMatch ? indentMatch[1] : '\n    ';
    const rebuiltBody = filtered.join(`,${indent}`);

    return `${start}${rebuiltBody}${end}`;
  });

  // 3. 清理可能产生的多余空行
  content = content.replace(/\n\n\n+/g, '\n\n');

  // 写回文件
  if (content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf-8');
    return { updated: true, removedTopLevel, removedChildren };
  }

  return { updated: false, removedTopLevel, removedChildren };
}

/**
 * 从对象中删除指定的键值对
 * @param {string} content - 文件内容
 * @param {string} key - 要删除的键名
 * @param {string} context - 上下文(如 'menu' 表示在 menu 对象内)
 * @returns {Object} 处理结果
 */
function removeObjectProperty(content, key, context = null) {
  let updated = content;
  let removed = false;

  if (context) {
    // 在指定对象内删除属性(如 menu.moduleName)
    const contextPattern = new RegExp(
      `(${context}\\s*:\\s*\\{[^}]*?)(\\n\\s*${key}\\s*:\\s*[^,\\n]+,?)`,
      's'
    );

    updated = updated.replace(contextPattern, (match, before, prop) => {
      removed = true;
      console.log(`  删除 ${context}.${key}`);
      return before;
    });
  } else {
    // 删除顶层对象块(如整个 moduleName: { ... } 对象)
    // 使用括号计数来正确匹配嵌套对象
    const keyPattern = new RegExp(`(^|\\n)(\\s*)${key}\\s*:\\s*\\{`, 'm');
    const match = keyPattern.exec(updated);

    if (match) {
      const blockStart = match.index + match[0].length - 1;
      let depth = 0;
      let blockEnd = -1;

      // 从 { 开始计数括号
      for (let i = blockStart; i < updated.length; i++) {
        if (updated[i] === '{') {
          depth++;
        } else if (updated[i] === '}') {
          depth--;
          if (depth === 0) {
            blockEnd = i;
            break;
          }
        }
      }

      if (blockEnd !== -1) {
        // 包括对象后面的逗号和换行
        let removalEnd = blockEnd + 1;
        while (removalEnd < updated.length && /[\s,]/.test(updated[removalEnd])) {
          removalEnd++;
        }

        const removalStart = match.index + (match[1] ? 1 : 0);
        updated = updated.slice(0, removalStart) + updated.slice(removalEnd);
        removed = true;
        console.log(`  删除对象块: ${key}`);
      }
    }
  }

  return { content: updated, removed };
}

/**
 * 从国际化文件中删除翻译条目
 * @param {string} filePath - 翻译文件路径
 * @param {string} moduleKey - 模块键名(camelCase)
 * @returns {Object} 删除结果
 */
function removeI18nEntries(filePath, moduleKey) {
  if (!fileExists(filePath)) {
    return { updated: false, removedMenu: false, removedModule: false, error: '翻译文件不存在' };
  }

  let content = fs.readFileSync(filePath, 'utf-8');
  const originalContent = content;
  let removedMenu = false;
  let removedModule = false;

  // 1. 从 menu 对象中删除 menu.moduleKey 属性
  const menuResult = removeObjectProperty(content, moduleKey, 'menu');
  content = menuResult.content;
  removedMenu = menuResult.removed;

  // 2. 删除顶层的 moduleKey 对象
  const moduleResult = removeObjectProperty(content, moduleKey);
  content = moduleResult.content;
  removedModule = moduleResult.removed;

  // 3. 清理多余空行
  content = content.replace(/\n\n\n+/g, '\n\n');

  // 写回文件
  if (content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf-8');
    return { updated: true, removedMenu, removedModule };
  }

  return { updated: false, removedMenu, removedModule };
}

// 列出所有可用的模块
function listAvailableModules() {
  const modulesDir = path.join(__dirname, '../src/config/modules');
  if (!fs.existsSync(modulesDir)) {
    return [];
  }

  const files = fs.readdirSync(modulesDir);
  return files
    .filter(file => file.endsWith('.config.ts'))
    .map(file => file.replace('.config.ts', ''))
    .sort();
}

// 主函数
async function main() {
  console.log('\n🗑️  模块删除工具（交互式）\n');

  try {
    // 1. 列出所有可用的模块
    const availableModules = listAvailableModules();

    if (availableModules.length === 0) {
      console.log('❌ 没有找到任何模块配置文件');
      rl.close();
      return;
    }

    console.log('📋 当前已有的模块:\n');
    availableModules.forEach((module, index) => {
      console.log(`${index + 1}. ${module}`);
    });

    console.log('\n───────────────────────────────\n');

    // 2. 输入要删除的模块名称或序号
    const input = await question('📝 输入要删除的模块名称或序号: ');

    if (!input || !input.trim()) {
      console.log('❌ 输入不能为空');
      rl.close();
      return;
    }

    // 判断输入是序号还是模块名称
    let cleanModuleName;
    const inputTrimmed = input.trim();
    const inputNumber = parseInt(inputTrimmed);

    if (!isNaN(inputNumber) && inputNumber >= 1 && inputNumber <= availableModules.length) {
      // 输入的是序号
      cleanModuleName = availableModules[inputNumber - 1];
      console.log(`\n✓ 已选择: ${cleanModuleName} (序号 ${inputNumber})`);
    } else if (availableModules.includes(inputTrimmed)) {
      // 输入的是模块名称
      cleanModuleName = inputTrimmed;
      console.log(`\n✓ 已选择: ${cleanModuleName}`);
    } else {
      console.log(`❌ 无效的输入: "${inputTrimmed}"`);
      console.log(`   请输入 1-${availableModules.length} 的序号，或者正确的模块名称`);
      rl.close();
      return;
    }

    const camelName = toCamelCase(cleanModuleName);
    const pascalName = toPascalCase(cleanModuleName);

    // 3. 检查要删除的文件
    const filesToCheck = {
      config: path.join(__dirname, `../src/config/modules/${cleanModuleName}.config.ts`),
      component: path.join(__dirname, `../src/components/${pascalName}.svelte`),
    };

    const filesToDelete = [];
    const warnings = [];

    console.log('\n📋 检查文件...\n');

    // 检查配置文件
    if (fileExists(filesToCheck.config)) {
      filesToDelete.push({
        path: filesToCheck.config,
        type: 'config',
        name: '模块配置文件',
        relativePath: `src/config/modules/${cleanModuleName}.config.ts`,
      });
      console.log(`✓ 找到配置文件: ${cleanModuleName}.config.ts`);
    } else {
      console.log(`⚠️  配置文件不存在: ${cleanModuleName}.config.ts`);
    }

    // 检查组件文件
    if (fileExists(filesToCheck.component)) {
      filesToDelete.push({
        path: filesToCheck.component,
        type: 'component',
        name: '自定义组件文件',
        relativePath: `src/components/${pascalName}.svelte`,
      });
      console.log(`✓ 找到组件文件: ${pascalName}.svelte`);
    }

    // 检查 App.svelte 中的引用
    const appPath = path.join(__dirname, '../src/App.svelte');
    const componentRefs = findComponentReferences(pascalName, appPath);
    if (componentRefs.length > 0) {
      warnings.push({
        file: 'App.svelte',
        type: 'component-reference',
        message: `需要手动删除 ${pascalName} 组件的 import 和使用`,
      });
    }

    // 检查 menu-structure.config.ts 中的引用
    const menuPath = path.join(__dirname, '../src/config/menu-structure.config.ts');
    const menuRefs = findMenuReferences(cleanModuleName, menuPath);
    if (menuRefs.length > 0) {
      warnings.push({
        file: 'menu-structure.config.ts',
        type: 'menu-reference',
        message: `需要手动删除菜单配置中的 '${cleanModuleName}'`,
      });
    }

    // 检查 i18n 文件
    const i18nFiles = [
      { name: 'zh-CN.ts', path: path.join(__dirname, '../src/lib/locales/zh-CN.ts') },
      { name: 'en-US.ts', path: path.join(__dirname, '../src/lib/locales/en-US.ts') },
    ];

    i18nFiles.forEach(file => {
      if (fileExists(file.path)) {
        warnings.push({
          file: file.name,
          type: 'i18n',
          message: `需要手动删除 menu.${camelName} 和相关翻译`,
        });
      }
    });

    if (filesToDelete.length === 0) {
      console.log('\n❌ 没有找到任何可删除的文件');
      rl.close();
      return;
    }

    console.log('\n📝 将要删除的文件:\n');
    filesToDelete.forEach((file, index) => {
      console.log(`${index + 1}. ${file.name}`);
      console.log(`   ${file.relativePath}`);
    });

    if (warnings.length > 0) {
      console.log('\n⚠️  需要手动处理的引用:\n');
      warnings.forEach((warning, index) => {
        console.log(`${index + 1}. ${warning.file}`);
        console.log(`   ${warning.message}`);
      });
    }

    console.log('\n⚠️  警告: 此操作不可撤销！\n');

    const confirm = await question('确认删除? (输入 "yes" 确认): ');
    if (confirm.toLowerCase() !== 'yes') {
      console.log('\n❌ 已取消删除');
      rl.close();
      return;
    }

    // 执行删除
    console.log('\n🗑️  开始删除...\n');

    let deletedCount = 0;
    filesToDelete.forEach(file => {
      if (deleteFile(file.path)) {
        console.log(`✅ 已删除: ${file.name}`);
        deletedCount++;
      } else {
        console.log(`❌ 删除失败: ${file.name}`);
      }
    });

    // 自动删除菜单配置引用
    console.log('\n🔧 自动清理配置文件...\n');

    const menuUpdate = removeMenuReferences(cleanModuleName, menuPath);

    if (menuUpdate.updated) {
      console.log('✅ 已更新菜单结构: menu-structure.config.ts');
      if (menuUpdate.removedTopLevel) {
        console.log('   - 已删除一级菜单项');
      }
      if (menuUpdate.removedChildren) {
        console.log('   - 已从 children 数组删除');
      }
    } else if (menuRefs.length > 0) {
      console.log('⚠️  菜单结构未找到引用或格式不匹配');
      warnings.push({
        file: 'menu-structure.config.ts',
        type: 'menu-reference',
        message: `请手动检查并删除 '${cleanModuleName}' 的菜单配置`,
      });
    }

    // 自动删除国际化翻译
    i18nFiles.forEach(file => {
      const result = removeI18nEntries(file.path, camelName);

      if (result.updated) {
        console.log(`✅ 已更新翻译: ${file.name}`);
        if (result.removedMenu) {
          console.log(`   - 已删除 menu.${camelName}`);
        }
        if (result.removedModule) {
          console.log(`   - 已删除 ${camelName} 对象`);
        }
      } else if (result.error) {
        console.log(`⚠️  ${file.name}: ${result.error}`);
      }
    });

    console.log(`\n✨ 成功删除 ${deletedCount} 个文件并清理了配置\n`);

    // 显示后续操作提示(仅针对自动化失败的项)
    const remainingWarnings = warnings.filter(w =>
      w.type === 'menu-reference' || w.type === 'app-reference'
    );

    if (componentRefs.length > 0 || remainingWarnings.length > 0) {
      console.log('📋 还需手动处理的项:\n');

      let stepNumber = 1;

      // App.svelte 引用
      if (componentRefs.length > 0) {
        console.log(`${stepNumber}. 在 src/App.svelte 中:`);
        console.log(`   - 删除: import ${pascalName} from './components/${pascalName}.svelte';`);
        console.log(`   - 删除路由渲染部分:`);
        console.log(`     {:else if currentModule?.customPage === '${pascalName}'}`);
        console.log(`       <${pascalName} />`);
        stepNumber++;
      }

      // 其他未自动处理的警告
      remainingWarnings.forEach(warning => {
        console.log(`\n${stepNumber}. ${warning.file}:`);
        console.log(`   ${warning.message}`);
        stepNumber++;
      });
    } else {
      console.log('✅ 所有配置已自动清理完成！');
    }

    console.log('\n💡 提示: 路由名称会自动同步，无需手动处理！\n');

  } catch (error) {
    console.error('\n❌ 删除失败:', error.message);
  } finally {
    rl.close();
  }
}

main();
