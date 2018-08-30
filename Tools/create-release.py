#!/usr/bin/env python2.7

import semver
import xml.etree.ElementTree as ET
import sys 

ANDROID_STRINGS_XML = "AndroidSDK/res/values/strings.xml"

def get_current_version(root):
  for element in root.iter("string"):
    if element.attrib.get("name") == "sdk_version":
      return element.text

def update_version(root, xml, version):
  for element in root.iter("string"):
    if element.attrib.get("name") == "sdk_version":
      element.text = str(version)
      xml.write(ANDROID_STRINGS_XML, encoding='utf-8', xml_declaration=True)

"""
1. Read in values.xml
2. Increment the SDK version value based on correct semVers
3. Return: new version number
"""
def main():
  release_type = sys.argv[1]
  alpha_build = sys.argv[2]
  xml = ET.parse(ANDROID_STRINGS_XML)
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

  if alpha_build == "alpha":
    release_version = semver.bump_prerelease(current_version, "alpha")
  elif alpha_build == "release":
    pass
  else:
    raise Exception("Please use alpha opt for pre release build")
  
  
  
  update_version(root, xml, release_version)
  sys.stdout.write(release_version)

if __name__ == "__main__":
  main()
