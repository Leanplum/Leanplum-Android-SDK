IMAGE:=jangrewe/gitlab-ci-android

build:
	docker run --volume `pwd`:/sdk --workdir /sdk ${IMAGE} ./gradlew clean assembleDebug testDebugUnitTest

.PHONY: build
