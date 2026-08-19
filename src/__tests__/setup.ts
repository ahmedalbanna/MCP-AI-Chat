import { vi } from 'vitest';

const store: Record<string, string> = {};

vi.mock('expo-secure-store', () => ({
  setItemAsync: vi.fn(async (key: string, value: string) => {
    store[key] = value;
  }),
  getItemAsync: vi.fn(async (key: string) => store[key] ?? null),
  deleteItemAsync: vi.fn(async (key: string) => {
    delete store[key];
  }),
}));

vi.mock('react-native', () => ({
  useColorScheme: () => 'light',
  Platform: { OS: 'web', select: (obj: any) => obj.web },
  StyleSheet: { create: (s: any) => s, flatten: (s: any) => s },
}));

vi.mock('react-native-webview', () => ({
  WebView: () => null,
}));

vi.mock('expo-router', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  useLocalSearchParams: () => ({}),
}));

export function resetStore() {
  Object.keys(store).forEach((k) => delete store[k]);
}
