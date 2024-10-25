#!/usr/bin/env bash

DEMO_LOCATION=$1
DEMO_NAME=$2
APP_NAME=$3
APPLE_AUTHENTICATION_KEY_ID=$4
APPLE_AUTHENTICATION_KEY_ISSUER_ID=$5

OUTUPUT_FOLDER="$DEMO_LOCATION/out"
DEMO_ARCHIVE="$OUTUPUT_FOLDER/$DEMO_NAME.xcarchive"
INFO_PLIST_LOCATION="$DEMO_ARCHIVE/Info.plist"
EXPORT_PLIST_LOCATION="$DEMO_LOCATION/$DEMO_NAME/ExportOptions.plist"

V1_SEARCH_SENTENCE="You've already uploaded a build with build number '1' for version number '1.0'"

# Check if it is building main or some PR
if [ -z "$6" ]
then
  # Every merge to main triggers a testflight build, and gets tracked
  # against version 1.0.0. The tip of main is the highest build number
  # for version 1.0.0 (this is the "no GH_PR_NUMBER" path)
  VERSION="1.0.0"
else
  # Every push to every PR also triggers a testflight build, and gets
  # tracked against version 0.0.<pr_number>. To test a PR, install the
  # highest build for its corresponding version.
  VERSION="0.0.$6"
fi

echo "Building the app"
xcodebuild -exportArchive -archivePath $DEMO_ARCHIVE -exportPath $OUTUPUT_FOLDER -exportOptionsPlist $EXPORT_PLIST_LOCATION -allowProvisioningUpdates -allowProvisioningDeviceRegistration

echo "Invoking to validate the app version $VERSION, so we can retrieve the latest build number from testflight"
echo "Will execute command: xcrun altool --validate-app -f $OUTUPUT_FOLDER/$APP_NAME -t ios --apiKey $APPLE_AUTHENTICATION_KEY_ID --apiIssuer $APPLE_AUTHENTICATION_KEY_ISSUER_ID"

validate_app_result=$(xcrun altool --validate-app -f $OUTUPUT_FOLDER/$APP_NAME -t ios --apiKey $APPLE_AUTHENTICATION_KEY_ID --apiIssuer $APPLE_AUTHENTICATION_KEY_ISSUER_ID 2>&1)

if echo "$validate_app_result" | grep -qF "$V1_SEARCH_SENTENCE"; then
    /usr/libexec/PlistBuddy -c "Set :ApplicationProperties:CFBundleShortVersionString 1.0" "${INFO_PLIST_LOCATION}" || {
        echo 'Plist Update Failed'
        exit 1
    }
    echo "Rebuild the app with the 1.0 build version"
    xcodebuild -exportArchive -archivePath $DEMO_ARCHIVE -exportPath $OUTUPUT_FOLDER -exportOptionsPlist $EXPORT_PLIST_LOCATION -allowProvisioningUpdates -allowProvisioningDeviceRegistration
    echo "Executing to validate the app result again"
    validate_app_result=$(xcrun altool --validate-app -f $OUTUPUT_FOLDER/$APP_NAME -t ios --apiKey $APPLE_AUTHENTICATION_KEY_ID --apiIssuer $APPLE_AUTHENTICATION_KEY_ISSUER_ID 2>&1)
fi

echo "Received result: $validate_app_result"

# Extracting previousBundleVersion using grep and awk
previous_build_version=$(echo "$validate_app_result" | grep -o 'previousBundleVersion = [0-9]*' | awk '{print $3}' | tail -1)

echo "Previous build version: $previous_build_version"

# Check if previousBundleVersion exists and increment it by 1
# If it is empty set it to 1
if [ -z "$previous_build_version" ]
then
  echo "The build version is already the right one. No need to increment it."
else
  echo "Will increment build version"
  new_build_version=$(($previous_build_version + 1))
  echo "New build version: $new_build_version"
  /usr/libexec/PlistBuddy -c "Set :ApplicationProperties:CFBundleShortVersionString ${VERSION}" "${INFO_PLIST_LOCATION}" || {
      echo 'Plist Update Failed'
      exit 1
  }
  /usr/libexec/PlistBuddy -c "Set :ApplicationProperties:CFBundleVersion ${new_build_version}" "${INFO_PLIST_LOCATION}" || {
       echo 'Plist Update Failed'
       exit 1
  }
  echo "Updated ${INFO_PLIST_LOCATION} with Version Number: ${VERSION} and build number ${new_build_version}"
  echo "Rebuild the app with the right build version"
  xcodebuild -exportArchive -archivePath $DEMO_ARCHIVE -exportPath $OUTUPUT_FOLDER -exportOptionsPlist $EXPORT_PLIST_LOCATION -allowProvisioningUpdates -allowProvisioningDeviceRegistration
fi
