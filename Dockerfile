FROM jangrewe/gitlab-ci-android

ARG emulator_platform=android-26
ARG system_image="system-images;${emulator_platform};google_apis;x86"
ARG platform_image="platforms;${emulator_platform}"

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  nodejs=6.11.4~dfsg-1ubuntu1 \
  build-essential \
  && rm -rf /var/lib/apt/lists/*

ENV PATH="$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin"

RUN sdkmanager emulator tools platform-tools ${platform_image} ${system_image} --verbose && \
  echo no | avdmanager create avd -n "device1" --package ${system_image} --tag google_apis
