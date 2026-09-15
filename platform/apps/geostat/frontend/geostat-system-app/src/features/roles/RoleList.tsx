import {
  List,
  Datagrid,
  TextField,
  ArrayField,
  SingleFieldList,
  ChipField,
  EditButton,
  DeleteButton,
} from "react-admin";
import { useHasPermission, PERMISSIONS } from "../../auth";

const RoleList = () => {
  const canManage = useHasPermission(PERMISSIONS.admin.manageRoles);

  return (
    <List title="Roles" sort={{ field: "id", order: "ASC" }}>
      <Datagrid bulkActionButtons={canManage ? undefined : false}>
        <TextField source="id" />
        <TextField source="name" />
        <ArrayField source="permissions" sortable={false}>
          <SingleFieldList linkType={false}>
            <ChipField source="name" size="small" />
          </SingleFieldList>
        </ArrayField>
        {canManage && <EditButton />}
        {canManage && <DeleteButton />}
      </Datagrid>
    </List>
  );
};

export default RoleList;