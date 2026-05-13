#!/usr/bin/env bash

set -euo pipefail

format="url"

usage() {
  echo "usage: $0 [--format=url|tsv] <commit-sha>" >&2
}

case "$#" in
  1)
    ;;
  2)
    case "$1" in
      --format=url)
        ;;
      --format=tsv)
        format="tsv"
        ;;
      *)
        usage
        exit 64
        ;;
    esac
    shift
    ;;
  *)
    usage
    exit 64
    ;;
esac

if [[ -z "${GITHUB_REPOSITORY:-}" ]]; then
  echo "GITHUB_REPOSITORY must be set" >&2
  exit 1
fi

commit_sha="$1"
repo_name="${GITHUB_REPOSITORY#*/}"
status_json="$(gh api "repos/${GITHUB_REPOSITORY}/commits/${commit_sha}/status")"

echo "${status_json}" | jq -r --arg repo "${repo_name}" --arg format "${format}" '
  def promoted_build_match:
    capture("/ui/builds/" + $repo + "/(?<build_number>[0-9]+)/?$")?;

  (
    [
      .statuses[]
      | select(.state == "success")
      | select(.target_url != null)
      | .target_url as $url
      | ($url | promoted_build_match) as $match
      | select($match != null)
      | {url: $url, build_number: $match.build_number}
    ][0]?
  ) as $result
  | if $result == null then
      empty
    elif $format == "tsv" then
      [$result.url, $result.build_number] | @tsv
    else
      $result.url
    end
'
