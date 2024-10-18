#!/usr/bin/env bash

if [[ $# -lt 1 ]]
then
  echo "Usage: $(basename "${BASH_SOURCE}") XCARCHIVE [GH_PR_NUMBER]"
  exit 1
fi

INFO_PLIST_LOCATION="$1/Info.plist"


if [ -z "$2" ]
then
  # Every merge to main triggers a testflight build, and gets tracked
  # against version 1.0.0. The tip of main is the highest build number
  # for version 1.0.0 (this is the "no GH_PR_NUMBER" path)
  VERSION="1.0.0"
else
  # Every push to every PR also triggers a testflight build, and gets
  # tracked against version 0.0.<pr_number>. To test a PR, install the
  # highest build for its corresponding version.
  VERSION="0.0.$2"
fi

echo "Setting Version... 'VERSION'"

/usr/libexec/PlistBuddy -c "Set :ApplicationProperties:CFBundleShortVersionString ${VERSION}" "${INFO_PLIST_LOCATION}" || {
    echo 'Plist Update Failed'
    exit 1
}

echo "Updated ${INFO_PLIST_LOCATION} with Version Number: ${VERSION}"
