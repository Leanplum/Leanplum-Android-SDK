####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

SDK_BUILD_IMAGE:=leanplum/android-sdk-build:latest
EMULATOR=$ANDROID_HOME/tools/emulator
DOCKER_RUN:=docker run \
			--tty --interactive --rm \
			--volume `pwd`:/leanplum \
			--workdir /leanplum \
			${SDK_BUILD_IMAGE}

clean-local-properties:
	rm -f local.properties

sdk: clean-local-properties
	${EMULATOR} -no-accel -verbose -avd device1 -no-skin -no-audio -no-window&
	./gradlew clean assembleDebug testDebugUnitTest --info

sdk-in-container:
	${DOCKER_RUN} make sdk

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} .
	docker push ${SDK_BUILD_IMAGE}

.PHONY: build
