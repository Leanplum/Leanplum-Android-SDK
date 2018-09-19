#!/usr/bin/env bash
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

# shellcheck source=plugins/_common/ask.sh
source "Tools/_common/ask.sh"

# Use colors, but only if connected to a terminal, and that terminal
# supports them.
if [ -t 1 ] && which tput >/dev/null 2>&1; then
  colors=$(tput colors)
fi
if [ -z "${NO_COLORS+x}" ] && [ -t 1 ] && [ -n "$colors" ] && [ "$colors" -ge 8 ]; then
  RED="$(tput setaf 1)" && export RED
  GREEN="$(tput setaf 2)" && export GREEN
  YELLOW="$(tput setaf 3)" && export YELLOW
  BLUE="$(tput setaf 4)" && export BLUE
  BOLD="$(tput bold)" && export BOLD
  NORMAL="$(tput sgr0)" && export NORMAL
else
  export RED=""
  export GREEN=""
  export YELLOW=""
  export BLUE=""
  export BOLD=""
  export NORMAL=""
fi

#######################################
# Print out error messages along with other status information.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
err() {
  printf "%s%s%s\n" "${RED}ERROR [$(date +'%Y-%m-%dT%H:%M:%S%z')]: " "$@" "${NORMAL}" >&2
}

#######################################
# Runs a sub command and only outputs the stderr to console, then exits.
# Globals:
#   None
# Arguments:
#   Description of the command to run.
#   The command to run.
# Returns:
#   None
#######################################
run() {
  echo "$1"
  local cmd=${*:2}

  set +o errexit
  local error
  error=$(${cmd} 2>&1 >/dev/null)
  set -o errexit

  if [ -n "$error" ]; then
    err "Error running command: '$cmd':" "$error"
    exit 1
  fi
}



#######################################
# Configures the leanplum/leanplum directory.
# Globals:
#   LEANPLUM_ROOT - Sets the root of the leanplum/leanplum project.
#   LEANPLUM_SDK_ROOT - Sets the root of the leanplum/sdk project.
#   LEANPLUM_SDK_CONFIG - The path to the sdk config file.
#   IOS_VERSION - The current Apple SDK version.
#   ANDROID_VERSION - The current Android SDK version.
#   UNITY_VERSION - The current Unity SDK version.
#   AIR_VERSION - The current Air SDK version.
#   JAVASCRIPT_VERSION - The current Javascript SDK version.
#   WARNING - Will set a warning on failure.
# Arguments:
#   None
# Returns:
#   None
#######################################
# lpm_init_leanplum() {
#   # If lpm config file exists read leanplum root value.
#   if [[ -f "$LPM_CONFIG_FILE" ]]; then
#     LEANPLUM_ROOT=$(jq -r '.leanplumRoot' "$LPM_CONFIG_FILE") && export LEANPLUM_ROOT
#     LEANPLUM_SDK_ROOT=$(jq -r '.leanplumSdkRoot' "$LPM_CONFIG_FILE") && \
#       export LEANPLUM_SDK_ROOT
#   else
#     WARNING="${YELLOW}Warning: Could not find ~/.lpm/.config file. " \
# "Some commands will not work as expected.${NORMAL}"
#   fi

#   if [[ "$LEANPLUM_ROOT" != "null" ]]; then
#     export LEANPLUM_SDK_CONFIG="$LEANPLUM_SDK_ROOT/.sdkconfig"
#     if [[ -f $LEANPLUM_SDK_CONFIG ]]; then
#       IOS_VERSION=$(jq -r '.development.ios' "$LEANPLUM_SDK_CONFIG") && \
#         export IOS_VERSION
#       ANDROID_VERSION=$(jq -r '.development.android' "$LEANPLUM_SDK_CONFIG") && \
#         export ANDROID_VERSION
#       UNITY_VERSION=$(jq -r '.development.unity' "$LEANPLUM_SDK_CONFIG") && \
#         export UNITY_VERSION
#       AIR_VERSION=$(jq -r '.development.air' "$LEANPLUM_SDK_CONFIG") && \
#         export AIR_VERSION
#       JAVASCRIPT_VERSION=$(jq -r '.development.javascript' "$LEANPLUM_SDK_CONFIG") && \
#         export JAVASCRIPT_VERSION
#     else
#       # shellcheck disable=SC2140
#       WARNING="${YELLOW}Warning: Could not find .sdkconfig in your Leanplum Root. "\
# "Some commands will not work as expected.${NORMAL}"
#       export WARNING
#     fi
#   fi
# }

