/**
 * Jenkins pipeline for Build and Test of Android SDK.
 */

pipeline {
    agent { label 'base-lp-agent'}
    stages {
        stage("Test this job") {
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
    sh 'Build and test job'
}
