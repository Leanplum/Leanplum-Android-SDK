/**
 * Jenkins pipeline for Build and Test of Android SDK.
 */

withDockerContainer(args: "", image: "leanplum/android-sdk-build") {
    sh 'make sdk'
}
