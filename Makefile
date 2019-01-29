####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

testSDK:
  ./gradlew assembleRelease testReleaseUnitTest

patchReleaseBranch:
  ./Tools/create-release.bash patch

releaseBinaries:
  ./gradlew assembleRelease

releasePoms:
  ./gradlew generatePomFileForAarPublication

releaseArtifacts: releaseBinaries releasePoms

tagCommit:
  git tag `cat sdk-version.txt`; git push origin `cat sdk-version.txt`

deploy: tagCommit
