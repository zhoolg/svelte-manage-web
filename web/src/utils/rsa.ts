/**
 * RSA-OAEP（SHA-256）加密工具，基于浏览器原生 WebCrypto。
 *
 * 设计：后端可能使用临时 RSA 密钥（每次启动变化），因此公钥在每次登录加密前
 * 实时从后端获取，不再硬编码，也不跨会话缓存，避免密钥轮换后出现陈旧公钥。
 */
import { BASE_URL } from '../api/request';

function base64ToArrayBuffer(b64: string): ArrayBuffer {
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

/** 从后端获取当前公钥并导入为 WebCrypto 加密密钥 */
async function fetchPublicKey(): Promise<CryptoKey> {
  const res = await fetch(`${BASE_URL}/admin/auth/public-key`, { credentials: 'include' });
  if (!res.ok) {
    throw new Error('获取加密公钥失败');
  }
  const json = await res.json();
  const spki: string | undefined = json?.data?.publicKey;
  if (!spki) {
    throw new Error('加密公钥无效');
  }
  return crypto.subtle.importKey(
    'spki',
    base64ToArrayBuffer(spki),
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt']
  );
}

async function encryptText(key: CryptoKey, text: string): Promise<string> {
  const encrypted = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    key,
    new TextEncoder().encode(text)
  );
  return arrayBufferToBase64(encrypted);
}

/**
 * 使用后端公钥对单段文本做 RSA-OAEP 加密，返回 Base64 密文。
 */
export async function encryptWithPublicKey(text: string): Promise<string> {
  const key = await fetchPublicKey();
  return encryptText(key, text);
}

/**
 * 加密登录凭证：单次获取公钥，并行加密账号与密码。
 */
export async function encryptLoginCredentials(
  accountNo: string,
  password: string
): Promise<{ accountNo: string; password: string }> {
  const key = await fetchPublicKey();
  const [encAccount, encPassword] = await Promise.all([
    encryptText(key, accountNo),
    encryptText(key, password),
  ]);
  return { accountNo: encAccount, password: encPassword };
}
