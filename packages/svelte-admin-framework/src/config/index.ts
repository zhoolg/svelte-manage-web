/**
 * 配置模块统一导出
 * @zhoolg/svelte-admin-framework
 */

// 模块类型定义
export type {
  TableColumn,
  SearchField,
  SearchFieldType,
  FormField,
  FormFieldType,
  FormRule,
  ActionButton,
  ModuleConfig,
} from './module';

export { defineModule } from './module';

// 应用模块配置
export type { AppModule, CrudConfig, MenuStructure, ModuleRegistry } from './app.modules';

export { createModuleRegistry, toMenuConfig } from './app.modules';
