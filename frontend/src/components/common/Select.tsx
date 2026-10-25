import React from "react";
import * as RadixSelect from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}
export const Select: React.FC<{
  value?: string;
  onValueChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  ariaLabel?: string;
  disabled?: boolean;
}> = ({
  value,
  onValueChange,
  options,
  placeholder = "Select an option",
  ariaLabel,
  disabled,
}) => (
  <RadixSelect.Root
    value={value || undefined}
    onValueChange={onValueChange}
    disabled={disabled}
  >
    <RadixSelect.Trigger
      aria-label={ariaLabel || placeholder}
      className="form-input"
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        width: "100%",
        background: "#fff",
      }}
    >
      <RadixSelect.Value placeholder={placeholder} />
      <RadixSelect.Icon>
        <ChevronDown size={17} />
      </RadixSelect.Icon>
    </RadixSelect.Trigger>
    <RadixSelect.Portal>
      <RadixSelect.Content
        position="popper"
        sideOffset={5}
        style={{
          zIndex: 2000,
          background: "#fff",
          border: "1px solid var(--border)",
          borderRadius: 12,
          boxShadow: "var(--shadow-lg)",
          overflow: "hidden",
          minWidth: "var(--radix-select-trigger-width)",
        }}
      >
        <RadixSelect.Viewport>
          {options.map((option) => (
            <RadixSelect.Item
              key={option.value}
              value={option.value}
              disabled={option.disabled}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: 16,
                padding: ".65rem .85rem",
                cursor: "pointer",
                outline: "none",
              }}
            >
              <RadixSelect.ItemText>{option.label}</RadixSelect.ItemText>
              <RadixSelect.ItemIndicator>
                <Check size={16} />
              </RadixSelect.ItemIndicator>
            </RadixSelect.Item>
          ))}
        </RadixSelect.Viewport>
      </RadixSelect.Content>
    </RadixSelect.Portal>
  </RadixSelect.Root>
);
