import type { ReactNode } from "react";
import {
  Layout as RALayout,
  AppBar,
  CheckForApplicationUpdate,
  Sidebar,
} from "react-admin";
import { styled } from "@mui/material/styles";
import Menu from "./menu/Menu";
import { SettingsLoader } from "./SettingsLoader";
import { HttpErrorBoundary } from "../auth/guards";
import { CustomUserMenu } from "./UserMenu";

const CustomSidebar = styled(Sidebar)(() => ({
  width: 392,
  "& .MuiDrawer-paper": {
    width: 350,
  },
}));

const CustomAppBar = () => <AppBar userMenu={<CustomUserMenu />} />;

export const AppLayout = ({ children }: { children: ReactNode }) => (
  <RALayout menu={Menu} sidebar={CustomSidebar} appBar={CustomAppBar}>
    <SettingsLoader />
    <HttpErrorBoundary>{children}</HttpErrorBoundary>
    <CheckForApplicationUpdate />
  </RALayout>
);
