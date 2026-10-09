import { useEffect, useSyncExternalStore } from "react";
import { faceById, faceLoadState, loadFace, subscribeFaces } from "@/lib/faces";

export function useFace(id: string) {
  const face = faceById(id);
  const status = useSyncExternalStore(subscribeFaces, () => faceLoadState(face.id), () => "idle" as const);
  useEffect(() => {
    void loadFace(face.id);
    const retry = () => { void loadFace(face.id); };
    window.addEventListener("online", retry);
    return () => window.removeEventListener("online", retry);
  }, [face.id]);
  return { face, status, retry: () => loadFace(face.id) };
}
