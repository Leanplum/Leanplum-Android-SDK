####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

testSDK:
	./gradlew clean assembleRelease testReleaseUnitTest

patchReleaseBranch:
	./Tools/create-release.bash patch

releaseBinaries:
	./gradlew assembleRelease makeJar

releasePoms:
	./gradlew generatePomFileForAarPublication

releaseArtifacts: releaseBinaries releasePoms

deployArtifacts: 
	./gradlew artifactoryPublish bintrayUpload

tagCommit:
	git tag `cat sdk-version.txt`; git push origin `cat sdk-version.txt`

deploy: tagCommit
