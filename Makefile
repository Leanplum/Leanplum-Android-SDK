
IMAGE:=jangrewe/gitlab-ci-android
DOCKER_RUN:=docker run  --tty --interactive --rm --volume `pwd`:/leanplum --workdir /leanplum ${IMAGE}
build:
	rm -f local.properties
	${DOCKER_RUN} ./gradlew clean assembleDebug testDebugUnitTest --info

shell:
	${DOCKER_RUN} bash

.PHONY: build
 