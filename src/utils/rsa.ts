/**
 * RSA 加密工具
 * 使用 JSEncrypt 库进行 RSA 公钥加密
 */

import { JSEncrypt } from 'jsencrypt';

// RSA 公钥（从后端提供）
const RSA_PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCvTXtdGfd6XjPO1wHDX+yRQfg1
jne0mdSZjpS/I2RPqDryUQdej2uRSTkld/f1RKrHCX2B3atlahQaWf2AmMFYhY5W
ncpMSUeN7BpBLKoaha5/CS8g5GAigOkAMbypOIoTqoqYxNJcue/jNdxpXTM4mYkg
vOCTacce+v2tihuKhQIDAQAB
-----END PUBLIC KEY-----`;

// 创建 JSEncrypt 实例
const encrypt = new JSEncrypt();
encrypt.setPublicKey(RSA_PUBLIC_KEY);

/**
 * 使用 RSA 公钥加密数据
 * @param text 待加密的文本
 * @returns 加密后的 Base64 字符串
 */
export function encryptWithPublicKey(text: string): string {
  try {
    const encrypted = encrypt.encrypt(text);
    if (!encrypted) {
      throw new Error('加密失败');
    }
    return encrypted;
  } catch (error) {
    console.error('RSA 加密失败:', error);
    throw new Error('加密失败');
  }
}

/**
 * 加密登录凭证
 * @param accountNo 账号
 * @param password 密码
 * @returns 加密后的账号和密码
 */
export function encryptLoginCredentials(
  accountNo: string,
  password: string
): { accountNo: string; password: string } {
  return {
    accountNo: encryptWithPublicKey(accountNo),
    password: encryptWithPublicKey(password),
  };
}
