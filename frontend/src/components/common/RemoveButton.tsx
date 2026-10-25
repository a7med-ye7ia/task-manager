import React, { useState } from "react";
import { Button } from "./Button";
export const RemoveButton: React.FC<React.ComponentProps<typeof Button>> = ({
  variant = "outline",
  ...rest
}) => {
  const [active, setActive] = useState(false);
  return (
    <span
      style={{ display: "inline-flex" }}
      onMouseEnter={() => setActive(true)}
      onMouseLeave={() => setActive(false)}
      onFocus={() => setActive(true)}
      onBlur={() => setActive(false)}
    >
      <Button {...rest} variant={active ? "danger" : variant} />
    </span>
  );
};