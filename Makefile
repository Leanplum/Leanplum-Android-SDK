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

GRADLE_COMMANDS:=assembleRelease testReleaseUnitTest generatePomFileForAarPublication
sdk: clean-local-properties
	gradle clean ${GRADLE_COMMANDS}

sdk-in-container:
	${DOCKER_RUN} make sdk

shell:
	${DOCKER_RUN} bash

build-image:
	docker build -t ${SDK_BUILD_IMAGE} . -f Tools/jenkins/build.dockerfile

.PHONY: build

sdkTest:
	./gradlew assembleDebug testDebugUnitTest

patchReleaseBranch:
	./Tools/create-release.bash patch

releaseBinaries:
	./gradlew assembleRelease

releasePoms:
	./gradlew generatePomFileForAarPublication

releaseArtifacts: releaseBinaries releasePoms

deployArtifacts:
	./Tools/deploy.py

tagCommit:
	git tag `cat sdk-version.txt`; git push --tags

deploy: tagCommit deployArtifacts
