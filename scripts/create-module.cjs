#!/usr/bin/env node

/**
 * 完全自动化的模块脚手架工具
 * 用法: npm run create-module
 *
 * 自动完成：
 * 1. 创建模块配置文件
 * 2. 创建自定义组件（如果需要）
 * 3. 自动添加到菜单结构
 * 4. 自动添加国际化翻译
 * 5. 自动注册自定义组件（如果需要）
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

/**
 * 读取多行输入（用于粘贴JSON数据）
 * @param {string} prompt - 提示信息
 * @param {string} endToken - 结束标记，默认为 'END'
 * @returns {Promise<string>} 返回完整的多行输入
 */
async function readMultilineInput(prompt, endToken = 'END') {
  console.log(prompt);
  console.log(`提示: 粘贴完成后，在新行输入 '${endToken}' 并按回车结束\n`);

  const lines = [];
  const normalizedEndToken = endToken.toUpperCase();

  while (true) {
    const line = await question('');

    // 检查是否为结束标记 (不区分大小写)
    if (line.trim().toUpperCase() === normalizedEndToken) {
      break;
    }

    lines.push(line);
  }

  return lines.join('\n');
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

// 从后端数据生成字段配置
function generateFieldsFromJson(jsonData, moduleName = 'module') {
  try {
    const data = typeof jsonData === 'string' ? JSON.parse(jsonData) : jsonData;

    // 如果是数组，取第一个元素
    const sample = Array.isArray(data) ? data[0] : data;

    if (!sample || typeof sample !== 'object') {
      throw new Error('无效的数据格式');
    }

    const fields = [];
    const commonFields = ['id', 'createTime', 'updateTime', 'status'];

    // 遍历对象的所有键
    Object.keys(sample).forEach(key => {
      const value = sample[key];
      const field = {
        field: key,
        label: `common.${key}`,
        width: 120,
      };

      // 根据字段名和值类型判断字段类型
      if (key === 'id') {
        field.width = 80;
        field.sortable = true;
      } else if (key === 'status') {
        field.width = 100;
        field.format = 'switch';
        field.switchConfig = {
          api: `/${moduleName}/status`,
          activeValue: 1,
          inactiveValue: 0,
        };
      } else if (key.includes('Time') || key.includes('Date') || key.includes('time') || key.includes('date')) {
        field.width = 180;
        field.format = 'datetime';
        field.sortable = true;
      } else if (key.includes('image') || key.includes('img') || key.includes('avatar') || key.includes('photo')) {
        field.width = 80;
        field.format = 'image';
      } else if (key.includes('name') || key.includes('title')) {
        field.minWidth = 150;
        field.sortable = true;
        delete field.width;
      } else if (typeof value === 'number') {
        field.width = 100;
      } else if (typeof value === 'string' && value.length > 50) {
        field.minWidth = 200;
        delete field.width;
      }

      fields.push(field);
    });

    return fields;
  } catch (error) {
    console.error('解析JSON失败:', error.message);
    return null;
  }
}

// 生成搜索字段配置
function generateSearchFields(columns) {
  const searchFields = [];

  columns.forEach(col => {
    // 跳过 id 和时间字段（大小写不敏感）
    const fieldLower = col.field.toLowerCase();
    if (col.field === 'id' || fieldLower.includes('time') || fieldLower.includes('date')) {
      return;
    }

    const field = {
      field: col.field,
      label: col.label,
      type: 'text',
      placeholder: 'common.pleaseEnter',
    };

    if (col.field === 'status') {
      field.type = 'select';
      field.options = [
        { label: 'common.all', value: '' },
        { label: 'common.enabled', value: 1 },
        { label: 'common.disabled', value: 0 },
      ];
      delete field.placeholder;
    }

    searchFields.push(field);
  });

  return searchFields.slice(0, 3); // 只保留前3个搜索字段
}

// 生成表单字段配置
function generateFormFields(columns) {
  const formFields = [];

  columns.forEach(col => {
    // 跳过 id 和时间字段（大小写不敏感）
    const fieldLower = col.field.toLowerCase();
    if (col.field === 'id' || fieldLower.includes('time') || fieldLower.includes('date')) {
      return;
    }

    const field = {
      field: col.field,
      label: col.label,
      type: 'text',
      required: true,
      placeholder: 'common.pleaseEnter',
    };

    if (col.field === 'status') {
      field.type = 'select';
      field.defaultValue = 1;
      field.options = [
        { label: 'common.enabled', value: 1 },
        { label: 'common.disabled', value: 0 },
      ];
      delete field.placeholder;
    } else if (col.format === 'image') {
      field.type = 'image';
      field.accept = 'image/*';
      field.required = false;
      delete field.placeholder;
    }

    formFields.push(field);
  });

  return formFields;
}

/**
 * 将数组对象格式化为缩进的字符串
 * @param {string} key - 配置项名称
 * @param {Array} items - 配置数组
 * @param {number} baseIndent - 基础缩进级别(空格数)
 * @returns {string} 格式化后的字符串
 */
function formatArrayField(key, items, baseIndent = 4) {
  const indent = ' '.repeat(baseIndent);

  // 处理空数组
  if (!items || items.length === 0) {
    return `${indent}${key}: []`;
  }

  const itemIndent = ' '.repeat(baseIndent + 2);

  const formattedItems = items.map(item => {
    const json = JSON.stringify(item, null, 2);
    // 为每行添加缩进
    const indentedJson = json
      .split('\n')
      .map((line, idx) => (idx === 0 ? line : itemIndent + line))
      .join('\n');
    return itemIndent + indentedJson;
  }).join(',\n');

  return `${indent}${key}: [\n${formattedItems},\n${indent}]`;
}

// 生成模块配置文件模板（支持自动生成字段）
function generateModuleConfig(moduleName, moduleInfo, autoFields = null) {
  const camelName = toCamelCase(moduleName);
  const pascalName = toPascalCase(moduleName);

  // 自定义页面配置
  if (moduleInfo.isCustom) {
    return `/**
 * ${pascalName} 模块配置
 */
import type { AppModule } from '../app.modules';

const ${camelName}Module: AppModule = {
  id: '${moduleName}',
  label: 'menu.${camelName}',
  icon: '${moduleInfo.icon}',
  path: '/${moduleName}',
  customPage: '${pascalName}',
};

export default ${camelName}Module;
`;
  }

  // 检查是否有自动生成的字段配置
  const hasAutoFields = !!(
    autoFields
    && Array.isArray(autoFields.columns)
    && autoFields.columns.length > 0
    && Array.isArray(autoFields.searchFields)
    && Array.isArray(autoFields.formFields)
  );

  // CRUD页面配置 - 使用自动生成或默认字段
  let columnsBlock, searchFieldsBlock, formFieldsBlock;

  if (hasAutoFields) {
    // 使用自动生成的字段配置
    columnsBlock = formatArrayField('columns', autoFields.columns);
    searchFieldsBlock = formatArrayField('searchFields', autoFields.searchFields);
    formFieldsBlock = formatArrayField('formFields', autoFields.formFields);
  } else {
    // 使用默认字段配置
    const defaultColumns = [
      {
        field: 'id',
        label: 'common.id',
        width: 80,
        sortable: true,
      },
      {
        field: 'name',
        label: 'common.name',
        minWidth: 150,
        sortable: true,
      },
      {
        field: 'status',
        label: 'common.status',
        width: 100,
        format: 'switch',
        switchConfig: {
          api: `/${moduleName}/status`,
          activeValue: 1,
          inactiveValue: 0,
        },
      },
      {
        field: 'createTime',
        label: 'common.createTime',
        width: 180,
        format: 'datetime',
        sortable: true,
      },
    ];

    const defaultSearchFields = [
      {
        field: 'name',
        label: 'common.name',
        type: 'text',
        placeholder: 'common.pleaseEnter',
      },
      {
        field: 'status',
        label: 'common.status',
        type: 'select',
        options: [
          { label: 'common.all', value: '' },
          { label: 'common.enabled', value: 1 },
          { label: 'common.disabled', value: 0 },
        ],
      },
    ];

    const defaultFormFields = [
      {
        field: 'name',
        label: 'common.name',
        type: 'text',
        required: true,
        placeholder: 'common.pleaseEnter',
      },
      {
        field: 'status',
        label: 'common.status',
        type: 'select',
        required: true,
        defaultValue: 1,
        options: [
          { label: 'common.enabled', value: 1 },
          { label: 'common.disabled', value: 0 },
        ],
      },
    ];

    columnsBlock = formatArrayField('columns', defaultColumns);
    searchFieldsBlock = formatArrayField('searchFields', defaultSearchFields);
    formFieldsBlock = formatArrayField('formFields', defaultFormFields);
  }

  return `/**
 * ${pascalName} 模块配置
 */
import type { AppModule } from '../app.modules';

const ${camelName}Module: AppModule = {
  id: '${moduleName}',
  label: 'menu.${camelName}',
  icon: '${moduleInfo.icon}',
  path: '/${moduleName}',
  crud: {
    title: '${camelName}.title',
    apiBase: '/${moduleName}',
${columnsBlock},
${searchFieldsBlock},
${formFieldsBlock},
    showAdd: true,
    showEdit: true,
    showDelete: true,
    showExport: true,
  },
};

export default ${camelName}Module;
`;
}

// 生成自定义组件模板
function generateCustomComponent(moduleName) {
  const pascalName = toPascalCase(moduleName);

  return `<script lang="ts">
  /**
   * ${pascalName} 页面
   */
  import { onMount } from 'svelte';
  import { t } from '$lib/locales';
  import { toast } from '../utils/toast';

  let loading = false;
  let data: any[] = [];

  onMount(async () => {
    await loadData();
  });

  async function loadData() {
    loading = true;
    try {
      // TODO: 调用 API 获取数据
      // const res = await api.getList();
      // data = res.data;

      console.log('${pascalName} loaded');
    } catch (error) {
      toast.error($t('message.loadFailed'));
      console.error(error);
    } finally {
      loading = false;
    }
  }
</script>

<div class="p-6">
  <div class="mb-4">
    <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">
      {$t('menu.${toCamelCase(moduleName)}')}
    </h1>
  </div>

  {#if loading}
    <div class="flex justify-center items-center h-64">
      <i class="pi pi-spin pi-spinner text-4xl text-blue-500"></i>
    </div>
  {:else}
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <p class="text-gray-600 dark:text-gray-400">
        TODO: 实现 ${pascalName} 页面内容
      </p>
    </div>
  {/if}
</div>
`;
}

// 自动添加到菜单结构
function addToMenuStructure(moduleName, parentMenu) {
  const menuPath = path.join(__dirname, '../src/config/menu-structure.config.ts');
  let content = fs.readFileSync(menuPath, 'utf-8');

  if (parentMenu && parentMenu.trim()) {
    // 添加为子菜单
    const parent = parentMenu.trim();

    // 更精确的正则：只匹配 MENU_STRUCTURE 数组中的内容
    // 使用更严格的模式，确保匹配的是数组元素而不是接口定义
    const regex = new RegExp(
      `(MENU_STRUCTURE[\\s\\S]*?\\{[\\s\\n]*id:[\\s\\n]*['"]${parent}['"][\\s\\S]*?children:[\\s\\n]*\\[)([^\\]]*)`,
      'g'
    );

    content = content.replace(regex, (match, p1, p2) => {
      // 检查是否已存在
      if (p2.includes(`'${moduleName}'`) || p2.includes(`"${moduleName}"`)) {
        return match;
      }
      // 添加到children数组末尾
      const trimmed = p2.trim();
      if (trimmed.endsWith(',')) {
        return `${p1}${p2} '${moduleName}',`;
      } else if (trimmed) {
        return `${p1}${p2}, '${moduleName}',`;
      } else {
        return `${p1}'${moduleName}',`;
      }
    });
  } else {
    // 添加为一级菜单
    const menuEntry = `  { id: '${moduleName}' },`;

    // 在最后一个菜单项之前插入（更精确的定位）
    const lines = content.split('\n');

    // 找到 MENU_STRUCTURE 数组结束的位置
    let inMenuStructure = false;
    let insertIndex = -1;

    for (let i = 0; i < lines.length; i++) {
      if (lines[i].includes('MENU_STRUCTURE') && lines[i].includes('[')) {
        inMenuStructure = true;
      }
      if (inMenuStructure && lines[i].includes('];')) {
        insertIndex = i;
        break;
      }
    }

    if (insertIndex > 0) {
      // 向上查找最后一个非空、非注释的菜单项行
      let lastMenuItemIndex = -1;
      for (let i = insertIndex - 1; i >= 0; i--) {
        const line = lines[i].trim();
        // 跳过空行和纯注释行
        if (!line || line.startsWith('//') || line.startsWith('/*') || line.startsWith('*')) {
          continue;
        }
        // 找到了包含内容的行
        if (line.includes('{') || line.includes('}')) {
          lastMenuItemIndex = i;
          break;
        }
      }

      // 检查最后一个菜单项是否需要添加逗号
      if (lastMenuItemIndex >= 0) {
        const line = lines[lastMenuItemIndex];
        const trimmed = line.trimEnd();
        // 如果这行以 } 结尾（可能带注释），且结尾没有逗号，则添加逗号
        // 匹配: }  或  } // comment  或  } /* comment */
        if (trimmed.match(/\}\s*(\/\/.*|\/\*.*\*\/)?$/) && !trimmed.match(/,\s*(\/\/.*|\/\*.*\*\/)?$/)) {
          // 在 } 后、注释前插入逗号
          lines[lastMenuItemIndex] = line.replace(/(\})\s*(\/\/.*|\/\*.*\*\/)?$/, '$1,$2');
        }
      }

      lines.splice(insertIndex, 0, menuEntry);
      content = lines.join('\n');
    }
  }

  fs.writeFileSync(menuPath, content, 'utf-8');
  return true;
}

