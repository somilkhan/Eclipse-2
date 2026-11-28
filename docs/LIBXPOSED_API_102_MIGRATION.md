# libxposed API 102 migration

Status: in progress. Entry point, packaging foundation, adapter, and compile gate are complete; APK/runtime gates remain pending. Do not merge or publish until every release gate below passes.

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
- [x] Add the API 102 compile-only dependency (`io.github.libxposed:api:102.0.0`).
- [x] Add exactly one Java entry in `META-INF/xposed/java_init.list`.
- [x] Add `module.prop` with min API 101 and target API 102.
- [x] Add a scope list covering every supported Instagram variant.
- [x] Remove legacy `assets/xposed_init` and legacy Xposed manifest metadata after wiring the new entry.
- [x] Replace zygote initialization with `XposedModule.onModuleLoaded`/`onPackageReady`; initialize DexKit/native libraries from the target process and preserve classloader lifecycle.
- [x] Preserve package filtering, clone package scope, and helper-process filtering using the API 102 process callback.

### 2. Hook engine
- [x] Introduce a project-owned callback adapter backed by `XposedInterface.hook(Executable)`.
- [x] Port before/after/replacement semantics to the API 102 interceptor chain.
- [x] Port method/constructor lookup helpers and callback parameter access into the project-owned adapter.
- [x] Preserve callback priorities, API 102 unhook handles, original invocation, exception propagation, and existing install isolation.
- [ ] Port all feature hooks; no runtime references to `de.robv.android.xposed` may remain in the APK.
- [ ] Replace deoptimization calls with supported modern API behaviour where possible; otherwise document feature-specific limitations.

### 3. Companion communication and process context
- [x] Replace `XSharedPreferences` with API-102 remote preferences, gated on `PROP_CAP_REMOTE`; unsupported frameworks use the safe empty/read-only path.
- [ ] Design a safe compatibility path for frameworks without remote-preference capability; do not assume companion app private files are readable by Instagram.
- [ ] Preserve timestamp conflict resolution, cold-start sync, resume sync, and live broadcast updates.
- [x] Replace `AndroidAppHelper` access with a lifecycle-populated application context adapter.
- [ ] Verify download, log, crash-report and settings IPC providers under the new process model.

### 4. Verification
- [x] Compile Java/resources against API 102 without `api-82.jar` on the app compile/runtime classpath; full APK gate remains pending.
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

## Verified work notes

- Starting branch HEAD: `91069b363305a491617d0a1e8a9df2dd7e321b6f` (PR #24, draft).
- Baseline command: `./gradlew clean test assembleDebug`; blocked before compilation because the original sandbox had no Android SDK configured.
- API contract sources verified against the official API 102 Javadoc/repository and `api-102.0.0.aar`.
- Foundation compile command: `ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew :app:compileDebugJavaWithJavac :app:processDebugResources`; result: PASS.
- Clean verification command: `./gradlew clean test assembleDebug`; result: PASS (debug and release unit-test tasks passed, debug APK packaged).
- APK scan: API 102 metadata present; no `de/robv/android/xposed`, `api-82`, or `xposed_init` strings found in `classes.dex`.
- Dependency scan: `io.github.libxposed:api:102.0.0` present; no API-82 dependency.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `a1fd0f91ac8aeea1c917512cc320ea893e366e9380449abec1dd6c0aae83bb37`.
- Runtime verification has not been performed; an actual API-102-compatible framework/device is still required.
