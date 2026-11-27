# hook-verify — offline check of every hook against an Instagram APK

Runs InstaEclipse's **real** hook-install code (the same `install(...)` calls `Xposed/Module.java`
makes on the phone) on a Linux host, against a given Instagram APK, and reports for every feature
which DexKit lookups resolved and which methods would be hooked. No phone, emulator or root needed.

```
tools/hook-verify/run.sh                         # all steps, IG APK from $IG_APK or ~/ig/ig457.apk
tools/hook-verify/run.sh --filter 'metaAi|Reel.*' # only some steps (regex on the skip() key)
tools/hook-verify/run.sh --echo                   # print every hook/log line live
tools/hook-verify/run.sh --skip-gradle            # reuse the last compiled module classes
HOOKVERIFY_LOCK=/tmp/ie-gradle.lock tools/hook-verify/run.sh   # shared box: serialise heavy jobs
```

Output: a summary table on stdout and `tools/hook-verify/build/report.md` with, per step, the
hooked members and the module's own log lines (failure lines are prefixed `!!`).

## How it works

| Piece | What it does |
|---|---|
| `libdexkit-<ver>.so` | DexKit's C++ core + JNI bridge built for the host from the DexKit tag matching `gradle/libs.versions.toml` (needs `cmake`, `g++`, zlib headers). So lookups run through the exact same engine as on the phone. |
| `classpath.init.gradle` | Gradle init script adding `:app:hookVerifyClasspath`: compiles the debug sources, writes the module's runtime classpath. Nothing in the app's build changes. |
| `IgClassLoader` | Stand-in for Instagram's class loader: every class in the APK's dex files is synthesised (ASM) with the same name, superclass, interfaces, fields and method signatures; bodies throw. Reflection and DexKit's `getMethodInstance` therefore behave as on the device. Framework classes come from the parent first, like ART's boot classpath. |
| `MockableJar` | android.jar with every `throw new RuntimeException("Stub!")` body replaced by a default return (AGP's `returnDefaultValues` idea), so module code that touches Android APIs during install doesn't blow up. |
| `src/xposed` | Host stand-ins for the Xposed API: `XposedBridge.hookMethod` records instead of patching; `XposedHelpers` is the pure-reflection subset. |
| `src/shadow/android/util/Log` | Routes `ModuleLog` lines into the report. |
| `Runner` | Parses every `if (!skip("Key")) <call>;` statement in `Module.java` and runs it with the real DexKit bridge and the synthesised class loader. New hooks added to Module.java are picked up automatically. All boolean `FeatureFlags` are forced on so every install path runs. |

## Status meaning

- **OK** — hooks recorded, no failure lines in the step's log.
- **PARTIAL** — some hooks recorded, but at least one sub-lookup logged a failure (`❌`, `⚠️`,
  "not found", ...). Read the `!!` lines.
- **FAIL** — failure logged and nothing hooked.
- **NO-HOOK** — nothing hooked, nothing logged. Usually a hook that installs lazily (e.g. after an
  Activity exists) or a feature that only hooks framework classes at runtime; check by hand.
- **ERROR** — the install call threw.

## Limits

- Instagram code never runs: anything a hook does *at runtime* (field reads on live objects,
  view-tree walks, network) is not verified, only that the targets resolve and the hooks attach.
- Classification is by log markers, so a hook that silently swallows a failed lookup shows as
  NO-HOOK/OK. Prefer logging `❌`/`⚠️` on every failed lookup.
- `getDeclaredMethods` on a synthesised class can fail if a signature references a type that is
  in neither the APK nor android.jar (hidden framework API); the hook then sees the same
  `NoClassDefFoundError` it would get from a broken lookup.

## Requirements

JDK 17, Android SDK platform jar (`$ANDROID_HOME/platforms/android-*/android.jar`), `cmake`
(`pip install cmake ninja`), `g++`, zlib headers (`zlib1g-dev`), `git`, `curl`. dexlib2 is taken
from `~/baksmali.jar` / `$BAKSMALI_JAR` or downloaded from Maven Central with ASM.

Xposed stand-ins `XC_MethodHook.java` / `XC_MethodReplacement.java` are copied from
rovo89/XposedBridge (Apache-2.0).
