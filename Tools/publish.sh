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

#######################################
# Moves the build from snapshot to specified channel.
# Globals:
#   None
# Arguments:
#   Release or snapshot publish.
#   Bucket name
#   New tag name.
#   Old tag name.
#   Build number.
# Returns:
#   None
#######################################
publish() {
  local release_type=$1
  local bucket=$2
  local new_tag=$3
  local old_tag=$4
  local build=$5

  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-${old_tag}.zip" \
    "gs://leanplum-sdk-${bucket}/Leanplum_Android-${new_tag}.zip"
  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-gcm-${old_tag}-mapping.zip" \
    "gs://leanplum-sdk-${bucket}/Leanplum_Android-gcm-${new_tag}-mapping.zip"
  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-fcm-${old_tag}-mapping.zip" \
    "gs://leanplum-sdk-${bucket}/Leanplum_Android-fcm-${new_tag}-mapping.zip"
  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-location-${old_tag}-mapping.zip" \
    "gs://leanplum-sdk-${bucket}/Leanplum_Android-location-${new_tag}-mapping.zip"
  # Set public acl on zip package only, NEVER on -mapping!
  if [[ $release_type = "release" ]]; then
    gsutil acl ch -u AllUsers:R "gs://leanplum-sdk-${bucket}/Leanplum_Android-${new_tag}.zip"
  fi

  if [[ $release_type = "release" ]]; then
    repo="repo"
  else
    repo="repo-internal"
  fi

  # SDK
  # Download specified build, and reupload with new tag
  if [ -d "/tmp/leanplum-${ANDROID_VERSION}.${build}" ]; then
    rm -rf "/tmp/leanplum-${ANDROID_VERSION}.${build}"
  fi
  cd /tmp && mkdir "Leanplum-${ANDROID_VERSION}.${build}" && \
    cd "Leanplum-${ANDROID_VERSION}.${build}"
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum/${ANDROID_VERSION}.${build}/"
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum-core/${ANDROID_VERSION}.${build}/"
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum-gcm/${ANDROID_VERSION}.${build}/" 
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum-fcm/${ANDROID_VERSION}.${build}/"
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum-location/${ANDROID_VERSION}.${build}/"
  # wget -q -r -nH -nd -np -R index.html \
  #   "https://repo-internal.leanplum.com/com/leanplum/leanplum-push/${ANDROID_VERSION}.${build}/"

  # leanplum
  jfrog rt u "leanplum-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum/${new_tag}/leanplum-${new_tag}.aar"
  jfrog rt u "leanplum-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum/${new_tag}/leanplum-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-${old_tag}.pom"
  jfrog rt u "leanplum-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum/${new_tag}/leanplum-${new_tag}.pom"

  # Leanplum
  jfrog rt u "leanplum-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/Leanplum/${new_tag}/Leanplum-${new_tag}.aar"
  jfrog rt u "leanplum-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/Leanplum/${new_tag}/Leanplum-${new_tag}.jar"
  sed -i '' -e "s|<artifactId>leanplum</artifactId>|<artifactId>Leanplum</artifactId>|g" \
  "leanplum-${old_tag}.pom"
  jfrog rt u "leanplum-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/Leanplum/${new_tag}/Leanplum-${new_tag}.pom"

  # leanplum-core
  jfrog rt u "leanplum-core-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-core/${new_tag}/leanplum-core-${new_tag}.aar"
  jfrog rt u "leanplum-core-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-core/${new_tag}/leanplum-core-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-core-${old_tag}.pom"
  jfrog rt u "leanplum-core-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-core/${new_tag}/leanplum-core-${new_tag}.pom"

  # leanplum-gcm
  jfrog rt u "leanplum-gcm-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-gcm/${new_tag}/leanplum-gcm-${new_tag}.aar"
  jfrog rt u "leanplum-gcm-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-gcm/${new_tag}/leanplum-gcm-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-gcm-${old_tag}.pom"
  jfrog rt u "leanplum-gcm-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-gcm/${new_tag}/leanplum-gcm-${new_tag}.pom"
 
 # leanplum-fcm
  jfrog rt u "leanplum-fcm-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-fcm/${new_tag}/leanplum-fcm-${new_tag}.aar"
  jfrog rt u "leanplum-fcm-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-fcm/${new_tag}/leanplum-fcm-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-fcm-${old_tag}.pom"
  jfrog rt u "leanplum-fcm-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-fcm/${new_tag}/leanplum-fcm-${new_tag}.pom"

  # leanplum-location
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-location-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-location/${new_tag}/"\
"leanplum-location-${new_tag}.aar"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-location-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-location/${new_tag}/"\
"leanplum-location-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-location-${old_tag}.pom"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-location-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-location/${new_tag}/"\
"leanplum-location-${new_tag}.pom"

 # leanplum-push
  jfrog rt u "leanplum-push-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-push/${new_tag}/leanplum-push-${new_tag}.aar"
  jfrog rt u "leanplum-push-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-push/${new_tag}/leanplum-push-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-push-${old_tag}.pom"
  jfrog rt u "leanplum-push-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-push/${new_tag}/leanplum-push-${new_tag}.pom"


  # UI Editor
  # Download specified build, and reupload with new tag
  if [ -d "/tmp/leanplum-uieditor-${ANDROID_VERSION}.${build}" ]; then
    rm -rf "/tmp/leanplum-uieditor-${ANDROID_VERSION}.${build}"
  fi
  cd /tmp && mkdir "leanplum-uieditor-${ANDROID_VERSION}.${build}" && \
    cd "leanplum-uieditor-${ANDROID_VERSION}.${build}"
  wget -q -r -nH -nd -np -R index.html \
    "https://repo-internal.leanplum.com/com/leanplum/leanplum-uieditor/${ANDROID_VERSION}.${build}/"

  # leanplum-uieditor
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-uieditor-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-uieditor/${new_tag}/"\
"leanplum-uieditor-${new_tag}.aar"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-uieditor-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-uieditor/${new_tag}/"\
"leanplum-uieditor-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-uieditor-${old_tag}.pom"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-uieditor-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-uieditor/${new_tag}/"\
"leanplum-uieditor-${new_tag}.pom"
    
  # UIEditor
  jfrog rt u "leanplum-uieditor-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/UIEditor/${new_tag}/UIEditor-${new_tag}.aar"
  jfrog rt u "leanplum-uieditor-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/UIEditor/${new_tag}/UIEditor-${new_tag}.jar"
  sed -i '' -e "s|<artifactId>leanplum-uieditor</artifactId>|<artifactId>UIEditor</artifactId>|g" \
    "leanplum-uieditor-${old_tag}.pom"
  jfrog rt u "leanplum-uieditor-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/UIEditor/${new_tag}/UIEditor-${new_tag}.pom"

  # AndroidSDKMonitoring
  # Download specified build, and reupload with new tag
  if [ -d "/tmp/leanplum-monitoring-${ANDROID_VERSION}.${build}" ]; then
    rm -rf "/tmp/leanplum-monitoring-${ANDROID_VERSION}.${build}"
  fi
  cd /tmp && mkdir "leanplum-monitoring-${ANDROID_VERSION}.${build}" && \
    cd "leanplum-monitoring-${ANDROID_VERSION}.${build}"
  wget -q -r -nH -nd -np -R index.html \
    "https://repo-internal.leanplum.com/com/leanplum/leanplum-monitoring/${ANDROID_VERSION}.${build}/"

  # leanplum-monitoring
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-monitoring-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/leanplum-monitoring/${new_tag}/"\
"leanplum-monitoring-${new_tag}.aar"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-monitoring-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/leanplum-monitoring/${new_tag}/"\
"leanplum-monitoring-${new_tag}.jar"
  sed -i '' -e "s|${old_tag}|${new_tag}|g" "leanplum-monitoring-${old_tag}.pom"
  # shellcheck disable=SC2140
  jfrog rt u "leanplum-monitoring-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/leanplum-monitoring/${new_tag}/"\
"leanplum-monitoring-${new_tag}.pom"
    
  # AndroidSDKMonitoring
  jfrog rt u "leanplum-monitoring-${old_tag}.aar" \
    "libs-${bucket}-local/com/leanplum/monitoring/${new_tag}/AndroidSDKMonitoring-${new_tag}.aar"
  jfrog rt u "leanplum-monitoring-${old_tag}.jar" \
    "libs-${bucket}-local/com/leanplum/monitoring/${new_tag}/AndroidSDKMonitoring-${new_tag}.jar"
  sed -i '' -e "s|<artifactId>leanplum-monitoring</artifactId>|<artifactId>AndroidSDKMonitoring</artifactId>|g" \
    "leanplum-monitoring-${old_tag}.pom"
  jfrog rt u "leanplum-monitoring-${old_tag}.pom" \
    "libs-${bucket}-local/com/leanplum/monitoring/${new_tag}/AndroidSDKMonitoring-${new_tag}.pom"
}

