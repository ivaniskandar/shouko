# Shouko / XPERI+ [![CI](https://github.com/ivaniskandar/shouko/actions/workflows/android-master.yml/badge.svg?branch=master&event=push)](https://github.com/ivaniskandar/shouko/actions/workflows/android-master.yml)

Xpand the feature set of your Xperia! <sub>and *xperience* more</sub>

Features
----------

- **Xperia 5 II Assistant Button Override**

  Launches an app or a shortcut when the Assistant button is pressed.

  This feature needs `READ_LOGS` permission granted manually with ADB or root. A guide to grant this permission is available in the app.

- **Flip to Shush**

  Turns on Do Not Disturb by placing your phone face down on a flat surface.

  Devices like the Xperia 5 II and Xperia 1 II supports always-listening which allows the gesture detection to run even when the screen is off. Otherwise, this feature will only work when the screen is on.

- **Accidental Touch Prevention**

  Shows a full-screen dialog and locks the screen when the phone screen is turned on with the proximity sensor blocked.

  Unlike the system feature, this one works even when your phone gets unlocked, so accidentally touching the fingerprint sensor when pocketing your phone will be less *disastrous*.

- **Coffee**

  A Quick Settings tile that runs a service to keep your phone screen awake. Turning off the screen will automatically disable this feature.

- **Tea**

  A Quick Settings tile that runs a service to turn off your phone screen while keeping the CPU awake. Block the proximity sensor when this service enabled to turn off the screen.

  This feature is **NOT** a power button replacement. Your phone will be kept awake when you turn off the screen with the proximity sensor. Turning off the screen with the power button will temporarily disable this feature until the screen back on.

Accessibility Service
----------

All features except the Quick Settings tiles need accessibility service to run. Features using the accessibility service is defined [here](app/src/main/java/xyz/ivaniskandar/shouko/feature).

Device Compatibility
----------

This app is mainly tested on the **Xperia 5 II** running **stock Android 11** firmware. Some features need specific hardware features to work, but other than that it should work properly on other devices.

Download
----------

Get the app on the [latest release page](https://github.com/ivaniskandar/shouko/releases/latest).

Canary build to try new features/fixes is also available [here](https://github.com/ivaniskandar/shouko/actions/workflows/android-master.yml), but any issue reported with this build will not be accepted.

Build it yourself
----------

Currently, you can only use the latest Canary of [Android Studio Arctic Fox](https://developer.android.com/studio/preview) to build this project since Jetpack Compose is used to *compose* the UI.