import type { ReactNode } from "react";

/** Terminal shell has no 777 splash. APK session is the first frame. */
export function BootRitual({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
