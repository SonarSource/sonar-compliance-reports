#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  echo "usage: $0 <version> <release-id>" >&2
  exit 64
fi

if [[ -z "${GITHUB_OUTPUT:-}" ]]; then
  echo "GITHUB_OUTPUT must be set" >&2
  exit 1
fi

if [[ -z "${GITHUB_REF_NAME:-}" ]]; then
  echo "GITHUB_REF_NAME must be set" >&2
  exit 1
fi

version="$1"
release_id="$2"
workflow_file="release.yml"
dispatch_time="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
dispatch_output=""
run_url=""
run_id=""

if ! dispatch_output="$(gh workflow run "${workflow_file}" --ref "${GITHUB_REF_NAME}" -f "version=${version}" -f "releaseId=${release_id}")"; then
  echo "::error title=Release dispatch failed::Unable to dispatch ${workflow_file} for ${version} (releaseId=${release_id})."
  exit 1
fi

if [[ "${dispatch_output}" =~ (https://github.com/[^[:space:]]+/actions/runs/([0-9]+)) ]]; then
  run_url="${BASH_REMATCH[1]}"
  run_id="${BASH_REMATCH[2]}"
fi

if [[ -z "${run_id}" ]]; then
  for _ in {1..20}; do
    run_id="$(
      gh run list \
        --workflow "${workflow_file}" \
        --branch "${GITHUB_REF_NAME}" \
        --event workflow_dispatch \
        --limit 20 \
        --json createdAt,databaseId,displayTitle \
        --jq "map(select(.displayTitle == \"Release ${version}\" and .createdAt >= \"${dispatch_time}\"))[0].databaseId // \"\""
    )"

    if [[ -n "${run_id}" ]]; then
      break
    fi

    sleep 3
  done
fi

if [[ -z "${run_id}" ]]; then
  echo "::error title=Dispatched release run not found::Triggered ${workflow_file} for ${version}, but could not resolve the downstream run id."
  exit 1
fi

if [[ -z "${run_url}" ]]; then
  run_url="https://github.com/${GITHUB_REPOSITORY}/actions/runs/${run_id}"
fi

echo "run_id=${run_id}" >> "${GITHUB_OUTPUT}"
echo "run_url=${run_url}" >> "${GITHUB_OUTPUT}"

echo "Watching downstream release run ${run_id} for ${version}."
gh run watch "${run_id}" --compact --interval 10 --exit-status
