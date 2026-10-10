export function FolderSizePopover({
  id,
  value,
  onChange,
  onClose,
}: {
  id: string;
  value: number;
  onChange: (value: number) => void;
  onClose: () => void;
}) {
  return (
    <div className="tx-folder-size-popover tx-folder-control" role="group" aria-label={`Size ${id} folder`}>
      <label>
        <span>FOLDER SIZE</span>
        <output>{value}</output>
        <input
          type="range"
          min="0"
          max="100"
          step="1"
          value={value}
          aria-label={`Size ${id} folder`}
          onChange={(event) => onChange(Number(event.currentTarget.value))}
        />
      </label>
      <button type="button" className="tx-folder-control-close" onClick={onClose} aria-label="Close folder size">
        ×
      </button>
    </div>
  );
}
