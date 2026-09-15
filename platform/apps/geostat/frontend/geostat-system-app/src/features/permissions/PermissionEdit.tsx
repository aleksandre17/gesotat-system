import { Edit, SimpleForm, TextInput, required } from "react-admin";
import LockIcon from "@mui/icons-material/Lock";
import { FormSection, TwoColGrid } from "../../components";

const PermissionEdit = () => (
  <Edit title="Edit Permission">
    <SimpleForm>
      <FormSection
        title="Permission"
        subtitle="Use SCREAMING_SNAKE_CASE (e.g. WRITE_RESOURCE)"
        icon={<LockIcon />}
      >
        <TwoColGrid>
          <TextInput disabled source="id" fullWidth />
          <TextInput source="name" validate={required()} fullWidth />
        </TwoColGrid>
      </FormSection>
    </SimpleForm>
  </Edit>
);

export default PermissionEdit;