#!/usr/bin/env bash
#
# LPM | Author: Ben Marten
# Copyright (c) 2017 Leanplum Inc. All rights reserved.
#
set -eo pipefail; [[ $DEBUG ]] && set -x

# Check for Jenkins build number, otherwise default to curent time in seconds.
if [[ -z "${BUILD_NUMBER+x}" ]]; then 
  BUILD_NUMBER=$(date "+%s")
fi
export ANDROID_VERSION_STRING=${ANDROID_VERSION_STRING:-${ANDROID_VERSION}.${BUILD_NUMBER}}

for i in "$@"; do
  case $i in
    --upload)
    upload=true
    shift # past argument=value
    ;;
  esac
done

LEANPLUM_SDK_ROOT=${LEANPLUM_SDK_ROOT:-"$(pwd)/."}
configuration="Release"
default="${LEANPLUM_SDK_ROOT}"
android_dir=${android_dir:-$default/..}
default="${android_dir}/AndroidSDK"
sdk_dir=${sdk_dir:-$default}
release_dir="${android_dir}/Release"

rm -rf "$release_dir"
mkdir -p "$release_dir"

# Build the AndroidSDK using gradle.
rm -rf "${sdk_dir}/build"
rm -rf "${sdk_dir}/javadoc"
if [[ -z ${upload+x} ]]; then
  GRADLE_TASK="assemble${configuration} makeJar generateJavadoc"
else
  GRADLE_TASK="assemble${configuration} makeJar generateJavadoc artifactoryPublish"
fi

cd "${android_dir}/Leanplum-Android-SDK"
# shellcheck disable=SC2086
./gradlew $GRADLE_TASK

mv "${sdk_dir}/javadoc" "${release_dir}/."
cp "${sdk_dir}/build/intermediates/bundles/release/classes.jar" "${release_dir}/Leanplum.jar"

echo "${GREEN} Done.${NORMAL}"
