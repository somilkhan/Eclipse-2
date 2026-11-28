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
| `Runner` | Parses every `if (!skip("Key")) <call>;` and `isolatedInstall("Key", () -> <call>);` statement in `Module.java` and runs it with the real DexKit bridge and the synthesised class loader. New hooks added to Module.java are picked up automatically. A line that gates an install on a skip key but matches neither form is reported as an `UNPARSED@<line>` ERROR step instead of being dropped (`ModuleInstallIsolationTest` checks the same thing in CI). All boolean `FeatureFlags` are forced on so every install path runs. |

## Status meaning

- **OK** — hooks recorded, no failure lines in the step's log.
- **PARTIAL** — some hooks recorded, but at least one sub-lookup logged a failure (`❌`, `⚠️`,
  "not found", ...). Read the `!!` lines.
- **FAIL** — failure logged and nothing hooked.
- **NO-HOOK** — nothing hooked, nothing logged. Usually a hook that installs lazily (e.g. after an
  Activity exists) or a feature that only hooks framework classes at runtime; check by hand.
- **ERROR** — the install call threw.
- A `(harness)` suffix means every failure line of the step matches a known harness gap
  (`hook-verify stub`, `CREATOR is null`, `Stub!`): the lookups resolved, but the module then tried
  to *run* Instagram or framework code, which the host cannot do. Check those on a device.

The report header names the exact Instagram build (`versionName (versionCode)`, read with `aapt`).
**Results hold for that build only.** IG obfuscates per build, so 457.0.0.55.73 and the beta
457.0.0.6.73 have different class/method names and string-table placement; an OK on one build says
nothing about another. Check the device's versionName against the header before trusting a report,
and run with `IG_APK=/path/to/other.apk` (or `--apk`) to verify a second build.

## Fidelity details

- Access flags are copied from dex, including dex-only `declared-synchronized` (0x20000), mapped to
  `ACC_SYNCHRONIZED` so `Modifier.isSynchronized` matches ART.
- Enums get real constants: `EnumSupport` decodes `<clinit>` (direct `new`+`<init>`, same-class static
  factories, register moves), maps name/ordinal from each ctor's `Enum.<init>` super call, and fills
  the static fields, `values()` and `valueOf()`. IG enums often override `toString()` with a server
  value (e.g. `"normal"`), so module code should match constants with `Enum.name()`. A decoding
  failure only logs to stderr and falls back to a plain synthesised class.

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
