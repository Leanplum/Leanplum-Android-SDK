#!/usr/bin/python

import semver
import xml.etree.ElementTree as ET
import sys 

"""Type: Major/Minor/Patch"""

"""
1. Read in values.xml
2. Increment the SDK version value based on correct semVers
3. Return: new version number
"""
def get_current_version(root):
  for sdk_version in root.iter("sdk_version"):
    return sdk_version.text

def update_version(root, xml, version):
  for sdk_version in root.iter("sdk_version"):
    sdk_version.text = str(version)
    sdk_version.set('updated', 'yes')
    xml.write("AndroidSDK/res/values/strings.xml")

def patch_increment(current_version):
  return semver.bump_patch(current_version)

def minor_increment(current_version):
  return semver.bump_minor(current_version)

def major_increment(current_version):
  return semver.bump_major(current_version)

def main():
  type = sys.argv[1]
  xml = ET.parse("AndroidSDK/res/values/strings.xml")
  root = xml.getroot()

  current_version = get_current_version(root)
  
  if type == "patch":
    release_version = patch_increment(current_version)
  elif type == "minor":
    release_version = minor_increment(current_version)
  elif type == "major":
    release_version = major_increment(current_version)
  else:
    release_version = current_version
  
  update_version(root, xml, release_version)
  sys.stdout.write(release_version)

if __name__ == "__main__":
  main()
