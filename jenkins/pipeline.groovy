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
    withDockerContainer(args: "", image: "leanplum/android-sdk-build") {
        sh 'make sdk-local'
    }
}
