####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

clean-local-properties:
	rm -f local.properties

GRADLE_COMMANDS:=assembleRelease testReleaseUnitTest generatePomFileForAarPublication
sdk: clean-local-properties
	gradle clean ${GRADLE_COMMANDS}

.PHONY: build

GRADLE_COMMAND:=assembleDebug testDebugUnitTest assembleRelease generatePomFileForAarPublication
gradlewTravis:
	./gradlew ${GRADLE_COMMAND}

patchReleaseBranch:
	./Tools/create-release.bash patch

releaseArtifacts: releaseBinaries releasePoms

releaseBinaries:
	gradle assembleRelease

releasePoms:
	gradle generatePomFileForAarPublication

deployArtifacts:
	./Tools/deploy.py

tagCommit:
	git tag `cat sdk-version.txt`; git push --tags

deploy: tagCommit releaseArtifacts deployArtifacts