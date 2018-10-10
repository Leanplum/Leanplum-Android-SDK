#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

# shellcheck source=plugins/_common/constants.sh
source "Tools/_common/constants.sh"
# shellcheck source=plugins/_common/functions.sh
source "Tools/_common/functions.sh"

# Check for Jenkins build number, otherwise default to current time in seconds.
if [[ -z "${BUILD_NUMBER+x}" ]]; then
  BUILD_NUMBER=$(date "+%s")
fi

: ANDROID_VERSION: ${ANDROID_VERSION:="4.1.2"}

export ANDROID_VERSION_STRING="${ANDROID_VERSION}.${BUILD_NUMBER}"
# Determine between fork and our build, env var is read by the gradle build.
export LEANPLUM_PACKAGE_IDENTIFIER="p"

for i in "$@"; do
  case $i in
    --upload)
    upload=true
    shift;;
  esac
done

export base_sdk_dir="AndroidSDK"
export sdk_core_dir="AndroidSDKCore"
export sdk_fcm_dir="AndroidSDKFcm"
export sdk_gcm_dir="AndroidSDKGcm"
export sdk_location_dir="AndroidSDKLocation"
export sdk_push_dir="AndroidSDKPush"
ui_editor_dir="../Leanplum-Android-UIEditor/AndroidSDKUIEditor"
monitoring_dir="../Leanplum-Android-Monitoring/AndroidSDKMonitoring"
sample_dir="../Leanplum-Android-Sample/AndroidSDKSample"

# Build the AndroidSDK using gradle.
rm -rf "${base_sdk_dir}/build"
rm -rf "${base_sdk_dir}/javadoc"

./build.sh $@

# TODO NEED THESE MODULES LATER
# echo "Building Android UI Editor SDK..."
# cd "$ui_editor_dir/.."
# # shellcheck disable=SC2086
# gradle clean
# gradle assembleRelease
# gradle makeJar
# gradle generateJavadoc
# if [[ ! -z ${upload+x} ]]; then
#   gradle artifactoryPublish
# fi

# echo "Building Android SDK Monitoring..."
# cd "$monitoring_dir/.."
# # shellcheck disable=SC2086
# gradle clean
# gradle assembleRelease
# gradle makeJar
# gradle generateJavadoc
# if [[ ! -z ${upload+x} ]]; then
#   gradle artifactoryPublish
# fi

# cd "${base_sdk_dir}"
# cp "${monitoring_dir}/build/intermediates/bundles/release/classes.jar" \
#   "${sample_dir}/leanplum-monitoring.jar"
# cp "${ui_editor_dir}/build/intermediates/bundles/release/classes.jar" \
#   "${sample_dir}/leanplum-uieditor.jar"

