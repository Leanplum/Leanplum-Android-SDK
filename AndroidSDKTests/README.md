Snapshot testing
------------

This file shows how to execute and record the snapshot tests (macOS).

Tool used for snapshot testing is [screenshot-tests-for-android](https://facebook.github.io/screenshot-tests-for-android/).

Prerequisites
------------

#### Install Pillow (python library for images):

    $ sudo -H python -m ensurepip
    $ python -m pip install Pillow --user

#### Install and run emulator:

    $ $ANDROID_HOME/tools/bin/sdkmanager "system-images;android-22;default;x86"
    $ echo no | $ANDROID_HOME/tools/bin/avdmanager create avd --force -n test -k "system-images;android-22;default;x86" -c 100M
    $ $ANDROID_HOME/emulator/emulator -verbose -avd test -no-snapshot -no-audio -camera-back none -camera-front none -selinux permissive -qemu -m 2048 &

Usage
------------
#### Executing tests

    $ ./gradlew verifyDebugAndroidTestScreenshotTest

If tests are failing compare snapshots:
- your snapshots - AndroidSDKTests/build/screenshotsDebugAndroidTest
- reference snapshots - AndroidSDKTests/screenshots

You could use [ImageMagick](https://imagemagick.org/).

    $ brew install imagemagick
    $ magick left.png right.png difference.png

#### Recording tests

    $ ./gradlew recordDebugAndroidTestScreenshotTest

Commit newly created PNG files located in AndroidSDKTests/screenshots.
