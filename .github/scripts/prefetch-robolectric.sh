#!/usr/bin/env bash
# Puts the Robolectric android-all jars in ~/.m2 before the unit tests run.
#
# Robolectric downloads these (150-200 MB each) from Maven Central on first use, inside the test
# JVM, with no retry. One failed request fails every Robolectric test in the run, which is how
# CI went red with "IOException at HttpURLConnection" on a commit that passed locally. Here the
# download retries, is checked against Maven's sha512, and is cached between runs by the workflow.
#
# Keep in sync with the Robolectric version in app/build.gradle (4.14.1) and the SDKs the tests
# run on (sdk 34 in @Config, 35 = highest SDK 4.14.1 supports, used for targetSdk 36).
set -euo pipefail

BASE="https://repo1.maven.org/maven2/org/robolectric/android-all-instrumented"
REPO="${HOME}/.m2/repository/org/robolectric/android-all-instrumented"
VERSIONS=(
  "14-robolectric-10818077-i7"
  "15-robolectric-12650502-i7"
)

for v in "${VERSIONS[@]}"; do
  dir="${REPO}/${v}"
  name="android-all-instrumented-${v}"
  mkdir -p "${dir}"
  for ext in jar pom; do
    file="${dir}/${name}.${ext}"
    url="${BASE}/${v}/${name}.${ext}"
    curl -fsSL --retry 6 --retry-all-errors --retry-delay 5 -o "${file}.sha512" "${url}.sha512"
    want="$(tr -d ' \n' < "${file}.sha512" | cut -c1-128)"
    if [[ -f "${file}" ]] && [[ "$(sha512sum "${file}" | cut -d' ' -f1)" == "${want}" ]]; then
      echo "cached  ${name}.${ext}"
      continue
    fi
    echo "fetch   ${name}.${ext}"
    curl -fsSL --retry 6 --retry-all-errors --retry-delay 5 -o "${file}.part" "${url}"
    got="$(sha512sum "${file}.part" | cut -d' ' -f1)"
    if [[ "${got}" != "${want}" ]]; then
      echo "sha512 mismatch for ${name}.${ext}" >&2
      rm -f "${file}.part"
      exit 1
    fi
    mv "${file}.part" "${file}"
  done
done
