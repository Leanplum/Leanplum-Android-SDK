#!/usr/bin/env python2.7

import semver
import sys 

SDK_VERSION_FILE = "sdk-version.txt"

def get_current_version():
    with open(SDK_VERSION_FILE, 'r') as f:
        return f.read()

def update_version(version):
    with open(SDK_VERSION_FILE, 'w') as f:
        f.write(version)


"""
1. Read in values.xml
2. Increment the SDK version value based on correct semVers
3. Return: new version number
"""
def main():
  release_type = sys.argv[1]

  current_version = get_current_version()

  if release_type == "patch":
    release_version = semver.bump_patch(current_version)
  elif release_type == "minor":
    release_version = semver.bump_minor(current_version)
  elif release_type == "major":
    release_version = semver.bump_major(current_version)
  else:
    raise Exception("Please pick one patch/minor/major")
  
  update_version(release_version)
  sys.stdout.write(release_version)

if __name__ == "__main__":
  main()
