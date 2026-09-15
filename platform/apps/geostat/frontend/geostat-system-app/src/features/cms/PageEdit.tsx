import { Edit, TabbedForm, EditProps } from "react-admin";
import type { RaRecord, Identifier } from "react-admin";
import { useWatch } from "react-hook-form";
import { Box, Typography } from "@mui/material";
import ArticleIcon from "@mui/icons-material/Article";
import SecurityIcon from "@mui/icons-material/Security";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import type { NodeType, PageRecord } from "./types";
import PageFormContent from "./components/PageFormContent";
import { AccessControlPanel } from "./components/AccessControlPanel";
import { FormSection } from "../../components";

type EditRecord = RaRecord<Identifier> &
  Partial<PageRecord> & { isFolder?: boolean };

const transformIncoming = (
  record: RaRecord<Identifier>,
): RaRecord<Identifier> => ({
  ...record,
  isFolder: record.nodeType === "DIRECTORY",
});

const transformOutgoing = (data: EditRecord): EditRecord => {
  const isDirectory = data.isFolder;
  const base = {
    id: data.id,
    name: data.name,
    level: data.level,
    slug: data.slug,
    icon: data.icon,
    parentId: data.parentId,
    orderIndex: data.orderIndex,
    nodeType: (isDirectory ? "DIRECTORY" : "PAGE") as NodeType,
    isFolder: isDirectory,
  };

  if (isDirectory) {
    return {
      ...base,
      description: data.description,
      accessControl: {
        userId: data.accessControl?.userId ?? null,
        roleId: data.accessControl?.roleId ?? null,
        permissionId: data.accessControl?.permissionId ?? null,
      },
      resource: null,
      metaDatabaseType: null,
      metaDatabaseUrl: null,
      metaDatabaseUser: null,
      metaDatabasePassword: null,
      metaDescription: null,
      metaDatabaseName: null,
      metaTitle: null,
    };
  }

  return {
    ...base,
    resource: data.resource,
    metaTitle: data.metaTitle,
    metaDatabaseType: data.metaDatabaseType,
    metaDatabaseUrl: data.metaDatabaseUrl,
    metaDatabaseUser: data.metaDatabaseUser,
    metaDatabasePassword: data.metaDatabasePassword,
    metaDescription: data.metaDescription,
    metaDatabaseName: data.metaDatabaseName,
    description: null,
    accessControl: null,
  };
};

// Tab 2 content — shown inside TabbedForm so useWatch works within form context
const AccessControlTab = () => {
  const isFolder = useWatch({ name: "isFolder" });

  if (!isFolder) {
    return (
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 1.5,
          py: 6,
          color: "text.disabled",
        }}
      >
        <LockOpenIcon sx={{ fontSize: 40, opacity: 0.4 }} />
        <Typography variant="body2" color="text.secondary">
          Access control is only available for folder nodes.
        </Typography>
        <Typography variant="caption" color="text.disabled">
          Switch the type to <strong>Folder</strong> to configure restrictions.
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ width: "100%" }}>
      <FormSection
        title="Access Control"
        subtitle="Restrict access to this directory and its children"
        icon={<SecurityIcon />}
      >
        <AccessControlPanel />
      </FormSection>
    </Box>
  );
};

const PageEdit = (props: EditProps) => (
  <Edit
    {...props}
    title="Edit Page"
    transform={transformOutgoing}
    queryOptions={{ select: transformIncoming }}
  >
    <TabbedForm>
      <TabbedForm.Tab label="Content" icon={<ArticleIcon />}>
        <PageFormContent isEditing />
      </TabbedForm.Tab>

      <TabbedForm.Tab label="Access Control" icon={<SecurityIcon />}>
        <AccessControlTab />
      </TabbedForm.Tab>
    </TabbedForm>
  </Edit>
);

export default PageEdit;
