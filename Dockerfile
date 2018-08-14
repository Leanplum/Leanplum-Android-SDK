FROM jangrewe/gitlab-ci-android

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  nodejs \
  build-essential \
  && rm -rf /var/lib/apt/lists/*

ENV PATH="$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin"

RUN echo $PATH

WORKDIR "/leanplum"

CMD bash

RUN rm -f local.properties

RUN sdkmanager --list

RUN sdkmanager --no_https --licenses

RUN sdkmanager --no_https emulator tools platform-tools "platforms;android-26" "system-images;android-26;google_apis;x86" --verbose
RUN echo "Creating an Emulator"
RUN echo no | avdmanager create avd -n "x86" --package "system-images;android-26;google_apis;x86" --tag google_apis

# Needed for adb to work
EXPOSE 5037 5554 5555

