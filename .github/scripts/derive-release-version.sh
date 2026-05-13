#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  echo "usage: $0 <module-version> <build-number>" >&2
  exit 64
fi

module_version="$1"
build_number="$2"

if [[ -z "${module_version}" ]]; then
  echo "module version is required" >&2
  exit 1
fi

if [[ -z "${build_number}" ]]; then
  echo "build number is required" >&2
  exit 1
fi

if [[ "${module_version}" == *-SNAPSHOT ]]; then
  base_version="${module_version%-SNAPSHOT}"
  dot_count="$(awk -F'.' '{print NF - 1}' <<< "${base_version}")"

  if [[ "${dot_count}" -eq 1 ]]; then
    printf '%s.0.%s\n' "${base_version}" "${build_number}"
  else
    printf '%s.%s\n' "${base_version}" "${build_number}"
  fi
else
  printf '%s\n' "${module_version}"
fi
