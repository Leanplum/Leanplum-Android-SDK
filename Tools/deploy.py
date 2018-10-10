import sys
import os

def deploy(project, destination, version):
    deployAAR(project, destination, version)
    deployPOM(project, destination, version)

def deployAAR(project, destination, version):
    localAARPath = "../" + project + "/build/outputs/aar/" + project + "-release.aar"
    remoteAARPath = "libs-release-local/com/leanplum/" + destination + "/" + version + "/" + destination + "-" + version + ".aar"
    jfrog(localAARPath, remoteAARPath)

def deployPOM(project, destination, version):
    localAARPath = "../" + project + "/build/publications/aar/pom-default.xml"
    remoteAARPath = "libs-release-local/com/leanplum/" + destination + "/" + version + "/" + destination + "-" + version + ".pom"
    jfrog(localAARPath, remoteAARPath)

def jfrog(source, destination):
    command = "jfrog rt u " + source + " " + destination
    # print command
    os.system(c)

packages = [{
        "project": "AndroidSDKCore",
        "destination": "leanplum-core"
    }, {
        "project": "AndroidSDKPush",
        "destination": "leanplum-push"
    }, {
        "project": "AndroidSDKGcm",
        "destination": "leanplum-gcm"
    }, {
        "project": "AndroidSDKFcm",
        "destination": "leanplum-fcm"
    }, {
        "project": "AndroidSDKLocation",
        "destination": "leanplum-location"
    }
]

def main():
    version = sys.argv[1]
    for package in packages:
        deploy(package['project'], package['destination'], version)
  
if __name__== "__main__":
  main()

