# building
xcodebuild clean build -workspace './OpenSesame.xcworkspace' -scheme 'OpenSesame' -destination 'generic/platform=iOS' -showBuildTimingSummary -allowProvisioningUpdates | tee ./xcodebuild.log | xcbeautify
# fixing lint
swiftlint analyze --config "./.swiftlint.yml" --reporter "xcode" --compiler-log-path ./xcodebuild.log ./ --fix
