#!/usr/bin/python
import os
import datetime
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
    xml.write("values.xml")

def patch_release(current_version):
  return semver.bump_patch(current_version)

def minor_release(current_version):
  return semver.bump_minor(current_version)

def major_release(current_version):
  return semver.bump_major(current_version)

def main(argv):
  type = sys.argv[1]
  xml = ET.parse("values.xml")
  root = xml.getroot()

  current_version = get_current_version(root)
  
  if type == "patch":
    release_ver = patch_release(current_version)
    update_version(root, xml, release_ver)
    return release_ver
  elif type == "minor":
    release_ver = minor_release(current_version)
    update_version(root, xml, release_ver)
    return release_ver
  elif type == "major":
    release_ver = major_release(current_version)
    update_version(root, xml, release_ver)
    return release_ver
  
  

if __name__ == "__main__":
  main(sys.argv[1:])

  """we also have semver.prerelease which does --> rc """







