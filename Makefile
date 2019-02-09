####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

verifyTag:
	./Tools/verifyTag.sh

tagCommit:
	git tag `cat sdk-version.txt`; git push origin `cat sdk-version.txt`

deploy: verifyTag tagCommit
