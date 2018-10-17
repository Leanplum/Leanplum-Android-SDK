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
    archiveArtifacts {
        pattern('build/**/*.aar')
        pattern('build/**/*.pom')
        onlyIfSuccessful()
    }
}
