/**
 * 菜单配置
 * ============================================================
 *
 *  新版本：统一配置在 app.modules.ts 中
 * 此文件仅保留兼容性，实际配置请修改 app.modules.ts
 *
 * 配置说明：
 * - path: 路由路径（父菜单可以不设置 path，仅作为分组）
 * - label: 菜单名称
 * - icon: 图标 (PrimeIcons)
 * - children: 子菜单数组
 * - hidden: 是否隐藏（路由仍然存在）
 * - external: 是否为外链
 * - module: 模块名称（用于 CrudPage）
 */

import { APP_MODULES, toMenuConfig } from './app.modules';

export interface MenuItem {
  /** 路由路径（父菜单可选） */
  path?: string;
  /** 菜单名称 */
  label: string;
  /** 图标 (PrimeIcons) */
  icon: string;
  /** 模块名称（对应 modules 目录下的文件夹名） */
  module?: string;
  /** 子菜单 */
  children?: MenuItem[];
  /** 是否隐藏菜单（路由仍然存在） */
  hidden?: boolean;
  /** 是否为外链 */
  external?: boolean;
  /** 是否默认展开 */
  defaultOpen?: boolean;
  /** 权限列表（用户需要拥有其中任一权限才能访问） */
  permissions?: string[];
  /** 角色列表（用户需要拥有其中任一角色才能访问） */
  roles?: string[];
}

/**
 * 菜单配置（自动从 app.modules.ts 生成）
 */
export const menuConfig: MenuItem[] = toMenuConfig(APP_MODULES) as MenuItem[];

/**
 * 获取所有菜单项（扁平化）
 */
export function getFlatMenus(menus: MenuItem[] = menuConfig): MenuItem[] {
  return menus.reduce<MenuItem[]>((acc, menu) => {
    if (menu.path) {
      acc.push(menu);
    }
    if (menu.children) {
      acc.push(...getFlatMenus(menu.children));
    }
    return acc;
  }, []);
}

/**
 * 根据路径获取菜单项
 */
export function getMenuByPath(path: string): MenuItem | undefined {
  return getFlatMenus().find((menu) => menu.path === path);
}

/**
 * 根据路径获取父菜单
 */
export function getParentMenu(path: string, menus: MenuItem[] = menuConfig): MenuItem | undefined {
  for (const menu of menus) {
    if (menu.children) {
      const child = menu.children.find((c) => c.path === path);
      if (child) return menu;
      const parent = getParentMenu(path, menu.children);
      if (parent) return parent;
    }
  }
  return undefined;
}

/**
 * 检查菜单是否有子菜单处于激活状态
 */
export function hasActiveChild(menu: MenuItem, currentPath: string): boolean {
  if (!menu.children) return false;
  return menu.children.some(
    (child) => child.path === currentPath || hasActiveChild(child, currentPath)
  );
}
