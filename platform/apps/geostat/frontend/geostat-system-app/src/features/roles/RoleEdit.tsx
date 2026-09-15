import { Edit, SimpleForm, TextInput, required } from "react-admin";
import GroupIcon from "@mui/icons-material/Group";
import LockIcon from "@mui/icons-material/Lock";
import { FormSection, TwoColGrid } from "../../components";
import { GroupedPermissionsInput } from "./GroupedPermissionsInput";

const transformRole = (data: Record<string, unknown>) => ({
  id: data.id,
  name: data.name,
  permissions: ((data.permissions as unknown[]) ?? []).map((p) =>
    typeof p === "number" ? { id: p } : { id: (p as { id: number }).id },
  ),
});

const RoleEdit = () => (
  <Edit title="Edit Role" transform={transformRole}>
    <SimpleForm>
      <FormSection
        title="Role Details"
        subtitle="Define the role name"
        icon={<GroupIcon />}
      >
        <TwoColGrid>
          <TextInput disabled source="id" fullWidth />
          <TextInput source="name" validate={required()} fullWidth />
        </TwoColGrid>
      </FormSection>

      <FormSection
        title="Permissions"
        subtitle="Select permissions granted to this role"
        icon={<LockIcon />}
      >
        <GroupedPermissionsInput source="permissions" />
      </FormSection>
    </SimpleForm>
  </Edit>
);

export default RoleEdit;