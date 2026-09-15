const cache: Record<string, boolean> = {};

const getComponent = (Icon: any): any => {
  if (typeof Icon === "function") return Icon;
  if (typeof Icon !== "object" || !Icon) return null;
  if (typeof Icon.type === "function") return Icon.type;
  if (typeof Icon.type === "object" && Icon.type?.render) return Icon.type.render;
  return null;
};

async function loadAllIcons(page = 1, pageSize = 20, search = "") {
  const start = (page - 1) * pageSize;
  const end = page * pageSize;

  console.log("Start");

  try {
    const mod = await import(`@mui-icons`);
    const allKeys = Object.keys(mod);

    // Apply filtering logic
    const filteredKeys = allKeys.filter((key) => {
      const Icon = mod[key];
      const component = getComponent(Icon);
      const isComponent = !!component;
      const isValidName = !["createSvgIcon"].includes(key);
      const isFilled = !key.match(/(Outlined|TwoTone|Rounded|Sharp)$/);
      const matchesSearch = !search || key.toLowerCase().includes(search.toLowerCase());
      return isComponent && isValidName && isFilled && matchesSearch;
    });

    const batchKeys = filteredKeys.slice(start, end);
    const pageIcons: string[] = [];

    const imports = batchKeys.map(async (iconName) => {
      if (cache[iconName]) {
        pageIcons.push(iconName);
        return;
      }

      try {
        //await import(`@mui-icons/${iconName}.js`);
        cache[iconName] = true;
        pageIcons.push(iconName);
      } catch (err) {
        console.error(`Failed to load icon ${iconName}`, err);
      }
    });

    await Promise.all(imports);

    // Calculate total pages
    const totalIcons = filteredKeys.length;
    const totalPages = Math.ceil(totalIcons / pageSize);

    self.postMessage({
      type: "iconsPage",
      icons: pageIcons,
      page,
      pageSize,
      totalPages,
    });
  } catch (e) {
    console.error("Failed to load icon index:", e);
    self.postMessage({ type: "error", message: "Failed to load icons index" });
  }
}

self.onmessage = (e) => {
  if (e.data?.type === "loadIconsPage") {
    const { page, pageSize, search } = e.data;
    loadAllIcons(page, pageSize, search);
  }
};