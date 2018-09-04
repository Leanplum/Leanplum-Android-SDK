#!/usr/bin/env python2.7
from jinja2 import Environment, FileSystemLoader
import sys

def main():
  template_name = sys.argv[1]
  sdk_version = sys.argv[2]
  sdk_module = sys.argv[3]

  env = Environment(loader=FileSystemLoader(searchpath="./artifact-poms"))
  template = env.get_template(template_name)
  output_from_parsed_template = template.render(sdk_version=sdk_version, sdk_module=sdk_module)
  print output_from_parsed_template

  # Save the new pom in path 
  with open("{0}-{1}.pom".format(sdk_module, sdk_version), "wb") as fh:
      fh.write(output_from_parsed_template)

if __name__ == "__main__":
  main()