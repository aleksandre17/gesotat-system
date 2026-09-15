import { Create, SimpleForm, CreateProps } from "react-admin";
import type { NodeType, PageRecord } from "./types";
import PageFormContent from "./components/PageFormContent";

const transform = (
  data: Partial<PageRecord> & { isFolder?: boolean },
): Omit<PageRecord, "id"> => {
  const isDirectory = data.isFolder;
  const base = {
    name: data.name!,
    level: data.level!,
    slug: data.slug!,
    icon: data.icon,
    parentId: data.parentId,
    orderIndex: data.orderIndex ?? 0,
    nodeType: (isDirectory ? "DIRECTORY" : "PAGE") as NodeType,
    isFolder: isDirectory,
  };

  if (isDirectory) {
    return { ...base, description: data.description };
  }

  return {
    ...base,
    resource: data.resource,
    metaTitle: data.metaTitle,
    metaDescription: data.metaDescription,
  };
};

const PageCreate = (props: CreateProps) => (
  <Create {...props} title="Create Page" transform={transform}>
    <SimpleForm>
      <PageFormContent />
    </SimpleForm>
  </Create>
);

export default PageCreate;
