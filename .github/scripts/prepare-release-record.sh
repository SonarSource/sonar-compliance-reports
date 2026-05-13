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
  local page=1
  local runs_json
  local run_count

  while :; do
    runs_json="$(gh api "repos/${GITHUB_REPOSITORY}/actions/workflows/release.yml/runs?event=release&status=success&per_page=100&page=${page}")"

    if jq -e --arg tag "${tag}" '
      any(
        .workflow_runs[]?;
        (.head_branch // "") == $tag
      )
    ' <<< "${runs_json}" >/dev/null; then
      return 0
    fi

    run_count="$(jq -r '.workflow_runs | length' <<< "${runs_json}")"
    if [[ "${run_count}" -lt 100 ]]; then
      break
    fi

    page=$((page + 1))
  done

  return 1
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

create_release_response_file="$(mktemp)"
create_release_stderr_file="$(mktemp)"

cleanup_create_release_files() {
  rm -f "${create_release_response_file}" "${create_release_stderr_file}"
}

if GH_DEBUG=api gh api --method POST "repos/${GITHUB_REPOSITORY}/releases" --input - >"${create_release_response_file}" 2>"${create_release_stderr_file}" <<< "${payload}"; then
  release_json="$(cat "${create_release_response_file}")"
  cleanup_create_release_files
else
  status=$?
  echo "::error title=Release creation failed::GitHub rejected release ${tag} for ${target_sha} with exit code ${status}."
  echo "::group::Release creation payload"
  printf '%s\n' "${payload}"
  echo "::endgroup::"
  echo "::group::gh api stderr"
  cat "${create_release_stderr_file}" >&2
  echo "::endgroup::"
  if [[ -s "${create_release_response_file}" ]]; then
    echo "::group::GitHub API response body"
    cat "${create_release_response_file}"
    echo "::endgroup::"
  fi
  cleanup_create_release_files
  exit "${status}"
fi

release_id="$(jq -r '.id' <<< "${release_json}")"

echo "action=run" >> "${GITHUB_OUTPUT}"
echo "release_id=${release_id}" >> "${GITHUB_OUTPUT}"
echo "status_message=Prepared ${tag}: created GitHub release ${release_id} for ${target_sha}." >> "${GITHUB_OUTPUT}"
