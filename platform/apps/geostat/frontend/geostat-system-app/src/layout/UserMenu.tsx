import { UserMenu, Logout } from "react-admin";
import { MenuItem, ListItemIcon, ListItemText } from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import { Link } from "react-router-dom";

export const CustomUserMenu = () => (
  <UserMenu>
    <MenuItem component={Link} to="/profile">
      <ListItemIcon>
        <PersonIcon fontSize="small" />
      </ListItemIcon>
      <ListItemText>Profile</ListItemText>
    </MenuItem>
    <Logout />
  </UserMenu>
);
