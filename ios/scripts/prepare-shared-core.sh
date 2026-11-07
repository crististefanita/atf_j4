#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
ios_root="$repo_root/ios"
j2objc_home="${1:-${J2OBJC_HOME:-}}"
if [[ -z "$j2objc_home" || ! -x "$j2objc_home/j2objc" ]]; then
  echo "J2OBJC_HOME must point to a built J2ObjC distribution." >&2
  exit 2
fi

generated="$ios_root/build/generated"
annotations="$ios_root/build/j2objc-annotations-3.1.jar"
mkdir -p "$generated" "$ios_root/build"
curl --fail --location --silent --show-error \
  "https://repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/3.1/j2objc-annotations-3.1.jar" \
  --output "$annotations"

core_sources="$repo_root/whist-lab-app/game-core/src/main/java"
names_sources="$repo_root/whist-lab-app/card-table-names/src/main/java"
bridge_sources="$repo_root/whist-lab-app/ios-bridge/src/main/java"
mapfile -t java_sources < <(find "$core_sources" "$names_sources" "$bridge_sources" -type f -name '*.java' | sort)
if [[ "${#java_sources[@]}" -eq 0 ]]; then
  echo "No shared Java sources were found." >&2
  exit 3
fi

"$j2objc_home/j2objc" \
  -source 17 \
  -sourcepath "$core_sources:$names_sources:$bridge_sources" \
  -classpath "$annotations" \
  -d "$generated" \
  "${java_sources[@]}"

if ! find "$generated" -type f -name 'WhistGameBridge.h' -print -quit | grep -q .; then
  echo "The translated bridge header was not generated." >&2
  exit 4
fi
test -f "$j2objc_home/lib/libjre_emul.a"
echo "Translated ${#java_sources[@]} shared Java sources into $generated"
