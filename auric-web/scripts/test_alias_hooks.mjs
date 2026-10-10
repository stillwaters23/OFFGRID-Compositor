import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";

export async function resolve(specifier, context, nextResolve) {
  if (specifier.startsWith("@/")) {
    const base = new URL(`../src/${specifier.slice(2)}`, import.meta.url);
    for (const extension of [".ts", ".tsx", ".js"]) {
      const url = new URL(`${base.href}${extension}`);
      if (existsSync(fileURLToPath(url))) return nextResolve(url.href, context);
    }
  }
  return nextResolve(specifier, context);
}
