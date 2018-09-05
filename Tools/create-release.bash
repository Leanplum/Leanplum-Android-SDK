#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Create a new release branch
#######################################
TYPE=$1

release_version=$(./Tools/create-release.py ${TYPE})
cat AndroidSDKCore/sdk-version.txt

git reset HEAD .
RELEASE_BRANCH:=release/${release_version}
git checkout -b ${RELEASE_BRANCH}

git add AndroidSDKCore/sdk-version.txt
git commit -m "Created release ${release_version}"
git push --set-upstream origin ${RELEASE_BRANCH}
