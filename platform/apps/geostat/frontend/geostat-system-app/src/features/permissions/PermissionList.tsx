import {
  List,
  Datagrid,
  TextField,
  EditButton,
  DeleteButton,
} from "react-admin";
import { useHasPermission, PERMISSIONS } from "../../auth";

const PermissionList = () => {
  const canManage = useHasPermission(PERMISSIONS.admin.managePermissions);

  return (
    <List title="Permissions" sort={{ field: "id", order: "ASC" }}>
      <Datagrid bulkActionButtons={canManage ? undefined : false}>
        <TextField source="id" />
        <TextField source="name" />
        {canManage && <EditButton />}
        {canManage && <DeleteButton />}
      </Datagrid>
    </List>
  );
};

export default PermissionList;