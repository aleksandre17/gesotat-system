import { Create, SimpleForm, TextInput, required } from "react-admin";
import LockIcon from "@mui/icons-material/Lock";
import { FormSection } from "../../components/FormSection";

const PermissionCreate = () => (
  <Create title="Create Permission">
    <SimpleForm>
      <FormSection
        title="Permission"
        subtitle="Use SCREAMING_SNAKE_CASE (e.g. WRITE_RESOURCE)"
        icon={<LockIcon />}
      >
        <TextInput
          source="name"
          validate={required()}
          fullWidth
          sx={{ maxWidth: 400 }}
        />
      </FormSection>
    </SimpleForm>
  </Create>
);

export default PermissionCreate;