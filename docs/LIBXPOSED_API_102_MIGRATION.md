# libxposed API 102 migration

Status: in progress. This document is the migration checklist for PR #24. Do not merge or publish until every release gate below passes.

## Target

Move Eclipse 2 from the legacy Xposed API 82 entry point to the modern libxposed API 102 entry point while preserving the existing feature behaviour and companion-app contract.

Official API contract:
- Compile against `io.github.libxposed:api:102.0.0`.
- Entry class extends `io.github.libxposed.api.XposedModule`.
- Register the entry in `META-INF/xposed/java_init.list`.
- Declare `minApiVersion=101` and `targetApiVersion=102` in `META-INF/xposed/module.prop`.
- Declare supported packages in `META-INF/xposed/scope.list`.
- API 102 modules must not call `de.robv.android.xposed` APIs.
- API 101+ no longer injects modules into zygote. Module initialization must move from `initZygote` / `handleLoadPackage` to the process lifecycle callbacks.

References:
- https://github.com/libxposed/api
- https://libxposed.github.io/api/io/github/libxposed/api/package-summary.html
- https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API

## Repository audit (baseline on this PR)

- 280 Java files under `app/src/main/java`.
- The current entry point is `Xposed/Module.java`, implementing `IXposedHookLoadPackage` and `IXposedHookZygoteInit`.
- Legacy hook API use is spread through feature code: `XposedBridge`, `XposedHelpers`, `XC_MethodHook`, `XC_MethodReplacement`, and legacy callback types.
- `XSharedPreferences` is used for companion-to-Instagram cold-start/resume preference sync in `Module.java`, `CompanionSync.java`, and media code; companion-side cache writes and explicit permission changes also rely on the legacy cross-process file model.
- `AndroidAppHelper` is used by hook and utility code.
- `tools/hook-verify` contains legacy API stand-ins. Those are host-test infrastructure, not a reason to keep legacy API calls in the shipped APK; the harness must be adapted to the new hook adapter or replaced with API-102-aware tests.
- The current APK uses `assets/xposed_init`, legacy manifest metadata (`xposedmodule`, `xposedminversion`, `xposedscope`) and `libs/api-82.jar`.

## Migration workstreams

### 1. Entry point and module metadata
- [ ] Add the API 102 compile-only dependency.
- [ ] Add exactly one Java entry in `META-INF/xposed/java_init.list`.
- [ ] Add `module.prop` with min API 101 and target API 102.
- [ ] Add a scope list covering every supported Instagram variant.
- [ ] Remove legacy `assets/xposed_init` and legacy Xposed manifest metadata only after the new entry is wired.
- [ ] Replace zygote initialization with process lifecycle callbacks; initialize DexKit/native libraries from the target process and correct classloader lifecycle.
- [ ] Ensure process/package filtering is equivalent to current behaviour, including clone package support.

### 2. Hook engine
- [ ] Introduce a project-owned hook abstraction backed by `XposedInterface.hook(Executable)`.
- [ ] Port before/after/replacement semantics to `Hooker`/chain semantics.
- [ ] Port method/constructor lookup helpers and callback parameter access.
- [ ] Preserve priorities, unhook handles, original invocation, exception behaviour, and install isolation.
- [ ] Port all feature hooks; no runtime references to `de.robv.android.xposed` may remain in the APK.
- [ ] Replace deoptimization calls with supported modern API behaviour where possible; otherwise document feature-specific limitations.

### 3. Companion communication and process context
- [ ] Replace `XSharedPreferences` with API-102 remote preferences where the framework advertises `PROP_CAP_REMOTE`.
- [ ] Design a safe compatibility path for frameworks without remote-preference capability; do not assume companion app private files are readable by Instagram.
- [ ] Preserve timestamp conflict resolution, cold-start sync, resume sync, and live broadcast updates.
- [ ] Replace `AndroidAppHelper` access with lifecycle-provided process/application context.
- [ ] Verify download, log, crash-report and settings IPC providers under the new process model.

### 4. Verification
- [ ] Compile debug and release variants against API 102 without `api-82.jar` on the app compile/runtime classpath.
- [ ] Scan the final DEX/APK for forbidden legacy API references and legacy entry metadata.
- [ ] Run unit tests and adapt `tools/hook-verify` so it tests the new hook abstraction rather than the old XposedBridge shim.
- [ ] Run offline hook-target verification against the currently supported Instagram APKs.
- [ ] Verify all feature install paths, per-feature failure isolation, and startup ordering.
- [ ] Verify cold start, activity resume, settings round-trip, downloads, logging, crash reports and clone packages on-device.
- [ ] Verify module appears/enables correctly in a compatible API-102 framework.
- [ ] Produce a test APK artifact only after CI passes.

## Release gates

Do not merge this PR while any of the following is true:
1. Any shipped app source or DEX calls a legacy `de.robv.android.xposed` API.
2. The module still depends on `assets/xposed_init` or legacy module metadata.
3. Any supported feature silently disappears because its hook was not ported.
4. Companion settings/download/log/crash IPC regressions remain.
5. CI build, unit tests, hook verification or APK scan fails.

The first user device test should happen only after a clean CI build produces a test APK. Keep the current stable API-82 release untouched during this migration.
