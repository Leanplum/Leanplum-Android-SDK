/**
 * Jenkins pipeline for Build and Test of Android SDK.
 */

pipeline {
    agent { label 'base-lp-agent'}
    stages {
        stage("Build and Test the SDK") {
            steps {
                timestamps {
                    ansiColor('xterm') {
                        buildAndTest()
                        archiveFiles()
                    }
                }
            }
        }
    }
}

def buildAndTest() {
    def buildImage = docker.build(
        "leanplum/android-sdk-build",
         "-f ./Tools/jenkins/build.dockerfile .")
    buildImage.inside {
        sh 'make sdk'
    }
}

def archiveFiles() {
    publishers {
        archiveArtifacts('build/**/*.aar')
        archiveArtifacts('build/**/*.pom')
    }
}
