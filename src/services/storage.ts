import * as SecureStore from 'expo-secure-store';

const KEYS = {
  API_KEY: 'anthropic_api_key',
  BASE_URL: 'anthropic_base_url',
  THEME_MODE: 'theme_mode',
  MODEL: 'selected_model',
} as const;

const DEFAULTS = {
  API_KEY: 'sk-XKnp5MEgQXtZNDTkwokwo09yUfFl2DBSfnRdq6rTamqpShmf',
  BASE_URL: 'https://agentrouter.org',
  MODEL: 'claude-opus-4-8',
} as const;

export async function saveApiKey(key: string): Promise<void> {
  await SecureStore.setItemAsync(KEYS.API_KEY, key);
}

export async function getApiKey(): Promise<string> {
  return (await SecureStore.getItemAsync(KEYS.API_KEY)) ?? DEFAULTS.API_KEY;
}

export async function deleteApiKey(): Promise<void> {
  await SecureStore.deleteItemAsync(KEYS.API_KEY);
}

export async function saveBaseUrl(url: string): Promise<void> {
  await SecureStore.setItemAsync(KEYS.BASE_URL, url);
}

export async function getBaseUrl(): Promise<string> {
  return (await SecureStore.getItemAsync(KEYS.BASE_URL)) ?? DEFAULTS.BASE_URL;
}

export async function saveThemeMode(mode: string): Promise<void> {
  await SecureStore.setItemAsync(KEYS.THEME_MODE, mode);
}

export async function getThemeMode(): Promise<string> {
  return (await SecureStore.getItemAsync(KEYS.THEME_MODE)) ?? 'system';
}

export async function saveModel(model: string): Promise<void> {
  await SecureStore.setItemAsync(KEYS.MODEL, model);
}

export async function getModel(): Promise<string> {
  return (await SecureStore.getItemAsync(KEYS.MODEL)) ?? DEFAULTS.MODEL;
}
