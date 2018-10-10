#!/usr/bin/env bash
#
# LPM | Author: Ben Marten
# Copyright (c) 2017 Leanplum Inc. All rights reserved.
#
set -eo pipefail; [[ $DEBUG ]] && set -x
# shellcheck source=plugins/_common/constants.sh
source "Tools/_common/constants.sh"
# shellcheck source=plugins/_common/functions.sh
source "Tools/_common/functions.sh"

echo "Leanplum-Android-SDK:"
leanplum_release_notes "Android/Leanplum-Android-SDK"
echo "Leanplum-Android-UIEditor:"
leanplum_release_notes "Android/Leanplum-Android-UIEditor"
# echo "Leanplum-Android-Monitoring:"
# leanplum_release_notes "Android/Leanplum-Android-Monitoring"
