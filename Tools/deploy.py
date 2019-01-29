#!/usr/bin/env python2.7

import os

SDK_VERSION_FILE = "sdk-version.txt"


def get_current_version():
    with open(SDK_VERSION_FILE, 'r') as f:
        return f.read().strip()

VERSION = get_current_version()


class Artifact:

    def __init__(self, path, extension):
        self.path = path
        self.extension = extension


class Package:

    def __init__(self, project, package):
        self.project = project
        self.package = package
        self.artifacts = [
            Artifact(
                "/build/intermediates/packaged-classes/release/classes.jar", "jar"),
            Artifact(
                "/build/publications/aar/pom-default.xml", "pom"),
            Artifact("/build/outputs/aar/" +
                    project + ".aar", "aar"),
        ]

    def validate(self):
        paths = [artifact.path for artifact in self.artifacts]
        valid = True
        for path in paths:
            filePath = self.project + path
            if not os.path.isfile(filePath):
                valid = False
                print "Critical artifact missing: " + filePath

        if not valid:
            system.exit(1)

    def deploy(self):
        self.validate()

        for artifact in self.artifacts:
            self.deployArtifact(artifact.path, artifact.extension)

    def deployArtifact(self, localPath, extension):
        location = self.project + localPath
        remotePathBase = self.remotePath(extension)
        artifactoryPath = "libs-release-local/" + remotePathBase
        bintrayPath = "https://api.bintray.com/content/leanplum/maven/" + \
            self.package + "/" + remotePathBase
        deployArtifacts(location, artifactoryPath, bintrayPath)

    def remotePath(self, extension):
        path = "com/leanplum/" + self.package + "/" + VERSION + \
            "/" + self.remoteFilename(extension)
        return path

    def remoteFilename(self, extension):
        return self.package + "-" + VERSION + "." + extension


def deployArtifacts(localPath, artifactoryPath, bintrayPath):
    artifactoryDeploy(localPath, artifactoryPath)
    bintrayDeploy(localPath, bintrayPath)


def artifactoryDeploy(source, destination):
    flags = "--url=https://artifactory.leanplum.com --apikey=" + os.environ['JFROG_CLI_API_KEY']
    command = "jfrog rt u " + source + " " + destination + " " + flags
    # print command
    os.system(command)


def bintrayDeploy(source, destination):
    command = "curl -T " + source + " -ue7mac:" + "API_KEY " + destination
    # print command
    # os.system(command)


packages = [
    Package("AndroidSDKCore", "leanplum-core"),
    Package("AndroidSDKPush", "leanplum-push"),
    Package("AndroidSDKGcm", "leanplum-gcm"),
    Package("AndroidSDKFcm", "leanplum-fcm"),
    Package("AndroidSDKLocation", "leanplum-location"),
]


def main():
    for package in packages:
        package.deploy()

if __name__ == "__main__":
    main()