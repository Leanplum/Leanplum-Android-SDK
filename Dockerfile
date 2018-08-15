FROM jangrewe/gitlab-ci-android

ARG emulator_platform=android-26

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  nodejs=6.11.4~dfsg-1ubuntu1 \
  build-essential \
  && rm -rf /var/lib/apt/lists/*

ENV PATH="$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin"

WORKDIR "/leanplum"

CMD bash

RUN rm -f local.properties

RUN sdkmanager --no_https --licenses && sdkmanager --no_https emulator tools platform-tools \
  "platforms;${emulator_platform}" "system-images;${emulator_platform};google_apis;x86" --verbose && \
  echo no | avdmanager create avd -n "device1" --package "system-images;${emulator_platform};google_apis;x86" \
  --tag google_apis && emulator -verbose -avd device1 -no-skin -no-audio -no-window&

