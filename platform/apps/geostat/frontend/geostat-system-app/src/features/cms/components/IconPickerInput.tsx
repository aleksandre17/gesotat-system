import { useInput, InputHelperText } from "react-admin";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  Grid,
  Box,
  Typography,
  FormControl,
  FormHelperText,
  InputLabel,
  Pagination,
} from "@mui/material";
import * as Icons from "@mui/icons-material";
import type { SvgIconProps } from "@mui/material/SvgIcon";
import React, { useState, useMemo, useRef, useCallback } from "react";

interface IconPickerInputProps {
  source: string;
  label?: string;
  helperText?: string;
}

type IconComponent = React.ComponentType<SvgIconProps>;

const PAGE_SIZE = 50;

const debounce = (fn: (value: string) => void, delay: number) => {
  let timer: ReturnType<typeof setTimeout>;
  return (value: string) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(value), delay);
  };
};

// Resolve a MUI icon by name, handling both function and object exports
const resolveIcon = (name: string): IconComponent => {
  const Icon = (Icons as Record<string, unknown>)[name];
  if (typeof Icon === "function") return Icon as IconComponent;
  if (Icon && typeof Icon === "object") {
    const obj = Icon as Record<string, unknown>;
    if (typeof obj.type === "function") return obj.type as IconComponent;
    if (
      obj.type &&
      typeof (obj.type as Record<string, unknown>).render === "function"
    )
      return (obj.type as Record<string, unknown>).render as IconComponent;
  }
  return () => null;
};

// Pre-filtered icon name list (filled variants only, no utilities)
const ALL_ICON_NAMES = Object.keys(Icons).filter(
  (key) =>
    key !== "createSvgIcon" && !key.match(/(Outlined|TwoTone|Rounded|Sharp)$/),
);

const IconPickerInput: React.FC<IconPickerInputProps> = ({
  source,
  label = "Icon",
  helperText,
}) => {
  const {
    field,
    fieldState: { error, isTouched },
    formState: { isSubmitted },
  } = useInput({ source });

  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);

  const iconCache = useRef(new Map<string, IconComponent>()).current;

  const getIcon = useCallback(
    (name: string): IconComponent => {
      if (!iconCache.has(name)) iconCache.set(name, resolveIcon(name));
      return iconCache.get(name)!;
    },
    [iconCache],
  );

  const filteredNames = useMemo(() => {
    const q = search.toLowerCase();
    return q
      ? ALL_ICON_NAMES.filter((n) => n.toLowerCase().includes(q))
      : ALL_ICON_NAMES;
  }, [search]);

  const pagedIcons = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredNames
      .slice(start, start + PAGE_SIZE)
      .map((name) => ({ name, component: getIcon(name) }));
  }, [filteredNames, page, getIcon]);

  const totalPages = Math.ceil(filteredNames.length / PAGE_SIZE);

  const debouncedSearch = useMemo(
    () =>
      debounce((value: string) => {
        setSearch(value);
        setPage(1);
      }, 200),
    [],
  );

  const handleSelect = (iconName: string | null) => {
    field.onChange(iconName);
    setOpen(false);
    setSearch("");
    setPage(1);
  };

  const SelectedIcon = field.value ? getIcon(field.value) : null;

  return (
    <FormControl fullWidth error={isTouched && !!error}>
      <InputLabel shrink id={`${source}-label`}>
        {label}
      </InputLabel>
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          border: "1px solid",
          borderColor: isTouched && error ? "error.main" : "grey.300",
          borderRadius: 1,
          p: 1,
          minHeight: 56,
          cursor: "pointer",
          bgcolor: "background.paper",
          "&:hover": { borderColor: "primary.main" },
        }}
        onClick={() => setOpen(true)}
      >
        {SelectedIcon && <SelectedIcon fontSize="small" color="primary" />}
        <Typography
          variant="body1"
          color={field.value ? "text.primary" : "text.secondary"}
        >
          {field.value || "Select an icon"}
        </Typography>
      </Box>
      <FormHelperText>
        <InputHelperText
          touched={isTouched || isSubmitted}
          error={error?.message}
          helperText={helperText}
        />
      </FormHelperText>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Select Icon</DialogTitle>
        <DialogContent>
          <TextField
            label="Search Icons"
            onChange={(e) => debouncedSearch(e.target.value)}
            fullWidth
            variant="outlined"
            sx={{ mb: 2 }}
            autoFocus
          />
          <Grid container spacing={2} sx={{ maxHeight: 400, overflow: "auto" }}>
            <Grid item xs={12}>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  p: 1,
                  cursor: "pointer",
                  bgcolor:
                    field.value === null ? "primary.main" : "transparent",
                  color: field.value === null ? "white" : "inherit",
                  borderRadius: 1,
                  "&:hover": { bgcolor: "primary.light", color: "white" },
                }}
                onClick={() => handleSelect(null)}
              >
                <Typography variant="body2">None</Typography>
              </Box>
            </Grid>
            {pagedIcons.map(({ name, component: IconComponent }) => (
              <Grid item xs={4} sm={3} md={2} key={name}>
                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: 1,
                    p: 1,
                    cursor: "pointer",
                    bgcolor:
                      field.value === name ? "primary.main" : "transparent",
                    color: field.value === name ? "white" : "inherit",
                    borderRadius: 1,
                    "&:hover": { bgcolor: "primary.light", color: "white" },
                  }}
                  onClick={() => handleSelect(name)}
                >
                  <IconComponent fontSize="medium" />
                  <Typography variant="caption" textAlign="center">
                    {name}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
          {totalPages > 1 && (
            <Pagination
              count={totalPages}
              page={page}
              onChange={(_, value) => setPage(value)}
              sx={{ mt: 2, display: "flex", justifyContent: "center" }}
            />
          )}
        </DialogContent>
      </Dialog>
    </FormControl>
  );
};

export default IconPickerInput;