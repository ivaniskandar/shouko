# Shouko / XPERI+ [![CI](https://github.com/ivaniskandar/shouko/actions/workflows/android-master.yml/badge.svg?branch=master&event=push)](https://github.com/ivaniskandar/shouko/actions/workflows/android-master.yml) [![Latest release](https://img.shields.io/github/v/release/ivaniskandar/shouko?label=download)](https://github.com/ivaniskandar/shouko/releases/latest)

Xpand the feature set of your Xperia! <sub>and *xperience* more</sub>

Features
----------

- **Assistant Button Override**

  This feature is available on Xperia 5 II, Xperia 10 III, Xperia 5 III, Xperia 1 III and Xperia 1 IV.

  Launches an app or a shortcut when the Assistant button is pressed. You can also disable the button completely.

  This feature needs `READ_LOGS` and `WRITE_SECURE_SETTINGS` permission granted manually with ADB or root. A guide to grant this permission is available in the app.

- **Flip to Shush**

  Turns on Do Not Disturb by placing your phone face down on a flat surface.

  Devices like the Xperia 5 II and Xperia 1 II supports always-listening which allows the gesture detection to run even when the screen is off. Otherwise, this feature will only work when the screen is on.

- **Accidental Touch Prevention**

  Shows a full-screen dialog and locks the screen when the phone screen is turned on with the proximity sensor blocked.

  Unlike the system feature, this one works even when your phone gets unlocked, so accidentally touching the fingerprint sensor when pocketing your phone will be less *disastrous*.
  
- **Android App Link Manager for Android 12+**

  See list of apps that can handle a link split into two categories of approved and unapproved apps.
  
  Approved apps will handle the approved link by default and will skip the selection dialog, while unapproved apps will not handle the opened link by default and instead will be opened in the web browser.
  
  Also includes a custom selection dialog to bring back the "Open with" dialog that used to be shown by default on older Android version.
  
- **Link Cleaner**

  Removes the unnecesary bits of a link such as tracking ID.
  
  To clean a link, share the original link to the Link Cleaner activity and the cleaned link will be reshared. You can also copy the original link to clipboard and open the Link Cleaner shortcut, the cleaned link will replace the original link in the clipboard.

- **Click to Chat**

  Start a WhatsApp chat without adding the recipient phone number to your contacts first.

  To do so, click a phone number detected by the system or launch the shortcut after copying a phone number.

- **Coffee**

  A Quick Settings tile that runs a service to keep your phone screen awake. Turning off the screen will automatically disable this feature.

- **Tea**

  A Quick Settings tile that runs a service to turn off your phone screen while keeping the CPU awake. Block the proximity sensor when this service enabled to turn off the screen.

  This feature is **NOT** a power button replacement. Your phone will be kept awake when you turn off the screen with the proximity sensor. Turning off the screen with the power button will temporarily disable this feature until the screen back on.

- **Lockscreen Shortcut Customizer**

  Customize the shortcuts in your lockscreen to launch any app you want.

  This feature needs `WRITE_SECURE_SETTINGS` permission granted manually with ADB or root. A guide to grant this permission is available in the app.

- **APK-DM Installer**

  If a given APK contains a Baseline Profile, it will be transcoded into a DexMetadata (DM) file and installed alongside the APK. This results in a best app performance possible from the first launch, without needing to wait the system to run the background dex optimizer.

Accessibility Service
----------

Some feature in this app requires accessibility service to run. Features using the accessibility service is defined [here](app/src/main/java/xyz/ivaniskandar/shouko/feature).

Device Compatibility
----------

This app features generally works on all devices unless specified otherwise. Some feature require specific hardware features to work.

Download
----------

~~Get the app on the [latest release page](https://github.com/ivaniskandar/shouko/releases/latest).~~ I don't make "release" anymore, get the canary build instead.

Canary build to try new features/fixes is also available [here](https://nightly.link/ivaniskandar/shouko/workflows/android-master/master/shouko-canary.zip).

Build it yourself
----------

You will need [Android Studio Arctic Fox](https://developer.android.com/studio) or newer to build this project.
