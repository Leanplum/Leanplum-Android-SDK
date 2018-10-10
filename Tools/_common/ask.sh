#!/usr/bin/env bash
set -o nounset
set -o xtrace
set -o pipefail
set -o errexit

#######################################
# Asks the user a question.
# Globals:
#   None
# Arguments:
#   The question to ask.
#   The default value.
# Returns:
#   None
#######################################
ask() {
  while true; do
    if [ "${2:-}" = "Y" ]; then
      prompt="Y/n"
      default=Y
    elif [ "${2:-}" = "N" ]; then
      prompt="y/N"
      default=N
    else
      prompt="y/n"
      default=
    fi

    # Ask the question (not using "read -p" as it uses stderr not stdout).
    printf "\\n%s" "$1 [$prompt] "

    # Read the answer (use /dev/tty in case stdin is redirected from somewhere else).
    read -r reply </dev/tty

    # Default?
    if [ -z "$reply" ]; then
        reply=$default
    fi

    # Check if the reply is valid.
    case "$reply" in
        Y*|y*) return 0 ;;
        N*|n*) return 1 ;;
    esac
  done
}
