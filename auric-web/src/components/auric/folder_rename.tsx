import { useEffect, useRef, useState } from "react";

export function FolderRename({
  id,
  value,
  style,
  onSave,
  onCancel,
}: {
  id: string;
  value: string;
  style: { left: number; top: number; width: number; height: number };
  onSave: (value: string) => boolean;
  onCancel: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const cancelled = useRef(false);
  const submitted = useRef(false);
  const [draft, setDraft] = useState(value);
  const [error, setError] = useState(false);

  useEffect(() => {
    inputRef.current?.focus();
    inputRef.current?.select();
  }, []);

  const save = () => {
    if (cancelled.current || submitted.current) return;
    if (!draft.trim() || !onSave(draft.trim().slice(0, 24))) {
      setError(true);
      window.setTimeout(() => inputRef.current?.focus(), 0);
      return;
    }
    setError(false);
    submitted.current = true;
  };

  return (
    <div
      className="tx-folder-rename tx-folder-control"
      data-invalid={error}
      style={style}
      onPointerDown={(event) => event.stopPropagation()}
    >
      <input
        ref={inputRef}
        value={draft}
        maxLength={24}
        aria-label={`Rename ${id} folder`}
        aria-describedby={error ? `folder-rename-error-${id}` : undefined}
        aria-invalid={error}
        onChange={(event) => {
          setDraft(event.currentTarget.value);
          setError(false);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            save();
          } else if (event.key === "Escape") {
            event.preventDefault();
            cancelled.current = true;
            onCancel();
          }
        }}
        onBlur={() => {
          if (!cancelled.current && !submitted.current) save();
        }}
      />
      {error ? <span id={`folder-rename-error-${id}`} role="alert">Name can't be empty</span> : null}
    </div>
  );
}
