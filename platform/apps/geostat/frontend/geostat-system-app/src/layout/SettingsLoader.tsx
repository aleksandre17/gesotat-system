import { useEffect } from "react";
import { useGetList } from "react-admin";
import { SettingsManager } from "../services/SettingsManager";
import type { PageRecord } from "../features/cms/types";
import type { MenuItems } from "../types/treeCategories";
import { PAGES_QUERY, PAGES_CACHE } from "../config";

export const SettingsLoader = () => {
  const { data } = useGetList<PageRecord>("pages", PAGES_QUERY, PAGES_CACHE);

  useEffect(() => {
    if (!data) return;
    SettingsManager.getInstance().setData(data as unknown as MenuItems);
  }, [data]);

  return null;
};