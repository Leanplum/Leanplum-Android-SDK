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
	emulator -verbose -avd device1 -no-skin -no-audio -no-window&
	sleep 30
	./gradlew clean assembleDebug testDebugUnitTest --info

sdk-in-container:
	${DOCKER_RUN} make sdk

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} .
	docker push ${SDK_BUILD_IMAGE}

.PHONY: build
