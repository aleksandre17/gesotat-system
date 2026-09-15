export interface SettingsHookReturn<T> {
  settings: T | null;
  loading: boolean;
  error: Error | null;
  updateSettings: (newSettings: Partial<T>) => Promise<T>;
}