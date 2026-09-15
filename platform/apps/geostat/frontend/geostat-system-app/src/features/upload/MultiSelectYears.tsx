import React from "react";
import { Select, MenuItem, FormControl, InputLabel } from "@mui/material";

const YEAR_CHOICES = ["2020", "2021", "2022", "2023", "2024", "2025", "2026"];

interface MultiSelectYearsProps {
  onChange: (years: string[]) => void;
  value: string[];
}

export const MultiSelectYears: React.FC<MultiSelectYearsProps> = ({
  onChange,
  value,
}) => {
  return (
    <FormControl fullWidth required>
      <InputLabel>Years</InputLabel>
      <Select
        multiple
        value={value}
        onChange={(e) => onChange(e.target.value as string[])}
        renderValue={(selected) => (selected as string[]).join(", ")}
      >
        {YEAR_CHOICES.map((year) => (
          <MenuItem key={year} value={year}>
            {year}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};