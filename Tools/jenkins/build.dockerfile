FROM jangrewe/gitlab-ci-android

################################ Tools Installation ################################
RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  build-essential \
  && rm -rf /var/lib/apt/lists/*

################################ Gradle Installation ################################
# Allow the host to use gradle cache, otherwise gradle will always download plugins & artifacts.
VOLUME ["/root/.gradle/"]
VOLUME ["/root/.android/"]

ARG GRADLE_VERSION=4.10
ARG GRADLE_ZIP=gradle-${GRADLE_VERSION}-bin.zip
RUN cd /usr/local && \
    curl -L https://services.gradle.org/distributions/${GRADLE_ZIP} -o ${GRADLE_ZIP} && \
    unzip ${GRADLE_ZIP} && \
    rm ${GRADLE_ZIP}

# Export gradle environment variables.
ENV GRADLE_HOME=/usr/local/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

########################### Android Emulator Installation ###########################
ARG emulator_platform=android-26
ARG system_image="system-images;${emulator_platform};google_apis;x86"
ARG platform_image="platforms;${emulator_platform}"

ENV PATH="$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin"

RUN sdkmanager emulator tools platform-tools ${platform_image} ${system_image} --verbose && \
  echo no | avdmanager create avd -n "device1" --package ${system_image} --tag google_apis

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

################################
CMD ["bash"]
