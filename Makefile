
IMAGE:=jangrewe/gitlab-ci-android
CP_ANDROID_SDK=cp -r /sdk .
DOCKER_RUN:=docker run --volume `pwd`:/leanplum --workdir /leanplum --tty --interactive ${IMAGE}
build:
	${DOCKER_RUN} ${CP_ANDROID_SDK}
	${DOCKER_RUN} ./gradlew clean assembleDebug testDebugUnitTest

shell:
	${DOCKER_RUN} bash

 .PHONY: build
