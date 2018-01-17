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

LEANPLUM_SDK_ROOT="${LEANPLUM_SDK_ROOT:-"$(pwd)/."}"
configuration="Release"
default="${LEANPLUM_SDK_ROOT}"
android_dir=${android_dir:-$default}
default="${android_dir}/AndroidSDK"
sdk_dir=${sdk_dir:-$default}
release_dir="${android_dir}/Release"

sdk_release_dir="${release_dir}/AndroidSDK"
sdk_core_dir="${sdk_dir}/../AndroidSDKCore"
sdk_core_release_dir="${release_dir}/AndroidSDKCore"
sdk_fcm_dir="${sdk_dir}/../AndroidSDKFcm"
sdk_fcm_release_dir="${release_dir}/AndroidSDKFcm"
sdk_gcm_dir="${sdk_dir}/../AndroidSDKGcm"
sdk_gcm_release_dir="${release_dir}/AndroidSDKGcm"
sdk_location_dir="${sdk_dir}/../AndroidSDKLocation"
sdk_location_release_dir="${release_dir}/AndroidSDKLocation"
sdk_push_dir="${sdk_dir}/../AndroidSDKPush"
sdk_push_release_dir="${release_dir}/AndroidSDKPush"
echo HEYHEYHEY
echo $sdk_core_dir
echo $sdk_core_release_dir
echo $sdk_fcm_dir
echo $sdk_fcm_release_dir
echo $sdk_gcm_dir
echo $sdk_gcm_release_dir
echo $sdk_location_dir
echo $sdk_location_release_dir
echo $sdk_push_dir
echo $sdk_push_release_dir


rm -rf "$release_dir"
mkdir -p "$sdk_release_dir"
mkdir -p "$sdk_core_release_dir"
mkdir -p "$sdk_fcm_release_dir"
mkdir -p "$sdk_gcm_release_dir"
mkdir -p "$sdk_location_release_dir"
mkdir -p "$sdk_push_release_dir"

# Build the AndroidSDK using gradle.
rm -rf "${sdk_dir}/build"
rm -rf "${sdk_dir}/javadoc"

rm -rf "${sdk_core_dir}/build"
rm -rf "${sdk_core_dir}/javadoc"
rm -rf "${sdk_fcm_dir}/build"
rm -rf "${sdk_fcm_dir}/javadoc"
rm -rf "${sdk_gcm_dir}/build"
rm -rf "${sdk_gcm_dir}/javadoc"
rm -rf "${sdk_location_dir}/build"
rm -rf "${sdk_location_dir}/javadoc"
rm -rf "${sdk_push_dir}/build"
rm -rf "${sdk_push_dir}/javadoc"

if [[ -z ${upload+x} ]]; then
  GRADLE_TASK="assemble${configuration} makeJar generateJavadoc"
else
  GRADLE_TASK="assemble${configuration} makeJar generateJavadoc artifactoryPublish"
fi

cd "${android_dir}/Leanplum-Android-SDK"
# shellcheck disable=SC2086
./gradlew $GRADLE_TASK

# mkdir -p "${sdk_dir}/javadoc"
# mv "${sdk_dir}/javadoc" "${sdk_release_dir}/."
cp "${sdk_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_release_dir}/Leanplum.jar"

mv "${sdk_core_dir}/javadoc" "${sdk_core_release_dir}/."
cp "${sdk_core_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_core_release_dir}/Leanplum-core.jar"
mv "${sdk_fcm_dir}/javadoc" "${sdk_fcm_release_dir}/."
cp "${sdk_fcm_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_fcm_release_dir}/Leanplum-fcm.jar"
mv "${sdk_gcm_dir}/javadoc" "${sdk_gcm_release_dir}/."
cp "${sdk_gcm_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_gcm_release_dir}/Leanplum-gcm.jar"
mv "${sdk_location_dir}/javadoc" "${sdk_location_release_dir}/."
cp "${sdk_location_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_location_release_dir}/Leanplum-location.jar"
mv "${sdk_push_dir}/javadoc" "${sdk_push_release_dir}/."
cp "${sdk_push_dir}/build/intermediates/bundles/release/classes.jar" "${sdk_push_release_dir}/Leanplum-push.jar"

echo "${GREEN} Done.${NORMAL}"
