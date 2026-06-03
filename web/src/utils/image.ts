/**
 * 图片 URL 工具函数
 */
import { APP_CONFIG } from '../config';

/**
 * 获取完整的图片 URL
 * @param path 图片路径（可以是相对路径或完整 URL）
 * @returns 完整的图片 URL
 */
export function getImageUrl(path: string | undefined | null): string {
  if (!path) return '';

  // 调试日志
  console.log('[getImageUrl] 输入路径:', path);
  console.log('[getImageUrl] imageBaseUrl配置:', APP_CONFIG.imageBaseUrl);

  // 如果已经是完整 URL（http:// 或 https://），直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    console.log('[getImageUrl] 已是完整URL，直接返回');
    return path;
  }

  // 如果配置了图片基础 URL，拼接路径
  if (APP_CONFIG.imageBaseUrl) {
    // 确保路径以 / 开头
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const fullUrl = `${APP_CONFIG.imageBaseUrl}${normalizedPath}`;
    console.log('[getImageUrl] 拼接后的完整URL:', fullUrl);
    return fullUrl;
  }

  // 否则返回原始路径
  console.log('[getImageUrl] 无配置，返回原始路径');
  return path;
}

/**
 * 获取图片列表的完整 URL
 * @param paths 逗号分隔的图片路径字符串
 * @returns 完整 URL 的数组
 */
export function getImageUrls(paths: string | undefined | null): string[] {
  if (!paths) return [];

  return paths
    .split(',')
    .filter(Boolean)
    .map(path => getImageUrl(path.trim()));
}
