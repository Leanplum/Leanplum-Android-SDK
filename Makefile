####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

SDK_BUILD_IMAGE:=leanplum/android-sdk-build:latest
DOCKER_RUN:=docker run \
			--tty --interactive --rm \
			--volume `pwd`:/leanplum \
			--workdir /leanplum \
			${SDK_BUILD_IMAGE}

clean-local-properties:
	rm -f local.properties

sdk: clean-local-properties
	./gradlew clean assembleDebug testDebugUnitTest --info

sdk-in-container:
	${DOCKER_RUN} make sdk

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} . -f jenkins/build.dockerfile
	docker push ${SDK_BUILD_IMAGE}

.PHONY: build
