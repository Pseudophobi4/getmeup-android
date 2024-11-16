# Get Me Up!

An Android alarm app designed to help users get out of bed—literally.

## Overview

**Get Me Up!** is an Android alarm app built with Kotlin, Android Views, and XML in Android Studio. It aims to help users wake up effectively by requiring them to physically move to turn off the alarm, addressing common issues like oversleeping, snoozing, or turning off alarms without fully waking up.

**Note:** This app is currently in the core functionality stage. Additional design elements and features are planned but have not yet been implemented.

## Features

### **1. Code-Based Alarm Deactivation**

- The alarm cannot be turned off without entering a pre-set deactivation code.
- The deactivation code is randomly generated and can be reset.
- Users are advised to store their codes away from the bed, as retrieving them requires getting up.
- The code is hidden in the app when the alarm is triggered, preventing users from cheating.

### **2. Volume Control Lock**

- Alarm volume can be adjusted during setup.
- Device volume controls are locked when the alarm is active, ensuring the alarm volume remains unchanged for the duration of the alarm.

### **3. 30-Second Mute (Snooze Alternative)**

- A "Mute" button provides temporary silence for up to 30 seconds.
- Users must press the button to mute the alarm. Pressing it again resets the timer.
- If the button is not pressed after 30 seconds, the alarm automatically resumes, in case the user has fallen asleep again.

### **4. Persistent Vibration**

- Alarm vibration continues even when the alarm is muted, discouraging users from abusing the mute functionality and encouraging them to start getting up.

### **5. Navigation Restriction**

- Navigating away from the deactivation screen immediately resumes the alarm sound, discouraging forced deactivation.

## Development Status

The app is in the early stages of development. While core functionality is in place, additional features, improved design, and user customization options are in progress.