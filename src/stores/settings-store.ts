import { create } from 'zustand';
import { SettingsState } from '../types';
import {
  saveApiKey,
  getApiKey,
  saveBaseUrl,
  getBaseUrl,
  saveModel,
  getModel,
} from '../services/storage';

export const useSettingsStore = create<SettingsState>((set) => ({
  apiKey: 'sk-XKnp5MEgQXtZNDTkwokwo09yUfFl2DBSfnRdq6rTamqpShmf',
  baseUrl: 'https://agentrouter.org',
  model: 'claude-opus-4-8',
  themeMode: 'system',

  setApiKey: (key: string) => {
    saveApiKey(key);
    set({ apiKey: key });
  },

  setBaseUrl: (url: string) => {
    saveBaseUrl(url);
    set({ baseUrl: url });
  },

  setModel: (model: string) => {
    saveModel(model);
    set({ model });
  },

  setThemeMode: (mode: 'light' | 'dark' | 'system') => {
    set({ themeMode: mode });
  },
}));

export async function loadSettings(): Promise<void> {
  const [apiKey, baseUrl, model] = await Promise.all([
    getApiKey(),
    getBaseUrl(),
    getModel(),
  ]);
  useSettingsStore.setState({ apiKey, baseUrl, model });
}
