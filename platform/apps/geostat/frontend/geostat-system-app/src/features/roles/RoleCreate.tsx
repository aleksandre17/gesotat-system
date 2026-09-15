import { Create, SimpleForm, TextInput, required } from "react-admin";
import GroupIcon from "@mui/icons-material/Group";
import LockIcon from "@mui/icons-material/Lock";
import { FormSection } from "../../components/FormSection";
import { GroupedPermissionsInput } from "./GroupedPermissionsInput";

const RoleCreate = () => (
  <Create title="Create Role">
    <SimpleForm>
      <FormSection
        title="Role Details"
        subtitle="Define the role name"
        icon={<GroupIcon />}
      >
        <TextInput
          source="name"
          validate={required()}
          fullWidth
          sx={{ maxWidth: 400 }}
        />
      </FormSection>

      <FormSection
        title="Permissions"
        subtitle="Select permissions granted to this role"
        icon={<LockIcon />}
      >
        <GroupedPermissionsInput source="permissions" />
      </FormSection>
    </SimpleForm>
  </Create>
);

export default RoleCreate;