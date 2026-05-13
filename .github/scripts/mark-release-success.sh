#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -ne 1 ]]; then
  echo "usage: $0 <release-id>" >&2
  exit 64
fi

release_id="$1"
success_marker='<!-- sonar-compliance-reports:release-success -->'

release_json="$(gh api "repos/${GITHUB_REPOSITORY}/releases/${release_id}")"
release_body="$(jq -r '.body // ""' <<< "${release_json}")"

if grep -Fq "${success_marker}" <<< "${release_body}"; then
  exit 0
fi

updated_body="${release_body}"
if [[ -n "${updated_body}" ]]; then
  updated_body+=$'\n'
fi
updated_body+="${success_marker}"

payload="$(jq -n --arg body "${updated_body}" '{body: $body}')"
gh api --method PATCH "repos/${GITHUB_REPOSITORY}/releases/${release_id}" --input - <<< "${payload}" >/dev/null
