/**
 * 菜单结构配置
 *
 * 定义菜单的层级关系和显示顺序
 * 只需在这里配置菜单结构，页面配置在各自的 modules/*.config.ts 文件中
 */

export interface MenuStructure {
  /** 模块 ID（对应 modules/*.config.ts 中的 id） */
  id: string;
  /** 子菜单 ID 列表 */
  children?: string[];
}

/**
 * 菜单结构配置
 *
 * 添加新菜单分组：
 * 1. 在 modules/ 下创建分组配置文件（如 xxx.config.ts）
 * 2. 在下面的数组中添加配置
 */
export const MENU_STRUCTURE: MenuStructure[] = [
  // 首页（一级菜单）
  {
    id: 'home',
  },

  // 房源管理（二级菜单）
  {
    id: 'property',
    children: ['properties', 'property-types'],
  },

  // 租客管理（二级菜单）
  {
    id: 'tenant',
    children: ['tenants', 'applications', 'contracts'],
  },

  // 财务管理（二级菜单）
  {
    id: 'finance',
    children: ['rent-payments', 'deposits', 'refunds'],
  },

  // 分销管理（二级菜单）
  {
    id: 'distribution',
    children: ['qrcodes', 'commissions'],
  },

  // 客服中心（二级菜单）
  {
    id: 'service',
    children: ['chat-messages', 'faq'],
  },

  // 系统管理（二级菜单）
  {
    id: 'system',
    children: ['admins', 'logs', 'dict', 'settings'],
  },

  // 隐藏菜单
  {
    id: 'profile',
  },
];
