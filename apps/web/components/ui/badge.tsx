import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap",
  {
    variants: {
      variant: {
        default: "bg-secondary text-secondary-foreground",
        primary: "bg-primary-subtle text-primary-subtle-foreground",
        success: "bg-success-subtle text-success-foreground",
        warning: "bg-warning-subtle text-warning-foreground",
        destructive:
          "bg-destructive-subtle text-destructive-subtle-foreground",
        outline: "border border-border text-muted-foreground",
      },
    },
    defaultVariants: { variant: "default" },
  }
);

function Badge({
  className,
  variant,
  dot,
  ...props
}: React.ComponentProps<"span"> &
  VariantProps<typeof badgeVariants> & { dot?: boolean }) {
  return (
    <span className={cn(badgeVariants({ variant }), className)} {...props}>
      {dot && (
        <span className="size-1.5 rounded-full bg-current opacity-80" />
      )}
      {props.children}
    </span>
  );
}

export { Badge, badgeVariants };
