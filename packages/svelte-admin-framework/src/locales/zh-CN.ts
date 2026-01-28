/**
 * 框架基础翻译 - 中文
 * @zhoolg/svelte-admin-framework
 */

export default {
  // 通用
  common: {
    confirm: '确定',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    add: '新增',
    search: '搜索',
    reset: '重置',
    export: '导出',
    import: '导入',
    refresh: '刷新',
    back: '返回',
    close: '关闭',
    loading: '加载中...',
    noData: '暂无数据',
    success: '操作成功',
    failed: '操作失败',
    warning: '警告',
    info: '提示',
    error: '错误',
    yes: '是',
    no: '否',
    all: '全部',
    selected: '已选择',
    items: '项',
    operation: '操作',
    status: '状态',
    createTime: '创建时间',
    updateTime: '更新时间',
    actions: '操作',
  },

  // 表格
  table: {
    total: '共 {total} 条',
    pageSize: '每页 {size} 条',
    noData: '暂无数据',
    loading: '加载中...',
    selectAll: '全选',
    selected: '已选择 {count} 项',
    batchDelete: '批量删除',
    confirmDelete: '确定要删除吗？',
    confirmBatchDelete: '确定要删除选中的 {count} 项吗？',
    deleteSuccess: '删除成功',
    deleteFailed: '删除失败',
    exportSuccess: '导出成功',
    exportFailed: '导出失败',
  },

  // 表单
  form: {
    required: '此项为必填项',
    invalid: '格式不正确',
    submitSuccess: '提交成功',
    submitFailed: '提交失败',
    pleaseInput: '请输入',
    pleaseSelect: '请选择',
    pleaseUpload: '请上传',
  },

  // 登录
  login: {
    title: '登录',
    username: '用户名',
    password: '密码',
    captcha: '验证码',
    rememberMe: '记住我',
    forgotPassword: '忘记密码',
    login: '登录',
    logging: '登录中...',
    loginSuccess: '登录成功',
    loginFailed: '登录失败',
    pleaseInputUsername: '请输入用户名',
    pleaseInputPassword: '请输入密码',
    pleaseInputCaptcha: '请输入验证码',
    captchaError: '验证码错误',
    accountOrPasswordError: '账号或密码错误',
  },

  // API 错误
  api: {
    networkError: '网络错误，请检查网络连接',
    timeout: '请求超时，请稍后重试',
    serverError: '服务器错误 ({status})',
    requestFailed: '请求失败',
    unknownError: '未知错误',
    noResponse: '服务器无响应',
    responseFormatError: '响应格式错误',
  },

  // 消息
  message: {
    sessionExpired: '登录已过期，请重新登录',
    noPermission: '您没有权限执行此操作',
    confirmLogout: '确定要退出登录吗？',
    logoutSuccess: '退出成功',
  },

  // 菜单
  menu: {
    home: '首页',
    dashboard: '仪表盘',
    profile: '个人中心',
    settings: '系统设置',
  },

  // 浏览器兼容性
  browserCompatibility: {
    unsupported: {
      title: '浏览器版本过低',
      description: '您当前使用的是 {browser} {version}，该版本不支持本系统的部分功能。',
      upgradeHint: '请升级到以下浏览器的最新版本：',
      downloadChrome: '下载 Chrome',
      downloadFirefox: '下载 Firefox',
    },
    oldVersion: {
      message: '您当前使用的 {browser} {version} 版本较旧，建议升级到最新版本以获得更好的体验。',
      dismiss: '知道了',
    },
  },

  // 404 页面
  notFound: {
    title: '页面未找到',
    subtitle: '抱歉，您访问的页面不存在或已被移除。',
    pathLabel: '您访问的路径：',
    reasonTitle: '可能的原因：',
    reason1: '页面地址输入错误',
    reason2: '页面已被删除或移动',
    reason3: '您没有访问该页面的权限',
    reason4: '链接已过期',
    back: '返回上一页',
    home: '返回首页',
    footer: '如果问题持续存在，请联系',
    support: '技术支持',
  },

  // 图片上传
  imageUpload: {
    button: '上传图片',
    loading: '上传中...',
    success: '上传成功',
    failed: '上传失败',
    fileTooLarge: '文件 {name} 超过 {maxSize}MB 限制',
    previewAlt: '预览图片',
    previewFailed: '加载失败',
    video: '视频',
    preview: '预览',
  },
};
