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
# Configures the testapp with given settings.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
main() {
  for i in "$@"; do
    case $i in
      -a=*|--app-id=*)
      app_id="${i#*=}"
      shift;;
      -p=*|--prod-key=*)
      prod_key="${i#*=}"
      shift;;
      -d=*|--dev-key=*)
      dev_key="${i#*=}"
      shift;;
      -ah=*|--api-host=*)
      api_host_name="${i#*=}"
      shift;;
      -sh=*|--socket-host=*)
      socket_host_name="${i#*=}"
      shift;;
      -sp=*|--socket-port=*)
      socket_port="${i#*=}"
      shift;;
      -s=*|--ssl=*)
      api_ssl="${i#*=}"
      shift;;
      -e=*|--environment=*)
      environment="${i#*=}"
      shift;;
    esac
  done

  # Check for all required args.
  if [[ -z ${app_id+x} ]]; then
    echo "Please specify the appId, e.g. --app-id=app_123..." && exit 1
  fi
  if [[ -z ${prod_key+x} ]]; then
    echo "Please specify the prodKey, e.g. --prod-key=prod_P123..." && exit 1
  fi
  if [[ -z ${dev_key+x} ]]; then
    echo "Please specify the devKey, e.g. --dev-key=dev_D123" && exit 1
  fi
  if [[ -z ${environment+x} ]]; then
    environment="prod"
  fi

  # Set defaults
  if [ "$environment" = "staging" ]; then
    api_host_name=${api_host_name:-"leanplum-staging.appspot.com"}
    socket_host_name=${socket_host_name:-"dev-staging.leanplum.com"}
    socket_port=${socket_port:-"80"}
    api_ssl=${api_ssl:-"true"}
  fi
  if [ "$environment" = "qa" ]; then
    api_host_name=${api_host_name:-"leanplum-qa-1372.appspot.com"}
    socket_host_name=${socket_host_name:-"dev-qa.leanplum.com"}
    socket_port=${socket_port:-"80"}
    api_ssl=${api_ssl:-"true"}
  fi
  if [ "$environment" = "prod" ]; then
    api_host_name=${api_host_name:-"www.leanplum.com"}
    socket_host_name=${socket_host_name:-"dev.leanplum.com"}
    socket_port=${socket_port:-"80"}
    api_ssl=${api_ssl:-"true"}
  fi

  template_src="../Leanplum-Android-SDK-Example/Configure.java.template"
  # shellcheck disable=SC2140
  template_dest="../Leanplum-Android-SDK-Example/app/src/main/"\
"java/com/leanplum/android/example/Configure.java"
  template_src_qa="../Leanplum-Android-TestApp/Configure.java.template"
  # shellcheck disable=SC2140
  template_dest_qa="../Leanplum-Android-TestApp/AndroidSDKTestApp/src/"\
"main/java/com/leanplum/testproj/Configure.java"
  echo "Copying configure file to projects..."
  cp "$template_src" "$template_dest"
  cp "$template_src_qa" "$template_dest_qa"
  replaceFileConfigPlaceholder "$app_id" "$prod_key" "$dev_key" "$api_host_name" "$api_ssl" \
    "$socket_host_name" "$socket_port" "${template_dest}";
  replaceFileConfigPlaceholder "$app_id" "$prod_key" "$dev_key" "$api_host_name" "$api_ssl" \
    "$socket_host_name" "$socket_port" "${template_dest_qa}";

  echo "Setup finished."
}

main "$@"
