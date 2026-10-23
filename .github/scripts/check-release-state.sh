#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo "usage: $0 <version> <target-sha> <default-branch-ref>" >&2
  exit 64
fi

version="$1"
target_sha="$2"
default_ref="$3"

if [[ -z "${GITHUB_OUTPUT:-}" ]]; then
  echo "GITHUB_OUTPUT must be set" >&2
  exit 1
fi

default_branch="${default_ref#origin/}"
if [[ "${GITHUB_REF_NAME:-}" != "${default_branch}" ]]; then
  echo "::error title=Not the default branch::The release operator must run on ${default_branch}, not on ${GITHUB_REF_NAME:-unknown}." >&2
  exit 1
fi

default_sha="$(git rev-parse "${default_ref}^{commit}")"
if [[ "${target_sha}" != "${default_sha}" ]]; then
  echo "::error title=Promoted commit is not the branch head::Commit ${target_sha} is promoted, but ${default_ref} is at ${default_sha}. The release workflow runs on the branch head: wait for its build to be promoted." >&2
  exit 1
fi

if release_json="$(gh release view "${version}" --repo "${GITHUB_REPOSITORY}" --json isDraft 2>/dev/null)"; then
  if [[ "$(jq -r '.isDraft' <<< "${release_json}")" == "false" ]]; then
    echo "action=skip" >> "${GITHUB_OUTPUT}"
    echo "status_message=Skipped ${version}: the release is already published." >> "${GITHUB_OUTPUT}"
    exit 0
  fi
  echo "action=run" >> "${GITHUB_OUTPUT}"
  echo "status_message=Resuming ${version}: reusing the existing draft release." >> "${GITHUB_OUTPUT}"
  exit 0
fi

echo "action=run" >> "${GITHUB_OUTPUT}"
echo "status_message=Releasing ${version} from ${target_sha}: the draft release is created by the release workflow." >> "${GITHUB_OUTPUT}"
