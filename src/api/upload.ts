/**
 * 文件上传 API
 */
import { BASE_URL } from './request';
import { authStore } from '../stores/authStore';
import { get as getStore } from 'svelte/store';

/**
 * 上传图片
 * @param file 图片文件
 * @returns 图片 URL
 */
export async function uploadImage(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  // 获取 token
  const { token } = getStore(authStore);

  const response = await fetch(`${BASE_URL}/file/uploadImg`, {
    method: 'POST',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
    },
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`上传失败: ${response.status}`);
  }

  // 获取响应内容类型
  const contentType = response.headers.get('content-type');

  // 如果返回的是纯文本（相对路径）
  if (contentType?.includes('text/plain')) {
    const relativePath = await response.text();
    // 将反斜杠转换为正斜杠，返回 API 原始路径
    const normalizedPath = relativePath.replace(/\\/g, '/');
    return normalizedPath;
  }

  // 如果返回的是 JSON 格式
  const result = await response.json();
  if (result.code === 0 || result.code === 200) {
    // 返回 API 原始数据，不添加 BASE_URL
    if (result.data && typeof result.data === 'string') {
      return result.data;
    }
    // 如果返回的是对象格式
    if (result.data && result.data.url) {
      return result.data.url;
    }
    throw new Error('上传成功但未返回图片地址');
  } else {
    throw new Error(result.msg || '上传失败');
  }
}

/**
 * 上传多张图片
 * @param files 图片文件数组
 * @returns 图片 URL 数组
 */
export async function uploadImages(files: File[]): Promise<string[]> {
  const uploadPromises = files.map(file => uploadImage(file));
  return Promise.all(uploadPromises);
}
