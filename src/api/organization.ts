/**
 * 组织架构管理 API
 */
import { get, post } from './request';

export interface OrganizationMember {
  id: number;
  username: string;
  accountNo: string;
  password?: string;
  phone: string;
  roleId: number;
  roleName?: string;
  status: 'Enable' | 'Disable';
  createTime?: string;
  updateTime?: string;
}

export interface OrganizationPageParams {
  page: number;
  size: number;
  username?: string;
  accountNo?: string;
  startTime?: string;
  endTime?: string;
  status?: 'Enable' | 'Disable';
}

export interface OrganizationPageResult {
  list: OrganizationMember[];
  total: number;
}

export const organizationApi = {
  // 查询组织架构成员（分页）
  page: (params: OrganizationPageParams) =>
    post<{
      count: number;
      size: number;
      totalPage: number;
      page: number;
      data: Array<{
        id: number;
        username: string;
        accountNo: string;
        roleName: string;
        roleKey: string;
        phone: string;
        createTime: string;
        status: string; // 后端返回 "enable" 或 "disable"
        roleId: number;
      }>;
    }>('/organization/page', params).then(res => {
      // 后端返回格式: { code: 200, data: { count, size, totalPage, page, data: [...] } }
      // 需要转换为: { code: 200, data: { list: [...], total: number } }
      const backendData = res.data;
      const members: OrganizationMember[] = (backendData?.data || []).map(item => ({
        id: item.id,
        username: item.username,
        accountNo: item.accountNo,
        phone: item.phone,
        roleId: item.roleId,
        roleName: item.roleName,
        // 将后端的 "enable"/"disable" 转换为前端的 "Enable"/"Disable"
        status: item.status === 'enable' ? 'Enable' : 'Disable',
        createTime: item.createTime,
      }));

      return {
        ...res,
        data: {
          list: members,
          total: backendData?.count || 0,
        },
      };
    }),

  // 新增管理员
  register: (payload: {
    username: string;
    accountNo: string;
    password: string;
    phone: string;
    roleId: number;
    status?: 'enable' | 'disable';
  }) => post('/organization/register', payload),

  // 修改用户
  update: (payload: {
    id: number;
    accountNo?: string;
    username?: string;
    password?: string;
    status?: 'enable' | 'disable';
    roleId?: number;
    phone?: string;
  }) => post('/organization/update', payload),

  // 删除用户
  delete: (id: number) => post('/organization/delete', null, { params: { id } }),

  // 后台登录
  login: (payload: { accountNo: string; password: string }) =>
    post<{
      token?: string;
      id?: number;
      username?: string;
      accountNo?: string;
      roleId?: number;
      roleName?: string;
    }>('/organization/login', payload),

  // 后台退出
  quit: () => post('/organization/quit'),
};
