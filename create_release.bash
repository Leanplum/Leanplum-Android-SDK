#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Create a new release branch
#######################################

# Needs to know what type of release we will be
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
  
  local releaseType="patch"
  local branch="develop"

  releaseVer=$( python release.py patch)
  echo $releaseVer
  #First we need to git checkout develop and fetch and pull
  git checkout "${branch}"
  git pull

  # create a branch, push on success
  git checkout -b "${release_version}"
  git push
}

main