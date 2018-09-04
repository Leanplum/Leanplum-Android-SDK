#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Deploy artifacts to artifactory
#######################################

# read version in from file
sdk_version=$(cat sdk-version.txt)
sdk_dir="AndroidSDK"
sdk_core_dir="AndroidSDKCore"
sdk_gcm_dir="AndroidSDKGcm"
sdk_fcm_dir="AndroidSDKFcm"
sdk_push_dir="AndroidSDKPush"
sdk_location_dir="AndroidSDKLocation"

# leanplum
jfrog rt u "{sdk_dir}/build/outputs/aar/AndroidSDK-release.aar" \
  "libs-release-local/com/leanplum/leanplum/${sdk_version}/leanplum-${sdk_version}.aar"
jfrog rt u "${sdk_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum/${sdk_version}/leanplum-${sdk_version}.jar"
./artifact-poms/template-pom.py module.pom "${sdk_version}" leanplum
jfrog rt u "leanplum-${sdk_version}.pom" \
  "libs-release-local/com/leanplum/leanplum/${new_tag}/leanplum-${sdk_version}.pom"

# leanplum-core
jfrog rt u "{sdk_core_dir}/build/outputs/aar/AndroidSDKCore-release.aar" \
  "libs-release-local/com/leanplum/leanplum-core/${sdk_version}/leanplum-core-${sdk_version}.aar"
jfrog rt u "${sdk_core_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-core/${sdk_version}/leanplum-core-${sdk_version}.jar"
./artifact-poms/template-pom.py core.pom "${sdk_version}" leanplum-core
jfrog rt u "leanplum-core-${sdk_version}.pom" \
  "libs-release-local/com/leanplum/leanplum-core/${sdk_version}/leanplum-core-${sdk_version}.pom"

# leanplum-gcm
jfrog rt u "{sdk_gcm_dir}/build/outputs/aar/AndroidSDKGcm-release.aar" \
  "libs-release-local/com/leanplum/leanplum-gcm/${sdk_version}/leanplum-gcm-${sdk_version}.aar"
jfrog rt u "${sdk_gcm_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-gcm/${sdk_version}/leanplum-gcm-${sdk_version}.jar"
./artifact-poms/template-pom.py module.pom "${sdk_version}" leanplum-gcm
jfrog rt u "leanplum-gcm-${sdk_version}.pom" \
  "libs-release-local/com/leanplum/leanplum-gcm/${sdk_version}/leanplum-gcm-${sdk-version}.pom"

# leanplum-fcm
jfrog rt u "{sdk_fcm_dir}/build/outputs/aar/AndroidSDKFcm-release.aar" \
  "libs-release-local/com/leanplum/leanplum-fcm/${sdk_version}/leanplum-fcm-${sdk_version}.aar"
jfrog rt u "${sdk_fcm_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-fcm/${sdk_version}/leanplum-fcm-${sdk_version}.jar"
./artifact-poms/template-pom.py module.pom "${sdk_version}" leanplum-fcm
jfrog rt u "leanplum-fcm-${sdk_version}.pom" \
  "libs-release-local/com/leanplum/leanplum-fcm/${sdk_version}/leanplum-fcm-${sdk-version}.pom"

# leanplum-location
jfrog rt u "{sdk_location_dir}/build/outputs/aar/AndroidSDKLocation-release.aar" \
  "libs-release-local/com/leanplum/leanplum-location/${sdk_version}/"\
"leanplum-location-${sdk_version}.aar"
jfrog rt u "{sdk_location_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-location/${sdk_version}/"\
"leanplum-location-${sdk_version}.jar"
./artifact-poms/template-pom.py module.pom "${sdk_version}" leanplum-location
jfrog rt u "leanplum-location-${sdk_version}.pom" \
  "libs-release-local/com/leanplum/leanplum-location/${sdk_version}/"\
"leanplum-location-${sdk_version}.pom"

# leanplum-push
jfrog rt u "{sdk_push_dir}/build/outputs/aar/AndroidSDKPush-release.aar" \
  "libs-release-local/com/leanplum/leanplum-push/${sdk_version}/leanplum-push-${sdk_version}.aar"
jfrog rt u "${sdk_push_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-push/${sdk_version}/leanplum-push-${sdk_version}.jar"
./artifact-poms/template-pom.py module.pom "${sdk_version}" leanplum-push
jfrog rt u "leanplum-push-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-push/${sdk_version}/leanplum-push-${sdk_version}.pom"