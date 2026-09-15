import { DataProvider } from "react-admin";
import { SettingsDataProvider } from "../providers/settingsDataProvider.ts";
import { MenuItems } from "../types/treeCategories";

export type SettingsData = MenuItems;

export class SettingsManager {
  private static instance: SettingsManager;
  private settings: SettingsData | null = null;
  private subscribers: Set<(settings: SettingsData | null) => void> = new Set();
  private dataProvider: (DataProvider & SettingsDataProvider) | null = null;
  private isInitialized = false; // Track initialization

  private constructor() {}

  public static getInstance(): SettingsManager {
    if (!SettingsManager.instance) {
      SettingsManager.instance = new SettingsManager();
    }
    return SettingsManager.instance;
  }

  public setDataProvider(provider: DataProvider & SettingsDataProvider) {
    if (this.dataProvider !== provider) {
      this.dataProvider = provider;
      this.isInitialized = false;
    }
    return SettingsManager.instance;
  }

  async loadSettings() {
    if (!this.dataProvider) {
      throw new Error("DataProvider not initialized");
    }

    if (this.isInitialized && this.settings) {
      return Promise.resolve(this.settings);
    }
    return this.dataProvider
      .getTreeCategories<SettingsData>("settings")
      .then(({ data }) => {
        this.settings = data;
        this.isInitialized = true;
        this.notifySubscribers();
        return data;
      })
      .catch((error: unknown) => {
        this.isInitialized = false;
        throw error;
      });
  }

  public getSettings(): SettingsData | null {
    return this.settings;
  }

  public setData(data: SettingsData) {
    this.settings = data;
    this.isInitialized = true;
    this.notifySubscribers();
  }

  public async updateSettings(
    newSettings: { data: MenuItems },
  ): Promise<SettingsData | null> {
    if (!this.dataProvider) {
      throw new Error("DataProvider not initialized");
    }
    const { data } = await this.dataProvider.applySettings<SettingsData>(
      "settings",
      newSettings,
    );
    this.settings = data;
    this.isInitialized = true;
    this.notifySubscribers();
    return data;
  }

  public subscribe(
    callback: (settings: SettingsData | null) => void,
  ): () => void {
    if (!this.subscribers.has(callback)) {
      this.subscribers.add(callback);
      if (this.settings !== null) {
        callback(this.settings); // Explicitly handle null
      }
    }
    return () => this.subscribers.delete(callback);
  }

  private notifySubscribers() {
    if (this.settings !== null) {
      this.subscribers.forEach((callback) => callback(this.settings));
    }
  }

  public async reset(): Promise<SettingsData | null> {
    this.settings = null;
    this.isInitialized = false;
    return this.loadSettings();
  }
}