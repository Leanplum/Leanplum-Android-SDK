import os

SDK_VERSION_FILE = "sdk-version.txt"

def get_current_version():
    with open(SDK_VERSION_FILE, 'r') as f:
        return f.read()

def deploy(project, package, version):
    deployAAR(project, package, version)
    deployPOM(project, package, version)

def deployAAR(project, package, version):
    localAARPath = project + "/build/outputs/aar/" + project + "-release.aar"
    filename = remoteFilename(package, version, "aar")
    
    remoteAARPath = "libs-release-local/com/leanplum/" + package + "/" + version + "/" + filename
    artifactoryDeploy(localAARPath, remoteAARPath)
    
    remoteBintrayPath = "https://api.bintray.com/content/leanplum/maven/" + package + "/" + version + "/" + filename
    bintrayDeploy(localAARPath, remoteBintrayPath)

def deployPOM(project, package, version):
    localAARPath = project + "/build/publications/aar/pom-default.xml"
    filename = remoteFilename(package, version, "pom")

    remoteAARPath = "libs-release-local/com/leanplum/" + package + "/" + version + "/" + package + "-" + version + ".pom"
    artifactoryDeploy(localAARPath, remoteAARPath)
    
    remoteBintrayPath = "https://api.bintray.com/content/leanplum/maven/" + package + "/" + version + "/" + filename
    bintrayDeploy(localAARPath, remoteBintrayPath)
    

def remoteFilename(package, version, extension):
    return package + "-" + version + "." + extension

def artifactoryDeploy(source, destination):
    command = "jfrog rt u " + source + " " + destination
    print command
    # os.system(command)

def bintrayDeploy(source, destination):
    command = "curl -T " + source + " -ue7mac:<API_KEY> " + destination
    print command
    # os.system(command)
    

packages = [{
        "project": "AndroidSDKCore",
        "package": "leanplum-core"
    }, {
        "project": "AndroidSDKPush",
        "package": "leanplum-push"
    }, {
        "project": "AndroidSDKGcm",
        "package": "leanplum-gcm"
    }, {
        "project": "AndroidSDKFcm",
        "package": "leanplum-fcm"
    }, {
        "project": "AndroidSDKLocation",
        "package": "leanplum-location"
    }
]

def main():
    version = get_current_version()
    for package in packages:
        deploy(package['project'], package['package'], version)
  
if __name__== "__main__":
  main()
