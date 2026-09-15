import {
  TextInput,
  BooleanInput,
  NumberInput,
  ReferenceInput,
  SelectInput,
  required,
} from "react-admin";
import { useWatch } from "react-hook-form";
import { Box, Typography } from "@mui/material";
import FolderOpenIcon from "@mui/icons-material/FolderOpen";
import ArticleIcon from "@mui/icons-material/Article";
import StorageIcon from "@mui/icons-material/Storage";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import { FormSection, TwoColGrid } from "../../../components";
import IconPickerInput from "./IconPickerInput";

const DB_TYPE_CHOICES = [
  { id: "MySql", name: "MySql" },
  { id: "MSSql", name: "MSSql" },
];

interface PageFormContentProps {
  isEditing?: boolean;
}

const PageFormContent = ({ isEditing = false }: PageFormContentProps) => {
  //const record = useRecordContext();
  const isFolder = useWatch({ name: "isFolder" });
  const parentFilter = { nodeType: "DIRECTORY" };

  return (
    <Box
      sx={{ display: "flex", flexDirection: "column", gap: 1, width: "100%" }}
    >
      {/* ── Type toggle ─────────────────────────────────── */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          px: 0,
          pb: 1,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          {isFolder ? (
            <FolderOpenIcon fontSize="small" color="action" />
          ) : (
            <ArticleIcon fontSize="small" color="action" />
          )}
          <Typography variant="body2" color="text.secondary">
            {isFolder
              ? "Directory — groups child pages in the menu"
              : "Page — a leaf route with its own data upload"}
          </Typography>
        </Box>
        <BooleanInput
          source="isFolder"
          label="Is Folder"
          helperText={false}
          sx={{ mb: 0 }}
        />
      </Box>

      {/* ── Basic info ───────────────────────────────────── */}
      <FormSection
        title="Basic Information"
        subtitle="Identity and appearance in the menu"
        icon={isFolder ? <FolderOpenIcon /> : <ArticleIcon />}
      >
        {isEditing && (
          <Box sx={{ mb: 2 }}>
            <TextInput
              disabled
              source="id"
              helperText={false}
              sx={{ maxWidth: 100 }}
            />
          </Box>
        )}
        <TwoColGrid>
          <TextInput
            source="name"
            validate={required()}
            fullWidth
            helperText="Label shown in the sidebar"
          />
          <TextInput
            source="slug"
            validate={required()}
            fullWidth
            helperText="URL segment, e.g. prices"
          />
        </TwoColGrid>
        <TwoColGrid>
          <NumberInput
            source="level"
            fullWidth
            helperText="Nesting depth (0 = top)"
          />
          <NumberInput
            source="orderIndex"
            defaultValue={0}
            fullWidth
            helperText="Sort order within parent"
          />
        </TwoColGrid>
        <Box sx={{ mt: 1 }}>
          <IconPickerInput
            source="icon"
            helperText="Icon displayed next to the label"
          />
        </Box>
      </FormSection>

      {/* ── Folder: description ──────────────────────────── */}
      {isFolder && (
        <FormSection
          title="Description"
          subtitle="Optional description for this folder"
          icon={<FolderOpenIcon />}
        >
          <TextInput
            source="description"
            multiline
            minRows={3}
            fullWidth
            helperText={false}
          />
        </FormSection>
      )}

      {/* ── Page: content ────────────────────────────────── */}
      {!isFolder && (
        <FormSection
          title="Content"
          subtitle="Resource endpoint and page metadata"
          icon={<ArticleIcon />}
        >
          <TextInput
            source="resource"
            fullWidth
            helperText="Backend resource path"
            sx={{ mb: 1 }}
          />
          <TwoColGrid>
            <TextInput
              source="metaTitle"
              fullWidth
              helperText="Browser tab title"
            />
            <TextInput
              source="metaDescription"
              fullWidth
              helperText="Short page description"
            />
          </TwoColGrid>
        </FormSection>
      )}

      {/* ── Page: database connection ─────────────────────── */}
      {!isFolder && (
        <FormSection
          title="Database Connection"
          subtitle="Source database credentials for this page"
          icon={<StorageIcon />}
        >
          <Box sx={{ mb: 2 }}>
            <SelectInput
              source="metaDatabaseType"
              label="Type"
              choices={DB_TYPE_CHOICES}
              helperText={false}
              sx={{ minWidth: 180 }}
            />
          </Box>
          <TwoColGrid>
            <TextInput
              source="metaDatabaseUrl"
              label="Host / URL"
              fullWidth
              helperText={false}
            />
            <TextInput
              source="metaDatabaseName"
              label="Database Name"
              fullWidth
              helperText={false}
            />
          </TwoColGrid>
          <TwoColGrid>
            <TextInput
              source="metaDatabaseUser"
              label="Username"
              fullWidth
              helperText={false}
            />
            <TextInput
              source="metaDatabasePassword"
              label="Password"
              type="password"
              fullWidth
              helperText={false}
            />
          </TwoColGrid>
        </FormSection>
      )}

      {/* ── Structure ─────────────────────────────────────── */}
      <FormSection
        title="Structure"
        subtitle="Parent folder in the menu hierarchy"
        icon={<AccountTreeIcon />}
      >
        <ReferenceInput
          source="parentId"
          reference="pages"
          filter={parentFilter}
        >
          <SelectInput
            optionText="name"
            emptyText="— Root level (no parent)"
            fullWidth
            helperText={false}
          />
        </ReferenceInput>
      </FormSection>

    </Box>
  );
};

export default PageFormContent;