#!/usr/bin/env bash
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



export base_sdk_dir="AndroidSDK"
export sdk_core_dir="AndroidSDKCore"
export sdk_fcm_dir="AndroidSDKFcm"
export sdk_gcm_dir="AndroidSDKGcm"
export sdk_location_dir="AndroidSDKLocation"
export sdk_push_dir="AndroidSDKPush"
ui_editor_dir="../Leanplum-Android-UIEditor/AndroidSDKUIEditor"
monitoring_dir="../Leanplum-Android-Monitoring/AndroidSDKMonitoring"
sample_dir="../Leanplum-Android-Sample/AndroidSDKSample"


cp "${sdk_core_dir}/build/intermediates/bundles/release/classes.jar" "${sample_dir}/leanplum-core.jar"
mkdir -p "${sample_dir}/javadoc/leanplum-core/" 
cp -r "Release/${sdk_core_dir}/javadoc/." "${sample_dir}/javadoc/leanplum-core/"
cp "${sdk_fcm_dir}/build/intermediates/bundles/release/classes.jar" "${sample_dir}/leanplum-fcm.jar"
mkdir -p "${sample_dir}/javadoc/leanplum-fcm/" 
cp -r "Release/${sdk_fcm_dir}/javadoc/." "${sample_dir}/javadoc/leanplum-fcm/"
cp "${sdk_gcm_dir}/build/intermediates/bundles/release/classes.jar" "${sample_dir}/leanplum-gcm.jar"
mkdir -p "${sample_dir}/javadoc/leanplum-gcm/" 
cp -r "Release/${sdk_gcm_dir}/javadoc/." "${sample_dir}/javadoc/leanplum-gcm/"
cp "${sdk_push_dir}/build/intermediates/bundles/release/classes.jar" "${sample_dir}/leanplum-push.jar"
mkdir -p "${sample_dir}/javadoc/leanplum-push/"
cp -r "Release/${sdk_push_dir}/javadoc/." "${sample_dir}/javadoc/leanplum-push/"
cp "${sdk_location_dir}/build/intermediates/bundles/release/classes.jar" "${sample_dir}/leanplum-location.jar"
mkdir -p "${sample_dir}/javadoc/leanplum-location"
cp -r "Release/${sdk_location_dir}/javadoc/." "${sample_dir}/javadoc/leanplum-location/"

# Copy the message templates but change their package.
cp -R "${sdk_core_dir}/src/main/java/com/leanplum/messagetemplates/" \
    "${sample_dir}/src/com/leanplum/customtemplates"
for file in ${sample_dir}/src/com/leanplum/customtemplates/*
do
  sed "s/com.leanplum.messagetemplates/com.leanplum.customtemplates/g" < "$file" > "${file}.tmp"
  mv "${file}.tmp" "$file"
done


######
pushd "${sample_dir}"

cp "src/com/leanplum/sample/MyApp.java" "src/com/leanplum/sample/MyApp-original.java"
cp "src/com/leanplum/sample/MyApp-release.java" "src/com/leanplum/sample/MyApp.java"

mkdir -p Release

zip -9r "Release/Leanplum_Android-$ANDROID_VERSION_STRING.zip" \
./* -x Release\* bin/\* build/\* \*-\*.java
popd
#######

pushd "${sdk_fcm_dir}/build/outputs/mapping/release"
zip -9r "Leanplum_Android-fcm-$ANDROID_VERSION_STRING-mapping.zip" ./*
popd

pushd "${sdk_gcm_dir}/build/outputs/mapping/release"
zip -9r "Leanplum_Android-gcm-$ANDROID_VERSION_STRING-mapping.zip" ./*
popd

pushd "${sdk_location_dir}/build/outputs/mapping/release"
zip -9r "Leanplum_Android-location-$ANDROID_VERSION_STRING-mapping.zip" ./*
popd

  # Upload to gcloud snapshot bucket.
  gsutil cp "${sample_dir}/Release/Leanplum_Android-$ANDROID_VERSION_STRING.zip" \
    gs://leanplum-sdk-snapshot
  # shellcheck disable=SC2140
  gsutil cp "${sdk_fcm_dir}/build/outputs/mapping/release/"\
"Leanplum_Android-fcm-$ANDROID_VERSION_STRING-mapping.zip" gs://leanplum-sdk-snapshot
  # shellcheck disable=SC2140
  gsutil cp "${sdk_gcm_dir}/build/outputs/mapping/release/"\
"Leanplum_Android-gcm-$ANDROID_VERSION_STRING-mapping.zip" gs://leanplum-sdk-snapshot
  # shellcheck disable=SC2140
  gsutil cp "${sdk_location_dir}/build/outputs/mapping/release/"\
"Leanplum_Android-location-$ANDROID_VERSION_STRING-mapping.zip" gs://leanplum-sdk-snapshot

pushd "${sample_dir}"
mv "src/com/leanplum/sample/MyApp-original.java" "src/com/leanplum/sample/MyApp.java"

echo "${GREEN} Done.${NORMAL}"
