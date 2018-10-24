####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

SDK_BUILD_IMAGE:=leanplum/android-sdk-build:latest
DOCKER_RUN:=docker run \
			--tty --interactive --rm \
			--volume `pwd`/..:/leanplum \
			--env JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
			--env DEBUG=1 \
			--workdir /leanplum/Leanplum-Android-SDK \
			${SDK_BUILD_IMAGE}

clean-local-properties:
	rm -f local.properties

sdk: clean-local-properties
	gradle clean assembleDebug testDebugUnitTest --info

sdk-in-container:
	${DOCKER_RUN} make sdk

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} . -f Tools/jenkins/build.dockerfile
	docker push ${SDK_BUILD_IMAGE}

# Step 1 from montly release
patchReleaseBranch:
	./Tools/create-release.bash patch

# Hiddent step 2 - jenkins
buildNtest:
	Tools/configure.sh --app-id=app_BWTRIgOs0OoevDfSsBtabRiGffu5wOFU3mkxIxA7NBs \
	--prod-key=prod_A1c7DfHO6XTo2BRwzhkkXKFJ6oaPtoMnRA9xpPSlx74 \
	--dev-key=dev_Bx8i3Bbz1OJBTBAu63NIifr3UwWqUBU5OhHtywo58RY
	${DOCKER_RUN} gradle clean assembleDebug testDebugUnitTest

clean:
	${DOCKER_RUN} gradle clean

releaseArtifacts:
	${DOCKER_RUN} gradle assembleRelease
	${DOCKER_RUN} gradle generatePomFileForAarPublication

deploy:
	./Tools/deploy.py
