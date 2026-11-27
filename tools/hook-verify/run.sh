#!/usr/bin/env bash
# hook-verify: run InstaEclipse's real hook-install code against an Instagram APK on a Linux host.
# See README.md. Usage: tools/hook-verify/run.sh [--filter REGEX] [--echo] [--apk PATH] [--skip-gradle]
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
BUILD="$HERE/build"
mkdir -p "$BUILD/libs" "$BUILD/classes"

APK="${IG_APK:-$HOME/ig/ig457.apk}"
FILTER=""
ECHO=false
SKIP_GRADLE=false
while [ $# -gt 0 ]; do
  case "$1" in
    --filter) FILTER="$2"; shift 2 ;;
    --echo) ECHO=true; shift ;;
    --apk) APK="$2"; shift 2 ;;
    --skip-gradle) SKIP_GRADLE=true; shift ;;
    *) echo "unknown arg $1" >&2; exit 2 ;;
  esac
done
[ -f "$APK" ] || { echo "Instagram APK not found: $APK (set IG_APK or --apk)" >&2; exit 2; }

# Optional lock for shared machines: HOOKVERIFY_LOCK=/tmp/ie-gradle.lock serialises heavy JVM/C++ jobs.
heavy() { if [ -n "${HOOKVERIFY_LOCK:-}" ]; then flock "$HOOKVERIFY_LOCK" "$@"; else "$@"; fi; }

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
ANDROID_JAR="${ANDROID_JAR:-$(ls -d "$ANDROID_HOME"/platforms/android-*/android.jar | sort -V | tail -1)}"
DEXKIT_VERSION="$(sed -n 's/^dexkit *= *"\(.*\)"/\1/p' "$REPO/gradle/libs.versions.toml")"

fetch() { # group artifact version -> jar path (Maven Central, cached)
  local g="$1" a="$2" v="$3" f="$BUILD/libs/$2-$3.jar"
  [ -f "$f" ] || curl -sfL -o "$f" "https://repo1.maven.org/maven2/${g//.//}/$a/$v/$a-$v.jar"
  echo "$f"
}
ASM="$(fetch org.ow2.asm asm 9.6):$(fetch org.ow2.asm asm-tree 9.6)"
if [ -n "${BAKSMALI_JAR:-}" ] && [ -f "$BAKSMALI_JAR" ]; then DEXLIB="$BAKSMALI_JAR"
elif [ -f "$HOME/baksmali.jar" ]; then DEXLIB="$HOME/baksmali.jar"
else DEXLIB="$(fetch org.smali dexlib2 2.5.2):$(fetch com.google.guava guava 30.1.1-jre)"; fi

# 1) Host build of DexKit's JNI library, same version as the module uses.
LIB="$BUILD/libdexkit-$DEXKIT_VERSION.so"
if [ ! -f "$LIB" ]; then
  command -v cmake >/dev/null || { echo "cmake is required (pip install cmake ninja)" >&2; exit 2; }
  SRC="$BUILD/dexkit-src-$DEXKIT_VERSION"
  [ -d "$SRC" ] || git clone -q --depth 1 --branch "$DEXKIT_VERSION" --recurse-submodules https://github.com/LuckyPray/DexKit.git "$SRC"
  JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
  GEN=(); command -v ninja >/dev/null && GEN=(-G Ninja)
  JAVA_HOME="$JAVA_HOME" cmake "${GEN[@]}" -S "$SRC/dexkit/src/main/cpp" -B "$BUILD/dexkit-cmake" \
      -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_CXX_FLAGS_RELEASE=-O2 -DCMAKE_C_FLAGS_RELEASE=-O2 >/dev/null
  heavy cmake --build "$BUILD/dexkit-cmake" -j "${HOOKVERIFY_JOBS:-2}"
  cp "$(find "$BUILD/dexkit-cmake" -name 'libdexkit.so' | head -1)" "$LIB"
fi

# 2) Module classes + runtime classpath (debug variant).
CPFILE="$REPO/app/build/hook-verify/classpath.txt"
if [ "$SKIP_GRADLE" != true ] || [ ! -f "$CPFILE" ]; then
  if command -v ie-gradle >/dev/null; then (cd "$REPO" && ie-gradle -q -I "$HERE/classpath.init.gradle" :app:hookVerifyClasspath)
  else (cd "$REPO" && ./gradlew -q -I "$HERE/classpath.init.gradle" :app:hookVerifyClasspath); fi
fi

# 3) Mockable android.jar + harness classes.
MOCK="$BUILD/mockable-$(basename "$(dirname "$ANDROID_JAR")").jar"
FW="$BUILD/classes/framework"; RUN="$BUILD/classes/runner"
if [ ! -f "$MOCK" ] || [ -n "$(find "$HERE/src" -newer "$MOCK" -name '*.java' | head -1)" ]; then
  rm -rf "$FW" "$RUN"; mkdir -p "$FW" "$RUN"
  javac -nowarn -encoding UTF-8 -d "$FW" -cp "$ANDROID_JAR:$ASM:$DEXLIB" \
      $(find "$HERE/src/xposed" "$HERE/src/shadow" "$HERE/src/harness" -name '*.java')
  java -cp "$FW:$ASM" hookverify.MockableJar "$ANDROID_JAR" "$MOCK"
  javac -nowarn -encoding UTF-8 -d "$RUN" -cp "$FW:$ANDROID_JAR" $(find "$HERE/src/runner" -name '*.java')
fi

MODULE_CP="$RUN:$(paste -sd: "$CPFILE")"
REPORT="$BUILD/report.md"
# versionName/versionCode of the checked APK go into the report header: an OK here only speaks for
# that exact build (457.0.0.55.73 and the 457.0.0.6.73 beta obfuscate differently).
AAPT="$(ls -d "${ANDROID_HOME:-$HOME/sdk}"/build-tools/*/aapt 2>/dev/null | tail -1)"
APKVER=""
[ -n "$AAPT" ] && APKVER="$("$AAPT" dump badging "$APK" 2>/dev/null | sed -n "s/^package: .*versionCode='\([0-9]*\)' versionName='\([^']*\)'.*/\2 (\1)/p" | head -1)"
heavy java -Xmx"${HOOKVERIFY_XMX:-3g}" -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote -XX:-BytecodeVerificationLocal \
  -cp "$FW:$MOCK:$ASM:$DEXLIB" \
  -Dhookverify.apk="$APK" -Dhookverify.dexkit="$LIB" -Dhookverify.moduleCp="$MODULE_CP" \
  -Dhookverify.moduleJava="$REPO/app/src/main/java/io/github/somilkhan/instaeclipse2/Xposed/Module.java" \
  -Dhookverify.report="$REPORT" -Dhookverify.apkVersion="$APKVER" -Dhookverify.filter="$FILTER" -Dhookverify.echo="$ECHO" \
  hookverify.Main
