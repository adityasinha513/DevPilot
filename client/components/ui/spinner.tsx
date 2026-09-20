import * as React from "react";

import { cn } from "@/lib/utils";

function Spinner({ className, ...props }: React.ComponentProps<"svg">) {
  return (
    <svg
      data-slot="spinner"
      role="status"
      aria-label="Loading"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      className={cn("size-4 animate-spin", className)}
      {...props}
    >
      <path d="M12 2.75v2.5" strokeLinecap="round" />
      <path d="M12 18.75v2.5" strokeLinecap="round" />
      <path d="M4.93 4.93l1.77 1.77" strokeLinecap="round" />
      <path d="M17.3 17.3l1.77 1.77" strokeLinecap="round" />
      <path d="M2.75 12h2.5" strokeLinecap="round" />
      <path d="M18.75 12h2.5" strokeLinecap="round" />
      <path d="M4.93 19.07l1.77-1.77" strokeLinecap="round" />
      <path d="M17.3 6.7l1.77-1.77" strokeLinecap="round" />
    </svg>
  );
}

export { Spinner };
