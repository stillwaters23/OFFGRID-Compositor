import * as nodeModule from "node:module";
import { resolve } from "./test_alias_hooks.mjs";

// registerHooks is synchronous and arrived after Node 22.14. The loader
// register() path is the one this runtime and current CI both accept.
// Lane 4 awaits register() so the hook is installed before tests import.
if (typeof nodeModule.registerHooks === "function") {
  nodeModule.registerHooks({ resolve });
} else {
  await nodeModule.register("./test_alias_hooks.mjs", import.meta.url);
}
