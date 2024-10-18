# Open Sesame iOS

iOS client for the Open Sesame server.

## Prerequisites

- Install [Xcode 15](https://developer.apple.com/xcode/), and set up your device [to run your own applications](https://developer.apple.com/documentation/xcode/distributing-your-app-to-registered-devices).

## Running locally

1. Clone this repository locally.
2. Install the pod dependencies.
3. Open the OpenSesame.xcworkspace in Xcode.
4. Tell Xcode to update its Package Dependencies by clicking File -> Packages -> Update to Latest Package Versions.
5. Build the project.
6. Run the project on your device.
7. Connect to the URL you are testing, and to see it work.

## Extra features

On iOS, you can enable an additional feature that allows the app to connect to the voice client automatically using a wake word mechanism. 

To enable the wake word, you need to turn it on in the settings. More details are shown in the image below:

<img src="./docsAssets/wakeWord.jpeg" alt="Alt text" height="500">

To trigger the wake word, we use a third-party library, [Porcupine](https://picovoice.ai/platform/porcupine/). 
You can test it for free by signing up at https://console.picovoice.ai/signup and retrieving your API key. 
Then, set the key in the `Picovoice` field, as shown in the image above.

Once enabled and the key is properly configured, you can start voice mode by saying: `Open Sesame`.
- The app will exit voice mode after 8 seconds of inactivity with the bot.

> **Note:** It will work even if the screen is locked, as long as the app is running in the background.

## Contributing and feedback

We are welcoming contributions to this project in form of issues and pull request. For questions about RTVI head over to the [Pipecat discord server](https://discord.gg/pipecat) and check the [#rtvi](https://discord.com/channels/1239284677165056021/1265086477964935218) channel.
