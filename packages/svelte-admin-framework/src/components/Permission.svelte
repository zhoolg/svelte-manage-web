<script lang="ts">
  /**
   * 权限控制组件
   * @zhoolg/svelte-admin-framework
   */
  import { permissionStore, isAdmin } from '../stores/permissionStore';

  /** 单个权限码 */
  export let permission: string | undefined = undefined;

  /** 多个权限码 */
  export let permissions: string[] | undefined = undefined;

  /** 单个角色 */
  export let role: string | undefined = undefined;

  /** 多个角色 */
  export let roles: string[] | undefined = undefined;

  /** 检查模式：OR（任一）或 AND（全部） */
  export let mode: 'OR' | 'AND' = 'OR';

  /** 反向控制：true 时无权限才显示 */
  export let fallback = false;

  // 响应式计算是否有权限
  $: hasAuth = checkPermission();

  function checkPermission(): boolean {
    // 如果是超级管理员，直接返回 true
    if ($isAdmin && !fallback) {
      return true;
    }

    // 收集所有需要检查的权限
    const permsToCheck: string[] = [];
    if (permission) permsToCheck.push(permission);
    if (permissions) permsToCheck.push(...permissions);

    // 收集所有需要检查的角色
    const rolesToCheck: string[] = [];
    if (role) rolesToCheck.push(role);
    if (roles) rolesToCheck.push(...roles);

    // 如果没有任何限制条件，默认显示
    if (permsToCheck.length === 0 && rolesToCheck.length === 0) {
      return !fallback;
    }

    let result = false;

    if (mode === 'AND') {
      // AND 模式：需要满足所有条件
      const hasAllPerms =
        permsToCheck.length === 0 || permissionStore.hasAllPermissions(permsToCheck);
      const hasAllRoles =
        rolesToCheck.length === 0 || rolesToCheck.every((r) => permissionStore.hasRole(r));
      result = hasAllPerms && hasAllRoles;
    } else {
      // OR 模式：满足任一条件即可
      const hasAnyPerm =
        permsToCheck.length > 0 && permissionStore.hasAnyPermission(permsToCheck);
      const hasAnyRole =
        rolesToCheck.length > 0 && rolesToCheck.some((r) => permissionStore.hasRole(r));

      if (permsToCheck.length > 0 && rolesToCheck.length > 0) {
        result = hasAnyPerm || hasAnyRole;
      } else if (permsToCheck.length > 0) {
        result = hasAnyPerm;
      } else {
        result = hasAnyRole;
      }
    }

    // 如果是 fallback 模式，反转结果
    return fallback ? !result : result;
  }
</script>

{#if hasAuth}
  <slot />
{/if}