#######################################
# Trims a string from spaces.
# Globals:
#   None
# Arguments:
#   The string to trim.
# Returns:
#   The trimmed string.
#######################################
trim() {
  local var="$*"
  var="${var#"${var%%[![:space:]]*}"}"   # remove leading whitespace characters
  var="${var%"${var##*[![:space:]]}"}"   # remove trailing whitespace characters
  echo -n "$var"
}

#######################################
# Retrieves the release notes.
# Globals:
#   None
# Arguments:
#   The leanplum submodule to detect changes.
# Returns:
#   The release notes.
#######################################
leanplum_release_notes() {
  local submodule=$1
  cd "$LEANPLUM_SDK_ROOT/${submodule}"
  # shellcheck disable=SC2140

  previous_release_tag=$(git tag | tail -n2 | head -n1)
  # If no tag is previous release tag is present, fallback to root commit.
  if [[ -z $previous_release_tag ]]; then
    previous_release_tag=$(git rev-list --max-parents=0 HEAD)
  fi

  release_notes=$(git log --no-merges --pretty=format:'- %cd %h %s <%an> %b' --abbrev-commit \
    --date=short --notes "${previous_release_tag}..HEAD")
  cd ~-

  printf "%s\n" "$release_notes"
}

#######################################
# Adds everything in current directory and commits with message.
# Globals:
#   None
# Arguments:
#   The commit message.
# Returns:
#   None
#######################################
git_commit_new_version() {
  git add . && git commit -m "$1"
}

#######################################
# Verifies if current working dir has changes, if yes fails.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
git_validate_clean_working_dir() {
  if [ ! -z "$(git status --porcelain)" ]; then
    echo "${RED}Working directory not clean; aborting...${NORMAL}"
    exit 1
  fi
}

#######################################
# Replaces a string in a file and checks for success via git status.
# Globals:
#   None
# Arguments:
#   The path to file.
#   The string to replace.
#   The new string.
#   Pass any value (e.g. 0) to disable validation.
# Returns:
#   None
#######################################
replace() {
  sed -i '' -e "s|$2|$3|g" "$1"
  cd "$(dirname "$1")" # Change to directory containing the file and check for success.
  if [[ -z "${4}" ]] && ! git status --porcelain 2>/dev/null | grep "$(basename "$1")"; then
    echo "${RED}Error patching file: $1${NORMAL}" && exit 1
  fi
  cd ~- # Change back to original folder.
  echo "Updated file: $1"
}


#######################################
# Downloads the Android SDK from internal repository.
# Globals:
#   None
# Arguments:
#   The version to download, e.g. "1.3.2+55"
# Returns:
#   None
#######################################
download_android_sdk() {
  local version=$1
  local repo=https://repo-internal.leanplum.com/com/leanplum/Leanplum/

  err "Not implemented."
}

#######################################
# Downloads the Android SDK from internal repository.
# Globals:
#   None
# Arguments:
#   The version to download, e.g. "1.3.2+55"
#   The target dir
# Returns:
#   None
#######################################
download_android_sdk_zip() {
  local version=$1
  local target_dir=$2

  echo "Downloading AppleSDK ${version} ..."
  if [ ! -d "/tmp/Leanplum_Android-${version}.zip" ]; then
    rm -rf "/tmp/Leanplum_Android-${version}.zip"
  fi
  if [ ! -d "/tmp/Leanplum_Android-${version}-mapping.zip" ]; then
    rm -rf "/tmp/Leanplum_Android-${version}-mapping.zip"
  fi

  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-${version}.zip" "$target_dir"
  gsutil cp "gs://leanplum-sdk-snapshot/Leanplum_Android-${version}-mapping.zip" "$target_dir"
  
  echo "Finished downloading Android SDK."
}

#######################################
# Publishes to Cocoapods.
# Globals:
#   None
# Arguments:
#   The name of the project
#   The name of the podspec
#   Location of the .framework file
#   Trunk or any other repository
#   The version name
#   The release notes
# Returns:
#   None
#######################################
pod_publish() {
  local project=$1
  local podspec=$2
  local framework_path=$3
  local repo=$4
  local version_string=$5
  local release_notes=$6

  local framework pod_commit_hash

  if [[ ! -d "$LEANPLUM_ROOT/../${project}/" ]]; then
    cd "$LEANPLUM_ROOT/../"
    git clone "git@github.com:Leanplum/${project}.git"
  fi
  cd "$LEANPLUM_ROOT/../${project}/" && git reset --hard && git clean -f && git pull --tags
  framework="$(basename "${framework_path}")"
  rm -rf "$framework"
  
  # Replace podspec versions.
  replace "../${project}/${podspec}.podspec" "s.version = .*" "s.version = \'${version_string}\'"
  # Use github release as a binary.
  github_release="https://github.com/Leanplum/${project}/releases/download/${version_string}/"
  github_release="${github_release}${framework}.zip"
  replace "../${project}/${podspec}.podspec" "s.source = .*" \
    "s.source = { :http => \'${github_release}\' }"
  # Replace version of dependency.
  replace "../${project}/${podspec}.podspec" "\'~> .*" "\'~> ${version_string}\'"

  git_commit_new_version "${version_string}"
  pod_commit_hash=$(git rev-parse HEAD)
  git push

  cp -rf "${framework_path}" .
  zip -9r "${framework}.zip" "${framework}"
  create_github_release "$GITHUB_KEY" "Leanplum" "${project}" \
    "${version_string}" "$pod_commit_hash" "Leanplum iOS SDK ${version_string}" \
    "${release_notes}" "$LEANPLUM_ROOT/../${project}/${framework}.zip"
  rm -rf "${framework}.zip"
  rm -rf "${framework}"

  pod repo update
  if [[ ${repo} = "trunk" ]]; then
    pod trunk push "${podspec}.podspec"
  else
    pod repo push "${repo}" "${podspec}.podspec" --sources=leanplum
  fi
  cd ~-
}

#######################################
# Upload files to google cloud bucket.
# Globals:
#   None
# Arguments:
#   The path to the file to be uploaded
# Returns:
#   None
#######################################
upload_testapp() {
  local file_path=$1
  gsutil cp "$file_path" gs://testapp-builds
}

#######################################
# Zips file and stores it with given name. 
# Globals:
#   None
# Arguments:
#   The path to the file to be uploaded
# Returns:
#   None
#######################################
zip_path() {
  local zipped_name=$1
  local file_path=$2
  zip -r9 -x "*.DS_Store" -x "__MACOSX/" -x "__MACOSX/*" "$zipped_name" "$file_path"
}

#######################################
# Creates a new github release posting to the github Rest API.
# Globals:
#   None
# Arguments:
#   The github token.
#   The owner name.
#   The repository name. 
#   The tag name.
#   The target commit hash for the tag.
#   The name of the release.
#   The body text of the release.
#   The binary to attach.
# Returns:
#   None
#######################################
create_github_release() {
  local token="$1";
  local owner="$2";
  local repo="$3";
  local tag_name="$4";
  local target_commitish="$5";
  local name="$6";
  local body="$7";
  local binary="$8";

  local payload="\"tag_name\":\"$tag_name\"";
  payload="$payload,\"target_commitish\":\"$target_commitish\"";
  payload="$payload,\"name\":\"$name\"";
  payload="$payload,\"body\":\"$body\"";
  payload="$payload,\"draft\":false";
  payload="$payload,\"prerelease\":false";
  payload="{$payload}";

  echo "Creating github release $name..."
  upload_url=$(curl --fail -s -S -X POST "https://api.github.com/repos/$owner/$repo/releases" \
    -H "Accept: application/vnd.github.v3+json" \
    -H "Authorization: token $token" \
    -H "Content-Type: application/json" \
    -d "$payload" | jq -r '.upload_url');

  binary_name=$(basename "$binary")
  # shellcheck disable=SC2001
  upload_url=$(echo "$upload_url" | sed "s/{?name,label}/?name=$binary_name/g")

  echo "Attaching binary to release..."
  # Attach binary to release
  curl --fail -s -S -X POST "$upload_url" \
    -H "Authorization: token $token" \
    -H "Content-Type: application/zip" \
    --data-binary "@$binary";
}

#######################################
# Initializes the leanplum internal cocoapods repo.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
pod_init_leanplum_repo() {
  if [[ ! -d ~/.cocoapods/repos/leanplum ]]; then
    pod repo add leanplum git@github.com:Leanplum/CocoaPods.git
  fi
  pod repo update leanplum
}

#######################################
# Opens vim to edit the content of a variable and returns it, usage: new=$(quickedit "$old")
# Globals:
#   None
# Arguments:
#   The variable to edit.
# Returns:
#   None
#######################################
quickedit() {
  local to_edit=$1
  
  echo "$to_edit" > ~/temp$$
  trap 'rm ~/temp$$' exit; vim ~/temp$$ >/dev/tty; cat ~/temp$$
}

#######################################
# Shows the git diff in current directory and asks if to proceed.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
git_show_diff() {
  local hr="--------------------------------------------------------------------------------"
  printf "\n%s\n%s\n" "${GREEN}Here's what changed:${NORMAL}" "$hr"
  git diff --color --unified=0
  echo $hr
  if ! ask "${BLUE}Do you want to proceed?${NORMAL}" Y; then
    exit 1;
  fi
}

#######################################
# Update the whatsnew release notes.
# Globals:
#   None
# Arguments:
#   The branch prefix, e.g. 'ios'.
#   The name of the Platform, e.g. 'iOS'.
#   The release notes.
#   The current version.
# Returns:
#   None
#######################################
function update_release_notes() {
  local branch_prefix=$1
  local platform=$2
  local release_notes=$3
  local current_version=$4

  local release_date release_notes html1 html2 line

  if [[ $branch_prefix = "apple" ]]; then
    folder="AppleSDK"
  elif [[ $branch_prefix = "android" ]]; then
    folder="Android"
  elif [[ $branch_prefix = "unity" ]]; then
    folder="Unity SDK"
  fi

  echo "Gathering release notes..."
  
  release_date=$(date "+%B %d, %Y")
  local html1='<li class="help-copy-nm"><span class="version">'"${current_version}"
  local html2=' - '"$release_date"'</span> '"$release_notes"'</li>'

  # Get line number of relevant section:
  local file_path="Server/static/handlebars/help/whatsnew.hbs"
  line=$(grep -n "{{spyHeader '${platform}' '${branch_prefix}'}}" ${file_path} | cut -d : -f 1)
  line=$((line+3))
  IFS='%'
  gsed -i "${line}i           ${html1}${html2}" "${file_path}"
  unset IFS
}

#######################################
# Creates the AppleSDK docs from release branch.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
create_docs_release() {
  local platform=$1

  local folder

  echo "Generating docs from release branch and amending to commit..."
  if [[ ${platform} = "apple" ]]; then
    create_apple_sdk_docs
    folder="iosdocs"
  elif [[ ${platform} = "android" ]]; then
    create_android_sdk_docs
    folder="javadoc"
  else
    return 0
  fi

  rm -rf "/tmp/${folder}"
  mv "$LEANPLUM_ROOT/Server/static/${folder}" "/tmp/"
  git clean -fd
  git checkout master
  rm -rf "$LEANPLUM_ROOT/Server/static/${folder}"
  mv "/tmp/${folder}" "$LEANPLUM_ROOT/Server/static/"
  git add .
  git commit --amend --no-edit
}

#######################################
# Creates the AppleSDK docs.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
create_apple_sdk_docs() {
  echo "Creating docs ..."
  cd "$LEANPLUM_SDK_ROOT/Apple/Leanplum-iOS-SDK"
  mkdir -p "../Release/docs"
  doxygen .doxygen
  rm -rf "$LEANPLUM_ROOT/Server/static/iosdocs"
  cp -r "$LEANPLUM_SDK_ROOT/Apple/Release/docs" "$LEANPLUM_ROOT/Server/static/iosdocs"
  cd ~-
}

#######################################
# Creates the AndroidSDK docs.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
create_android_sdk_docs() {
  echo "Creating docs ..."
  gradle clean assembleRelease generateJavadoc
  rm -rf "$LEANPLUM_ROOT/Server/static/javadoc"
  cp -r "AndroidSDKCore/javadoc" \
    "$LEANPLUM_ROOT/Server/static/javadoc"
  cp -r "AndroidSDKPush/javadoc" \
    "$LEANPLUM_ROOT/Server/static/javadoc"
  cp -r "AndroidSDKGcm/javadoc" \
    "$LEANPLUM_ROOT/Server/static/javadoc"
  cp -r "AndroidSDKFcm/javadoc" \
    "$LEANPLUM_ROOT/Server/static/javadoc"
  cp -r "AndroidSDKLocation/javadoc" \
    "$LEANPLUM_ROOT/Server/static/javadoc"
}

#######################################
# Configures the Configure.template for the sdk test apps.
# Globals:
#   None
# Arguments:
#   appId
#   prodKey
#   devKey
#   apiHostName
#   apiSsl
#   socketHostName
#   socketPort
#   destination
# Returns:
#   None
#######################################
replaceFileConfigPlaceholder() {
  local app_id=$1
  local prod_key=$2
  local dev_key=$3
  local api_host_name=$4
  local api_ssl=$5
  local socket_host_name=$6
  local socket_port=$7
  local destination=$8

  echo "Customizing configuration..."
  sed -i "" -e "s/{{APP_ID}}/${app_id}/g" "$destination"
  sed -i "" -e "s/{{PRODUCTION_KEY}}/${prod_key}/g" "$destination"
  sed -i "" -e "s/{{DEVELOPMENT_KEY}}/${dev_key}/g" "$destination"
  sed -i "" -e "s/{{API_HOST_NAME}}/${api_host_name}/g" "$destination"
  sed -i "" -e "s/{{API_SSL}}/${api_ssl}/g" "$destination"
  sed -i "" -e "s/{{SOCKET_HOST_NAME}}/${socket_host_name}/g" "$destination"
  sed -i "" -e "s/{{SOCKET_PORT}}/${socket_port}/g" "$destination"
}

#######################################
# Prints the lpm logo.
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
#######################################
lpm_logo_cmd() {
cat << "EOF"
   __                        _                     ___   __   _____ 
  / /  ___  __ _ _ __  _ __ | |_   _ _ __ ___     / __\ / /   \_   \
 / /  / _ \/ _` | '_ \| '_ \| | | | | '_ ` _ \   / /   / /     / /\/
/ /__|  __/ (_| | | | | |_) | | |_| | | | | | | / /___/ /___/\/ /_  
\____/\___|\__,_|_| |_| .__/|_|\__,_|_| |_| |_| \____/\____/\____/  
                      |_|                                           
                  
                          (    (      *     
                          )\ ) )\ ) (  `    
                         (()/((()/( )\))(   
                          /(_))/(_)|(_)()\  
                         (_)) (_)) (_()((_) 
                         | |  | _ \|  \/  | 
                         | |__|  _/| |\/| | 
                         |____|_|  |_|  |_| 

EOF
}