#######################################
# Updates the Android Test and Sample app dependencies to the specified version.
# Globals:
#   None
# Arguments:
#   The version string.
# Returns:
#   None
#######################################
updateTestApps() {
  local version_string=$1

  replace "Android/AndroidSDKTestApp/build.gradle" "$ANDROID_VERSION" "$version_string"
  replace "Android/AndroidSDKSample/build.gradle" "$ANDROID_VERSION" "$version_string"
  
  git add .
  git commit -m "[Android] Updated Test/Sample App SDK dependencies."
}

#######################################
# Publishes a specified SDK version to a specific channel.
# Globals:
#   None
# Arguments:
#   The release name, e.g.: 2017-01-30
#   The build name, e.g.: 55
#   The target channel name [alpha, beta, rc, stable].
# Returns:
#   None
#######################################
main() {
  # Read cli args.
  for i in "$@"; do
    case $i in
      --release=*)
        release="${i#*=}"
        shift;;
      --build=*)
        build="${i#*=}"
        shift;;
      --channel=*)
        channel="${i#*=}"
        shift;;
      --version=*)
        version="${i#*=}"
        shift;;
    esac
  done

  # Check for all required args.
  if [[ -z ${release+x} ]]; then
    echo "Please specify the release name, e.g. --release=release/apple/1.0.0" && exit 1
  fi
  if [[ -z ${build+x} ]]; then
    echo "Please specify the build number, e.g. --build=[1-999999]" && exit 1
  fi
  if [[ -z ${channel+x} ]]; then
    echo "Please specify the desired release channel, e.g. --channel=[alpha, beta, rc, stable]" &&
      exit 1
  fi
  if [[ "$channel" != "stable" ]] && [[ "$channel" != "private-stable" ]] &&
    [[ -z ${version+x} ]]; then
    echo "Please specify the release version, e.g. --version=[1, 2, ...]" &&
      exit 1
  fi

  echo "Publishing Android SDK build:${build} from '${release}' release to ${channel} channel..."

  old_tag="${ANDROID_VERSION}.${build}"
  if [[ $channel = "stable" ]]; then
    version_string="${ANDROID_VERSION}"
    publish "release" "release" "${ANDROID_VERSION}" "$old_tag" "$build"
  elif [[ $channel = "private-stable" ]]; then
    publish "private-release" "snapshot" "${ANDROID_VERSION}" "$old_tag" "$build"
  else
    version_string="${ANDROID_VERSION}-${channel}.${version}"
    publish "private" "snapshot" "${ANDROID_VERSION}-${channel}.${version}" "$old_tag" "$build"
  fi
  
  echo "${GREEN} Done.${NORMAL}"
}

main "$@"
