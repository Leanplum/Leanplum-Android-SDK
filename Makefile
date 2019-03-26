####################################################################
#
# Rules used to build and release the SDK.
#
####################################################################

tagCommit:
	git tag `cat sdk-version.txt`; git push origin `cat sdk-version.txt`

deploy: tagCommit
