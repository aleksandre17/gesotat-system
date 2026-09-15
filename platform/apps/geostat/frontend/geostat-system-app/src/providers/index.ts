import { DataProvider } from "react-admin";
import { springDataProvider } from "./springDataProvider";
import { dashboardDataProvider } from "./dashbaordDataProvider.ts";
import { userDataProvider } from "./userDataProvider.ts";
import { pagesDataProvider } from "./pagesDataProvider";

interface ProviderConfig {
  resources: string[];
  provider: DataProvider;
}

const providerConfigs: ProviderConfig[] = [
  {
    resources: ["dashboard"],
    provider: dashboardDataProvider,
  },
  {
    resources: ["users", "roles", "permissions"],
    provider: userDataProvider,
  },
  {
    resources: ["pages"],
    provider: pagesDataProvider,
  },
  {
    resources: ["*"],
    provider: springDataProvider,
  },
];

const dataProviderFactory = (): DataProvider => {
  return new Proxy({} as DataProvider, {
    get(_, name) {
      if (name === "then") return undefined;

      return (resource: string, params: never) => {
        const config =
          providerConfigs.find((c) => c.resources.includes(resource)) ??
          providerConfigs.find((c) => c.resources.includes("*"));

        if (!config) {
          throw new Error(`No provider configured for resource: ${resource}`);
        }

        const method = name.toString() as keyof DataProvider;
        return config.provider[method](resource, params);
      };
    },
  });
};

export default dataProviderFactory;