// 自动添加翻译
function addTranslations(moduleName, chineseName, isCustom) {
  const camelName = toCamelCase(moduleName);
  const pascalName = toPascalCase(moduleName);

  // 中文翻译
  const zhPath = path.join(__dirname, '../src/lib/locales/zh-CN.ts');
  let zhContent = fs.readFileSync(zhPath, 'utf-8');

  // 在menu对象中添加
  zhContent = zhContent.replace(
    /(menu:\s*\{[^}]*)/,
    `$1\n    ${camelName}: '${chineseName}',`
  );

  // 如果是CRUD页面,添加模块翻译对象
  if (!isCustom) {
    zhContent = zhContent.replace(
      /(export default \{)/,
      `$1\n  ${camelName}: {\n    title: '${chineseName}',\n    name: '名称',\n  },`
    );
  }

  fs.writeFileSync(zhPath, zhContent, 'utf-8');

  // 英文翻译
  const enPath = path.join(__dirname, '../src/lib/locales/en-US.ts');
  let enContent = fs.readFileSync(enPath, 'utf-8');

  enContent = enContent.replace(
    /(menu:\s*\{[^}]*)/,
    `$1\n    ${camelName}: '${pascalName}',`
  );

  if (!isCustom) {
    enContent = enContent.replace(
      /(export default \{)/,
      `$1\n  ${camelName}: {\n    title: '${pascalName} Management',\n    name: 'Name',\n  },`
    );
  }

  fs.writeFileSync(enPath, enContent, 'utf-8');
  return true;
}

// 自动注册自定义组件到App.svelte
function registerCustomComponent(componentName) {
  const appPath = path.join(__dirname, '../src/App.svelte');
  let content = fs.readFileSync(appPath, 'utf-8');

  // 检查是否已存在
  if (content.includes(`import ${componentName} from`)) {
    return false; // 已存在
  }

  // 1. 添加import语句（在其他组件import之后）
  const importStatement = `  import ${componentName} from './components/${componentName}.svelte';`;
  content = content.replace(
    /(import\s+\w+\s+from\s+['"]\.\/components\/\w+\.svelte['"];?\s*\n)/g,
    `$1${importStatement}\n`
  );

  // 2. 添加路由渲染（在NotFound之前）
  const routeBlock = `  {:else if currentModule?.customPage === '${componentName}'}\n    <${componentName} />`;
  content = content.replace(
    /(\{:else\}[\s\S]*?<NotFound \/>)/,
    `${routeBlock}\n$1`
  );

  fs.writeFileSync(appPath, content, 'utf-8');
  return true;
}

// 常用图标列表
const commonIcons = [
  { name: '用户', icon: 'pi pi-users' },
  { name: '购物车', icon: 'pi pi-shopping-cart' },
  { name: '商品', icon: 'pi pi-box' },
  { name: '设置', icon: 'pi pi-cog' },
  { name: '文件', icon: 'pi pi-file' },
  { name: '图表', icon: 'pi pi-chart-bar' },
  { name: '列表', icon: 'pi pi-list' },
  { name: '星标', icon: 'pi pi-star' },
  { name: '标签', icon: 'pi pi-tags' },
  { name: '日历', icon: 'pi pi-calendar' },
];

// 主函数
async function main() {
  console.log('\n🚀 模块脚手架工具（完全自动化）\n');

  try {
    // 1. 输入模块名称
    const moduleName = await question('📝 模块名称 (kebab-case，如 user-manage): ');
    if (!moduleName || !moduleName.trim()) {
      console.log('❌ 模块名称不能为空');
      rl.close();
      return;
    }

    // 验证命名格式
    if (!/^[a-z][a-z0-9-]*$/.test(moduleName.trim())) {
      console.log('❌ 模块名称格式错误，只能使用小写字母、数字和连字符，且必须以字母开头');
      rl.close();
      return;
    }

    const cleanModuleName = moduleName.trim();
    const camelName = toCamelCase(cleanModuleName);
    const pascalName = toPascalCase(cleanModuleName);

    console.log(`\n✓ 模块 ID: ${cleanModuleName}`);
    console.log(`✓ camelCase: ${camelName}`);
    console.log(`✓ PascalCase: ${pascalName}`);

    // 2. 输入中文名称
    const chineseName = await question('\n📝 中文名称 (如 用户管理): ');
    const finalChineseName = chineseName.trim() || `${pascalName}管理`;
    console.log(`✓ 中文名: ${finalChineseName}`);

    // 3. 选择模块类型
    console.log('\n📋 模块类型:');
    console.log('1. CRUD 页面（通用增删改查）');
    console.log('2. 自定义页面（完全自定义）');
    const typeChoice = await question('选择类型 (1 或 2): ');
    const isCustom = typeChoice.trim() === '2';

    console.log(`✓ 类型: ${isCustom ? '自定义页面' : 'CRUD 页面'}`);

    // 3.1 如果是CRUD页面，询问是否粘贴JSON自动生成字段
    let autoFields = null;
    if (!isCustom) {
      console.log('\n📊 字段配置:');
      console.log('1. 使用默认字段配置 (id, name, status, createTime)');
      console.log('2. 粘贴后端JSON数据自动生成字段');

      const fieldChoice = await question('选择字段配置方式 (1 或 2，默认1): ');

      if (fieldChoice.trim() === '2') {
        try {
          console.log('\n💡 示例JSON格式:');
          console.log('   [{"id":1,"name":"测试","price":99.9,"status":1,"createTime":"2024-01-01"}]');
          console.log('   或');
          console.log('   {"id":1,"name":"测试","price":99.9,"status":1,"createTime":"2024-01-01"}\n');

          const jsonText = await readMultilineInput('📋 请粘贴后端返回的JSON数据:');

          if (!jsonText || !jsonText.trim()) {
            console.log('⚠️  未输入JSON数据，将使用默认字段配置');
          } else {
            // 尝试解析JSON并生成字段配置
            const columns = generateFieldsFromJson(jsonText.trim(), cleanModuleName);

            if (!columns || columns.length === 0) {
              console.log('⚠️  JSON解析失败或无有效字段，将使用默认字段配置');
            } else {
              // 成功生成字段配置
              autoFields = {
                columns: columns,
                searchFields: generateSearchFields(columns),
                formFields: generateFormFields(columns),
              };

              console.log(`✅ 已成功解析JSON，生成了 ${columns.length} 个字段配置`);
              console.log('   字段列表:', columns.map(f => f.field).join(', '));
            }
          }
        } catch (error) {
          console.log(`⚠️  处理JSON时出错: ${error.message}`);
          console.log('   将使用默认字段配置');
        }
      } else {
        console.log('✓ 使用默认字段配置');
      }
    }

    // 4. 选择图标
    console.log('\n🎨 选择图标:');
    commonIcons.forEach((item, index) => {
      console.log(`${index + 1}. ${item.name} (${item.icon})`);
    });
    console.log(`${commonIcons.length + 1}. 自定义图标`);

    const iconChoice = await question(`选择图标 (1-${commonIcons.length + 1}): `);
    let icon = 'pi pi-star';

    const iconIndex = parseInt(iconChoice.trim()) - 1;
    if (iconIndex >= 0 && iconIndex < commonIcons.length) {
      icon = commonIcons[iconIndex].icon;
    } else if (iconIndex === commonIcons.length) {
      const customIcon = await question('输入自定义图标 (如 pi pi-home): ');
      icon = customIcon.trim() || 'pi pi-star';
    }

    console.log(`✓ 图标: ${icon}`);

    // 5. 选择菜单级别
    console.log('\n📂 菜单级别:');
    console.log('1. 一级菜单');
    console.log('2. 二级菜单（子菜单）');
    const levelChoice = await question('选择级别 (1 或 2): ');
    const isSubMenu = levelChoice.trim() === '2';

    let parentMenu = '';
    if (isSubMenu) {
      console.log('\n常用父菜单: system, product, content');
      parentMenu = await question('输入父菜单 ID: ');
      console.log(`✓ 父菜单: ${parentMenu.trim()}`);
    } else {
      console.log('✓ 一级菜单');
    }

    // 6. 确认信息
    console.log('\n📋 配置摘要:');
    console.log('─────────────────────────────────');
    console.log(`模块名称: ${cleanModuleName}`);
    console.log(`中文名称: ${finalChineseName}`);
    console.log(`模块类型: ${isCustom ? '自定义页面' : 'CRUD 页面'}`);
    console.log(`图标: ${icon}`);
    console.log(`菜单级别: ${isSubMenu ? `二级菜单 (父: ${parentMenu.trim()})` : '一级菜单'}`);
    console.log('─────────────────────────────────\n');

    const confirm = await question('确认创建? (y/n): ');
    if (confirm.toLowerCase() !== 'y') {
      console.log('❌ 已取消');
      rl.close();
      return;
    }

    // 7. 开始创建
    console.log('\n🔨 开始创建...\n');

    const moduleInfo = { isCustom, icon };

    // 7.1 创建配置文件
    const configPath = path.join(__dirname, `../src/config/modules/${cleanModuleName}.config.ts`);
    if (fs.existsSync(configPath)) {
      const overwrite = await question(`⚠️  配置文件已存在，是否覆盖? (y/n): `);
      if (overwrite.toLowerCase() !== 'y') {
        console.log('❌ 已取消');
        rl.close();
        return;
      }
    }

    fs.writeFileSync(configPath, generateModuleConfig(cleanModuleName, moduleInfo, autoFields));
    console.log(`✅ 已创建: src/config/modules/${cleanModuleName}.config.ts`);

    // 7.2 如果是自定义页面，创建组件文件
    if (isCustom) {
      const componentPath = path.join(__dirname, `../src/components/${pascalName}.svelte`);
      if (!fs.existsSync(componentPath)) {
        fs.writeFileSync(componentPath, generateCustomComponent(cleanModuleName));
        console.log(`✅ 已创建: src/components/${pascalName}.svelte`);
      } else {
        console.log(`⚠️  组件文件已存在: src/components/${pascalName}.svelte`);
      }
    }

    // 7.3 自动添加到菜单结构
    try {
      addToMenuStructure(cleanModuleName, parentMenu);
      console.log(`✅ 已添加到菜单结构: menu-structure.config.ts`);
    } catch (error) {
      console.log(`⚠️  添加菜单结构失败: ${error.message}`);
      console.log(`   请手动在 menu-structure.config.ts 中添加`);
    }

    // 7.4 自动添加翻译
    try {
      addTranslations(cleanModuleName, finalChineseName, isCustom);
      console.log(`✅ 已添加翻译: zh-CN.ts, en-US.ts`);
    } catch (error) {
      console.log(`⚠️  添加翻译失败: ${error.message}`);
      console.log(`   请手动在国际化文件中添加翻译`);
    }

    // 7.5 如果是自定义页面，自动注册组件
    if (isCustom) {
      try {
        const registered = registerCustomComponent(pascalName);
        if (registered) {
          console.log(`✅ 已注册组件: App.svelte`);
        } else {
          console.log(`⚠️  组件已存在于 App.svelte 中`);
        }
      } catch (error) {
        console.log(`⚠️  注册组件失败: ${error.message}`);
        console.log(`   请手动在 App.svelte 中添加:`);
        console.log(`   import ${pascalName} from './components/${pascalName}.svelte';`);
        console.log(`   {:else if currentModule?.customPage === '${pascalName}'}`);
        console.log(`     <${pascalName} />`);
      }
    }

    console.log('\n✨ 模块创建完成！');
    console.log('💡 提示: 所有配置已自动完成，路由和面包屑会自动生效！');
    console.log('🚀 运行 npm run dev 查看效果\n');

  } catch (error) {
    console.error('\n❌ 创建失败:', error.message);
    console.error(error.stack);
  } finally {
    rl.close();
  }
}

main();
