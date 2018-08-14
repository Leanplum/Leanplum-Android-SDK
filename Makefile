
SDK_BUILD_IMAGE:=leanplum/android-sdk-build:latest
DOCKER_RUN:=docker run \
			--tty --interactive --rm \
			--volume `pwd`:/leanplum \
			--workdir /leanplum \
			${SDK_BUILD_IMAGE}

clean-local-properties:
	rm -f local.properties

sdk-local: clean-local-properties
	./gradlew clean assembleDebug testDebugUnitTest --info

sdk-in-container: clean-local-properties
	${DOCKER_RUN} make sdk-local

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} .
	docker push ${SDK_BUILD_IMAGE}

.PHONY: build
