import {
  createContext,
  useContext,
  useEffect,
  useState,
  useMemo,
  ReactNode,
  useCallback,
} from "react";
import {
  SettingsManager,
  SettingsData,
} from "../services/SettingsManager";
import { SettingsDataProvider } from "../providers/settingsDataProvider.ts";
import type { MenuItems } from "../types/treeCategories";

interface SettingsContextType {
  settings: SettingsData | null;
  setSettings: (settings: SettingsData | null) => void;
  reloadSettings: () => Promise<void>;
  updateSettings: (newSettings: { data: MenuItems }) => Promise<void>;
  error: Error | null;
  loading: boolean;
  setError: (error: Error | null) => void;
}

const SettingsContext = createContext<SettingsContextType | undefined>(
  undefined,
);

export const SettingsProvider = ({
  children,
  dataProvider,
}: {
  children: ReactNode;
  dataProvider: SettingsDataProvider;
}) => {
  const [settings, setSettings] = useState<SettingsData | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const manager = SettingsManager.getInstance().setDataProvider(dataProvider);

    const unsubscribe = manager.subscribe((settings) => {
      setSettings(settings);
      setLoading(false);
    });
    return () => {
      unsubscribe();
    };
  }, [dataProvider]);

  const reloadSettings = useCallback(async () => {
    const manager = SettingsManager.getInstance().setDataProvider(dataProvider);
    try {
      setError(null);
      await manager.reset();
    } catch (err) {
      setError(
        err instanceof Error ? err : new Error("Failed to reload settings"),
      );
    } finally {
      setLoading(false);
    }
  }, [dataProvider]);

  const updateSettings = useCallback(
    async (newSettings: { data: MenuItems }) => {
      const manager =
        SettingsManager.getInstance().setDataProvider(dataProvider);
      try {
        setError(null);
        await manager.updateSettings(newSettings);
      } catch (err) {
        setError(
          err instanceof Error ? err : new Error("Failed to update settings"),
        );
      } finally {
        setLoading(false);
      }
    },
    [dataProvider],
  );

  const contextValue = useMemo(
    () => ({
      settings,
      setSettings,
      reloadSettings,
      updateSettings,
      error,
      loading,
      setError,
    }),
    [settings, reloadSettings, updateSettings, error, loading],
  );

  return (
    <SettingsContext.Provider value={contextValue}>
      {children}
    </SettingsContext.Provider>
  );
};

export const useSettings = () => {
  const ctx = useContext(SettingsContext);
  if (!ctx)
    throw new Error("useSettings must be used within a SettingsProvider");
  return ctx;
};
