import { DataProvider } from "react-admin";
import { springDataProvider } from "./springDataProvider";
import { httpClient } from "../api";
import { MenuItems } from "../types";
import { ENV } from "../config";

const apiUrl = ENV.API_URL;

export interface SettingsDataProvider extends DataProvider {
  getTreeCategories: <T = MenuItems>(resource: string) => Promise<{ data: T }>;
  applySettings: <T = MenuItems>(
    resource: string,
    params: Partial<{ data: MenuItems }>,
  ) => Promise<{ data: T }>;
  resetSettings: <T = void>(resource: string) => Promise<{ data: T }>;
}

export const settingsDataProvider: DataProvider & SettingsDataProvider = {
  ...springDataProvider,
  getTreeCategories: async <T = MenuItems>(_resource: string) => {
    const { json } = await httpClient(`${apiUrl}/v1/pages/roots`, {
      method: "GET",
    });
    return { data: json as T };
  },
  applySettings: async <T = MenuItems>(
    _resource: string,
    params: Partial<{ data: MenuItems }>,
  ) => {
    const { json } = await httpClient(`${apiUrl}/settings/apply`, {
      method: "POST",
      body: JSON.stringify(params),
    });
    return Promise.resolve({ data: json as T });
  },

  resetSettings: async <T = never>() => {
    const { json } = await httpClient(`${apiUrl}/settings/reset`, {
      method: "POST",
    });
    return { data: json as T };
  },
};
