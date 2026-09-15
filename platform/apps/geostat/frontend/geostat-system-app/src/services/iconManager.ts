import React, { useEffect, useRef, useState, useCallback } from "react";

const getComponent = (Icon: any): any => {
  if (typeof Icon === "function") return Icon;
  if (typeof Icon !== "object" || !Icon) return null;
  if (typeof Icon.type === "function") return Icon.type;
  if (typeof Icon.type === "object" && Icon.type?.render) return Icon.type.render;
  return null;
};

export function useIconWorkerManager() {
  const [icons, setIcons] = useState<Record<string, any>>({});
  const [totalPages, setTotalPages] = useState(1);
  const workerRef = useRef<Worker | null>(null);
  const iconCache = useRef<Record<string, any>>({});

  useEffect(() => {
    const worker = new Worker(
      new URL("../workers/iconWorker", import.meta.url),
      {
        type: "module",
      },
    );
    workerRef.current = worker;

    worker.onmessage = async (e: MessageEvent) => {
      if (e.data.type === "iconsPage") {
        const iconNames: string[] = e.data.icons;
        const loadedIcons: Record<string, any> = {};

        setTotalPages(e.data.totalPages || 1);

        await Promise.all(
          iconNames.map(async (iconName) => {
            if (iconCache.current[iconName]) {
              loadedIcons[iconName] = iconCache.current[iconName];
              return;
            }

            try {
              const iconMode = React.lazy(() =>
                import(`@mui-icons/${iconName}.js`).then((mod) => ({
                  default: mod.default,
                })),
              );
              //const iconComponent = (await iconMode._payload._result()).default;
              iconCache.current[iconName] = iconMode;
              loadedIcons[iconName] = iconMode;
            } catch (err) {
              console.error(
                `Failed to load icon ${iconName} in main thread`,
                err,
              );
            }
          }),
        );

        setIcons(loadedIcons);
      }
    };

    return () => {
      worker.terminate();
      workerRef.current = null;
    };
  }, []);

  const loadPage = useCallback(
    (page: number, pageSize: number, search: string = "") => {
      workerRef.current?.postMessage({
        type: "loadIconsPage",
        page,
        pageSize,
        search,
      });
    },
    [],
  );

  const loadSingleIcon = useCallback(
    (iconName: string): React.ComponentType => {
      return React.lazy(() =>
        import(`@mui-icons/${iconName}.js`).then((mod) => ({
          default: mod.default,
        })),
      );
    },
    [],
  );

  return { icons, loadPage, totalPages, loadSingleIcon };
}