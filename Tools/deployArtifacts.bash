#!/usr/bin/env bash
set -o noglob
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Deploy artifacts to artifactory
#######################################
# bucket is either release or snapshot

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

jfrog rt u "leanplum-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum/${new_tag}/leanplum-${new_tag}.pom"

# # Leanplum
# jfrog rt u "{sdk_dir}/build/outputs/aar/AndroidSDK-release.aar" \
#   "libs-release-local/com/leanplum/Leanplum/${sdk_version}/Leanplum-${sdk_version}.aar"
# jfrog rt u "${sdk_dir}/build/intermediates/packaged-classes/release/classes.jar" \
#   "libs-release-local/com/leanplum/Leanplum/${sdk_version}/Leanplum-${sdk_version}.jar"
# sed -i '' -e "s|<artifactId>leanplum</artifactId>|<artifactId>Leanplum</artifactId>|g" \
# "leanplum-${old_tag}.pom"
# jfrog rt u "leanplum-${old_tag}.pom" \
#   "libs-release-local/com/leanplum/Leanplum/${new_tag}/Leanplum-${new_tag}.pom"




# leanplum-core
jfrog rt u "{sdk_core_dir}/build/outputs/aar/AndroidSDKCore-release.aar" \
  "libs-release-local/com/leanplum/leanplum-core/${sdk_version}/leanplum-core-${sdk_version}.aar"
jfrog rt u "${sdk_core_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-core/${sdk_version}/leanplum-core-${sdk_version}.jar"

##############################################################################
# this is updating the pom part, we can perhaps do this auto
sed -e "s|sdk_version|${sdk_version}|g" "Tools/artifact-poms/core.pom" > "leanplum-core-${sdk_version}.pom"
jfrog rt u "leanplum-core-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-core/${new_tag}/leanplum-core-${new_tag}.pom"






# leanplum-gcm
jfrog rt u "{sdk_gcm_dir}/build/outputs/aar/AndroidSDKGcm-release.aar" \
  "libs-release-local/com/leanplum/leanplum-gcm/${sdk_version}/leanplum-gcm-${sdk_version}.aar"
jfrog rt u "${sdk_gcm_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-gcm/${sdk_version}/leanplum-gcm-${sdk_version}.jar"

##############################################################################
# this is updating the pom part, we can perhaps do this auto/template
sed -e "s|sdk_version|${sdk_version}|g;s|sdk_module|leanplum-gcm|g" "Tools/artifact-poms/core.pom" > "leanplum-core-${sdk_version}.pom"
jfrog rt u "leanplum-gcm-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-gcm/${new_tag}/leanplum-gcm-${new_tag}.pom"

# leanplum-fcm
jfrog rt u "{sdk_fcm_dir}/build/outputs/aar/AndroidSDKFcm-release.aar" \
  "libs-release-local/com/leanplum/leanplum-fcm/${sdk_version}/leanplum-fcm-${sdk_version}.aar"
jfrog rt u "${sdk_fcm_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-fcm/${sdk_version}/leanplum-fcm-${sdk_version}.jar"
##############################################################################
# this is updating the pom part, we can perhaps do this auto
sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-fcm-${old_tag}.pom"
jfrog rt u "leanplum-fcm-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-fcm/${new_tag}/leanplum-fcm-${new_tag}.pom"

# leanplum-location
# shellcheck disable=SC2140
jfrog rt u "{sdk_location_dir}/build/outputs/aar/AndroidSDKLocation-release.aar" \
  "libs-release-local/com/leanplum/leanplum-location/${sdk_version}/"\
"leanplum-location-${sdk_version}.aar"
# shellcheck disable=SC2140
jfrog rt u "{sdk_location_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-location/${sdk_version}/"\
"leanplum-location-${sdk_version}.jar"
sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-location-${old_tag}.pom"
# shellcheck disable=SC2140
jfrog rt u "leanplum-location-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-location/${new_tag}/"\
"leanplum-location-${new_tag}.pom"

# leanplum-push
jfrog rt u "{sdk_push_dir}/build/outputs/aar/AndroidSDKPush-release.aar" \
  "libs-release-local/com/leanplum/leanplum-push/${sdk_version}/leanplum-push-${sdk_version}.aar"
jfrog rt u "${sdk_push_dir}/build/intermediates/packaged-classes/release/classes.jar" \
  "libs-release-local/com/leanplum/leanplum-push/${sdk_version}/leanplum-push-${sdk_version}.jar"
sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-push-${old_tag}.pom"
jfrog rt u "leanplum-push-${old_tag}.pom" \
  "libs-release-local/com/leanplum/leanplum-push/${new_tag}/leanplum-push-${new_tag}.pom"