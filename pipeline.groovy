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
    withDockerContainer(args: "", image: "jangrewe/gitlab-ci-android") {
        sh 'rm -f local.properties'
        sh './gradlew clean assembleDebug testDebugUnitTest --info'
    }
}