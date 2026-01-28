/**
 * 模块配置类型定义
 * 用于配置 CRUD 页面的表格列、搜索字段、表单字段等
 * @zhoolg/svelte-admin-framework
 */
// ==================== 辅助函数 ====================
/**
 * 创建模块配置（带类型推断）
 */
export function defineModule(config) {
    return {
        ...config,
        table: {
            rowKey: 'id',
            showIndex: false,
            showSelection: false,
            actionWidth: 150,
            ...config.table,
        },
        toolbar: {
            showAdd: true,
            addText: '新增',
            showRefresh: true,
            ...config.toolbar,
        },
        pagination: {
            pageSize: 10,
            pageSizes: [10, 20, 50, 100],
            ...config.pagination,
        },
    };
}
