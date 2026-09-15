import {
  List,
  Datagrid,
  TextField,
  EmailField,
  ArrayField,
  SingleFieldList,
  ChipField,
  EditButton,
  DeleteButton,
} from "react-admin";
import { useHasPermission } from "../../auth";
import { PERMISSIONS } from "../../auth";

const UserList = () => {
  const canManage = useHasPermission(PERMISSIONS.admin.manageUsers);

  return (
    <List title="Users" sort={{ field: "id", order: "ASC" }}>
      <Datagrid bulkActionButtons={canManage ? undefined : false}>
        <TextField source="id" />
        <TextField source="username" />
        <ArrayField source="roles" sortable={false}>
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

export default UserList;