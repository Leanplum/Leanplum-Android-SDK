#!/bin/bash
version=`cat sdk-version.txt`

body="{
\"request\": {
\"branch\":\"master\",
\"message\" : \"Building Android SDK $version\",
 \"config\": {
   \"env\": {
     \"LEANPLUM_ANDROID_SDK_VERSION\": \"$version\"
   }
  }
}}"

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $TRAVIS_TOKEN" \
   -d "$body" \
   https://api.travis-ci.com/repo/Leanplum%2FLeanplum-ReactNative-SDK/requests
