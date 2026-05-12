#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  echo "usage: $0 <tag> <target-sha>" >&2
  exit 64
fi

tag="$1"
target_sha="$2"
success_marker='<!-- sonar-compliance-reports:release-success -->'
operator_marker='<!-- sonar-compliance-reports:release-origin=operator -->'

legacy_release_success() {
  gh api "repos/${GITHUB_REPOSITORY}/actions/workflows/release.yml/runs?event=release&status=completed&per_page=100" |
    jq -e --arg tag "${tag}" '
      any(
        .workflow_runs[]?;
        .conclusion == "success" and (.head_branch // "") == $tag
      )
    ' >/dev/null
}

if [[ -z "${GITHUB_OUTPUT:-}" ]]; then
  echo "GITHUB_OUTPUT must be set" >&2
  exit 1
fi

tag_sha=""
if git rev-parse -q --verify "refs/tags/${tag}^{commit}" >/dev/null 2>&1; then
  tag_sha="$(git rev-parse "refs/tags/${tag}^{commit}")"
fi

if [[ -n "${tag_sha}" && "${tag_sha}" != "${target_sha}" ]]; then
  echo "::error title=Existing tag points to the wrong commit::Tag ${tag} already points to ${tag_sha}, but the operator resolved ${target_sha}."
  exit 1
fi

release_json=""
if release_json="$(gh api "repos/${GITHUB_REPOSITORY}/releases/tags/${tag}" 2>/dev/null)"; then
  release_id="$(jq -r '.id' <<< "${release_json}")"
  release_body="$(jq -r '.body // ""' <<< "${release_json}")"

  if grep -Fq "${success_marker}" <<< "${release_body}"; then
    echo "action=skip" >> "${GITHUB_OUTPUT}"
    echo "release_id=${release_id}" >> "${GITHUB_OUTPUT}"
    echo "status_message=Skipped ${tag}: a successful release already exists." >> "${GITHUB_OUTPUT}"
    exit 0
  fi

  if grep -Fq "${operator_marker}" <<< "${release_body}"; then
    echo "::error title=Existing release requires rerun::GitHub release ${tag} already exists as id ${release_id}, but the previous operator run did not finish successfully. Use the Release workflow rerun path with version=${tag} and releaseId=${release_id}."
    exit 1
  fi

  if legacy_release_success; then
    bash .github/scripts/mark-release-success.sh "${release_id}"
    echo "action=skip" >> "${GITHUB_OUTPUT}"
    echo "release_id=${release_id}" >> "${GITHUB_OUTPUT}"
    echo "status_message=Skipped ${tag}: a successful legacy release run already exists." >> "${GITHUB_OUTPUT}"
    exit 0
  fi

  echo "::error title=Existing release requires rerun::GitHub release ${tag} already exists as id ${release_id}, but no successful prior release marker was found. Use the Release workflow rerun path with version=${tag} and releaseId=${release_id}."
  exit 1
fi

payload="$(jq -n \
  --arg tag "${tag}" \
  --arg target "${target_sha}" \
  --arg name "${tag}" \
  --arg body "${operator_marker}" \
  '{
    tag_name: $tag,
    target_commitish: $target,
    name: $name,
    body: $body,
    draft: false,
    prerelease: false,
    generate_release_notes: false
  }'
)"

release_json="$(gh api --method POST "repos/${GITHUB_REPOSITORY}/releases" --input - <<< "${payload}")"
release_id="$(jq -r '.id' <<< "${release_json}")"

echo "action=run" >> "${GITHUB_OUTPUT}"
echo "release_id=${release_id}" >> "${GITHUB_OUTPUT}"
echo "status_message=Prepared ${tag}: created GitHub release ${release_id} for ${target_sha}." >> "${GITHUB_OUTPUT}"
