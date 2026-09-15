import {
  Edit,
  TabbedForm,
  TextInput,
  PasswordInput,
  required,
} from "react-admin";
import PersonIcon from "@mui/icons-material/Person";
import GroupIcon from "@mui/icons-material/Group";
import LockIcon from "@mui/icons-material/Lock";
import { FormSection, TwoColGrid } from "../../components";
import { GroupedRolesInput } from "./GroupedRolesInput";

const UserEdit = () => (
  <Edit title="Edit User">
    <TabbedForm>
      <TabbedForm.Tab label="Account" icon={<PersonIcon />}>
        <FormSection
          title="Basic Info"
          subtitle="Username and contact details"
          icon={<PersonIcon />}
        >
          <TwoColGrid>
            <TextInput disabled source="id" fullWidth />
            <TextInput source="username" validate={required()} fullWidth />
          </TwoColGrid>
        </FormSection>

        <FormSection
          title="Security"
          subtitle="Leave blank to keep current password"
          icon={<LockIcon />}
        >
          <PasswordInput source="password" fullWidth sx={{ maxWidth: 400 }} />
        </FormSection>
      </TabbedForm.Tab>

      <TabbedForm.Tab label="Roles" icon={<GroupIcon />}>
        <FormSection
          title="Assign Roles"
          subtitle="System roles grant global access; department roles restrict to specific routes"
          icon={<GroupIcon />}
        >
          <GroupedRolesInput source="roles" />
        </FormSection>
      </TabbedForm.Tab>
    </TabbedForm>
  </Edit>
);

export default UserEdit;