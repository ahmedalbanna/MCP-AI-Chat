import * as SecureStore from 'expo-secure-store';

const COOKIE_KEY = 'waf_cookies';
const COOKIE_DOMAIN_KEY = 'waf_cookie_domain';

export function isWafResponse(body: string): boolean {
  return body.includes('aliyun_waf') || body.includes('Access Verification');
}

export async function saveWafCookies(cookies: string, domain: string): Promise<void> {
  await SecureStore.setItemAsync(COOKIE_KEY, cookies);
  await SecureStore.setItemAsync(COOKIE_DOMAIN_KEY, domain);
}

export async function getWafCookies(): Promise<{ cookies: string; domain: string } | null> {
  const cookies = await SecureStore.getItemAsync(COOKIE_KEY);
  const domain = await SecureStore.getItemAsync(COOKIE_DOMAIN_KEY);
  if (cookies && domain) return { cookies, domain };
  return null;
}

export async function clearWafCookies(): Promise<void> {
  await SecureStore.deleteItemAsync(COOKIE_KEY);
  await SecureStore.deleteItemAsync(COOKIE_DOMAIN_KEY);
}

export function buildCookieHeader(cookies: Record<string, string>): string {
  return Object.entries(cookies)
    .map(([k, v]) => `${k}=${v}`)
    .join('; ');
}

export function parseCookieString(str: string): Record<string, string> {
  const out: Record<string, string> = {};
  str.split(';').forEach((part) => {
    const [k, ...rest] = part.split('=');
    if (k) out[k.trim()] = rest.join('=').trim();
  });
  return out;
}
