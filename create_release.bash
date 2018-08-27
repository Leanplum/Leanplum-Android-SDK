#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Create a new release branch
#######################################
main() {
  for i in "$@"; do
    case $i in
      --releaseType=*)
      releaseType="${i#*=}"
      shift;;
      --branch=*)
      branch="${i#*=}"
      shift;;
    esac
  done

  releaseVer=$( python release.py patch)
  echo $releaseVer

  git checkout "${branch}"
  git pull

  # create a branch, push on success
  git checkout -b "${release_version}"
  git push
}

main "$@"