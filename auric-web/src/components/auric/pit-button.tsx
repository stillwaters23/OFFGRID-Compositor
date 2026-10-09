import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type Variant = "gold" | "ghost" | "chrome" | "danger";

export function PitButton({
  variant = "gold",
  className,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      type="button"
      className={cn(
        "inline-flex min-h-11 items-center justify-center gap-2 rounded-md px-4 text-sm font-medium tracking-wide",
        "transition-[transform,background-color,box-shadow,opacity] duration-150 ease-out",
        "active:not-disabled:scale-[0.96] disabled:opacity-40",
        variant === "gold" &&
          "bg-accent text-on-accent shadow-[0_0_0_1px_color-mix(in_oklab,var(--digit)_12%,transparent)] hover:brightness-110",
        variant === "ghost" &&
          "bg-transparent text-digit shadow-[0_0_0_1px_var(--line)] hover:bg-chrome/60",
        variant === "chrome" && "panel text-digit hover:border-accent/60",
        variant === "danger" && "bg-danger text-on-danger hover:brightness-110",
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
