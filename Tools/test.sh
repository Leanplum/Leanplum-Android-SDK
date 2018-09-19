#!/usr/bin/env bash
#
# LPM | Author: Ben Marten
# Copyright (c) 2017 Leanplum Inc. All rights reserved.
#
set -eo pipefail; [[ $DEBUG ]] && set -x
# shellcheck source=plugins/_common/constants.sh
source "Tools/_common/constants.sh"
# shellcheck source=plugins/_common/functions.sh
source "Tools/_common/functions.sh"

#######################################
# Runs Android Tests
# Globals:
#   None
# Arguments:
#   Tests to run, e.g.: --sdk, --uieditor
# Returns:
#   None
#######################################
main() {
  # Read cli args.
  for i in "$@"; do
    case $i in
      --sdk)
      sdk=true
      shift;;
      --uieditor)
      uieditor=true
      shift;;
    esac
  done
  # Prepare environment.
  prepare
  run_tests
}

#######################################
# Prepares test environment.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
prepare() {
  echo "Checking for test environment prerequisites..."
  if [[ -z "${JAVA_HOME+x}" ]]; then
    echo "Error: Your JAVA_HOME is not set." && exit 1;
  fi
  # if ! grep -q "org.bouncycastle.jce.provider.BouncyCastleProvider" \
  #     "$JAVA_HOME/jre/lib/security/java.security"; then
  #   echo "sudo vim $JAVA_HOME/jre/lib/security/java.security"
  #   echo "and add this line here:"
  #   echo "security.provider.10=..."
  #   echo "security.provider.11=org.bouncycastle.jce.provider.BouncyCastleProvider"
  #   exit 1
  # fi
}

#######################################
# Runs tests
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
run_tests() {
  if [[ ! -z ${sdk+x} ]]; then
    echo "Running only SDK unit tests..."
    run_sdk_tests
  elif [[ ! -z ${uieditor+x} ]]; then
    echo "Running UIEditor unit tests..."
    run_uieditor_tests
  else
    echo "Running all Android SDK unit tests..."
    run_sdk_tests
    # run_uieditor_tests -- out of scope
  fi  
}

#######################################
# Runs SDK tests only
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
run_sdk_tests() {
  cd "AndroidSDKTests"
  gradle clean assembleDebug testDebugUnitTest
  echo "${GREEN} Done.${NORMAL}"
}

#######################################
# Runs UIEditor tests only
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
run_uieditor_tests() {
  cd "$LEANPLUM_SDK_ROOT/Android/Leanplum-Android-UIEditor/AndroidSDKUIEditorTests"
  gradle clean assembleDebug testDebugUnitTest
  echo "${GREEN} Done.${NORMAL}"
}

main "$@"
