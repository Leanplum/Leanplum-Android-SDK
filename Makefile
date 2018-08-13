
IMAGE:=jangrewe/gitlab-ci-android
CP_ANDROID_SDK=cp -r /sdk .
DOCKER_RUN:=docker run  --tty --interactive --rm --volume `pwd`:/leanplum --workdir /leanplum ${IMAGE}
build:
	rm -f local.properties
	${DOCKER_RUN} ./gradlew clean assembleDebug testDebugUnitTest

shell:
	${DOCKER_RUN} bash

.PHONY: build
