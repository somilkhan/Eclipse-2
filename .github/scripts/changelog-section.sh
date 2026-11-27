#!/usr/bin/env bash
# Prints the CHANGELOG.md section for a version, without its heading.
#   .github/scripts/changelog-section.sh 0.8.4 [CHANGELOG.md]
# Accepted headings: "## 0.8.4", "## v0.8.4", "## [0.8.4]", optionally followed by " - date" etc.
# For a pre-release (0.8.5-beta.1) the exact section is preferred, else the base version (0.8.5).
# Exits 1 with a message on stderr if no section (or an empty one) exists.
set -euo pipefail
version="${1:?usage: changelog-section.sh <version> [changelog]}"
file="${2:-CHANGELOG.md}"
[ -f "$file" ] || { echo "error: $file not found" >&2; exit 1; }

extract() {
  awk -v want="$1" '
    function norm(h) {
      sub(/^##[ \t]+/, "", h); sub(/^\[/, "", h); sub(/^v/, "", h)
      sub(/[\] \t].*$/, "", h)
      return h
    }
    /^## / { if (found) exit; if (norm($0) == want) { found = 1; next } }
    found { print }
  ' "$file"
}

base="${version%%-*}"
for v in "$version" "$base"; do
  body="$(extract "$v" | sed -e '/./,$!d')"   # drop leading blank lines
  if [ -n "$(printf '%s' "$body" | tr -d '[:space:]')" ]; then
    printf '%s\n' "$body"
    exit 0
  fi
done
echo "error: no non-empty '## $version' section in $file (add one before tagging)" >&2
exit 1
