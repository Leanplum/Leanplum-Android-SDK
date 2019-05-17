![Leanplum](Leanplum.svg)

<p align="center">
    <img src='https://img.shields.io/badge/branch-master-blue.svg'>
    <img src='https://jenkins.leanplum.com/buildStatus/icon?job=android-sdk-master' alt="Build status">
    &nbsp;&nbsp;&nbsp;&nbsp;
    <img src='https://img.shields.io/badge/branch-develop-red.svg'>
    <img src='https://jenkins.leanplum.com/buildStatus/icon?job=android-sdk-develop' alt="Build status">
</p>
<p align="center">
    <a href="https://github.com/Leanplum/Leanplum-Android-SDK/master/LICENSE">
    <img src="https://img.shields.io/badge/license-apache%202.0-blue.svg?style=flat" alt="License: Apache 2.0" /></a> 
</p>

## Installation & Usage
Please refer to: https://www.leanplum.com/docs/android/setup
## Development Workflow
- We use feature branches that get merged to `master`.
## Build the SDK
To build the sdk run:
```bash
./gradlew assembleRelease
```
## Contributing
Please follow the Conventional Changelog Commit Style and send a pull request to `master` branch.
## License
See LICENSE file.
## Support
Leanplum does not support custom modifications to the SDK, without an approved pull request (PR). If you wish to include your changes, please fork the repo and send a PR to the develop branch. After the PR has been reviewed and merged into develop, it will go into our regular release cycle, which includes QA. Once the release process has finished, the PR will be available in master and your changes are now officialy supported by Leanplum.
