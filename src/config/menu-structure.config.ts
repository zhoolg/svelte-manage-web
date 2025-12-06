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

  // 用户中心（二级菜单）
  {
    id: 'user-center',
    children: ['users', 'agents'],
  },

  // 内容管理（二级菜单）
  {
    id: 'content',
    children: ['faq', 'articles'],
  },

  // 系统管理（二级菜单）
  {
    id: 'system',
    children: ['logs', 'dict', 'settings'],
  },

  // 隐藏菜单
  {
    id: 'profile',
  },
];
