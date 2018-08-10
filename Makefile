
IMAGE:=jangrewe/gitlab-ci-android
DOCKER_RUN:=docker run --volume `pwd`:/leanplum --workdir /leanplum --tty --interactive ${IMAGE}
build:
	${DOCKER_RUN} ./gradlew clean assembleDebug testDebugUnitTest

shell:
	${DOCKER_RUN} bash

.PHONY: build
