import { useEffect } from "react";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

export function Toasts() {
  const toasts = useAuric((s) => s.toasts);

  return (
    <div className="pointer-events-none fixed inset-x-0 top-8 z-[65] mx-auto flex w-full max-w-sm flex-col gap-2 px-3">
      {toasts.map((t) => (
        <ToastItem key={t.id} id={t.id} title={t.title} body={t.body} tone={t.tone} />
      ))}
    </div>
  );
}

function ToastItem({
  id,
  title,
  body,
  tone,
}: {
  id: string;
  title: string;
  body?: string;
  tone?: "gold" | "danger" | "mute";
}) {
  useEffect(() => {
    const t = window.setTimeout(() => useAuric.getState().dismissToast(id), 4200);
    return () => window.clearTimeout(t);
  }, [id]);

  return (
    <div
      className={cn(
        "pointer-events-auto rise rounded-lg panel px-3 py-2",
        tone === "danger" && "shadow-[0_0_0_1px_var(--danger)]",
      )}
    >
      <p className="font-display text-sm text-accent">{title}</p>
      {body ? <p className="text-xs text-muted">{body}</p> : null}
    </div>
  );
}
