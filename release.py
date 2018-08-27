#!/usr/bin/env python2.7

import semver
import xml.etree.ElementTree as ET
import sys 

"""Type: Major/Minor/Patch"""

"""
1. Read in values.xml
2. Increment the SDK version value based on correct semVers
3. Return: new version number
"""
android_strings_xml = "AndroidSDK/res/values/strings.xml"

def get_current_version(root):
  for element in root.iter("string"):
    if element.attrib.get("name") == "sdk_version":
      return element.text

def update_version(root, xml, version):
  for element in root.iter("string"):
    if element.attrib.get("name") == "sdk_version":
      element.text = str(version)
      xml.write(android_strings_xml)

def main():
  release_type = sys.argv[1]
  xml = ET.parse(android_strings_xml)
  root = xml.getroot()

  current_version = get_current_version(root)

  if release_type == "patch":
    release_version = semver.bump_patch(current_version)
  elif release_type == "minor":
    release_version = semver.bump_minor(current_version)
  elif release_type == "major":
    release_version = semver.bump_major(current_version)
  else:
    raise Exception("Please pick one patch/minor/major")
  
  update_version(root, xml, release_version)
  sys.stdout.write(release_version)

if __name__ == "__main__":
  main()
