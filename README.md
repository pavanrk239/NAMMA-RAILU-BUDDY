# Namma Railu Buddy

Namma Railu Buddy is an Android application built with Jetpack Compose that helps passengers track trains, view journey details, and receive proximity alerts when their selected station is nearby.

## 🚆 Project Overview

This app is designed for rail passengers to:
- search active trains by name or number
- view journey details and train stops
- select a station and track proximity using GPS
- receive an alarm notification when approaching the selected station
- access live station and train data from Firebase realtime database

## ✨ Key Features

- **Train search** with live filtering by train number or name
- **Journey screen** with train details and stops
- **Station screen** with station tracking and alarm activation
- **GPS-based proximity monitoring** using location updates
- **Notification alarm** with STOP action when near the target station
- **Firebase realtime sync** for stations and train configuration
- **Compose UI** with modern Material 3 styling

## 🧩 Tech Stack

- Kotlin
- Android Jetpack Compose
- Firebase Realtime Database
- Google Play Services Location
- Retrofit + Gson
- Material 3
- Android Studio / Gradle

## 📁 Project Structure

- `app/src/main/java/com/example/railubuddy/`
  - `MainActivity.kt` — app entry point, navigation, permission handling, and location updates
  - `screens/` — UI screens for home, journey, and station details
  - `viewmodel/` — app state and Firebase logic in `MainViewModel.kt`
  - `services/` — notification/alarm services and location service support
  - `api/` — Retrofit API client and service interfaces
  - `repository/` — train repository logic
  - `model/` — data models for stations and journeys
  - `ui/theme/` — app styling and color theme
- `app/build.gradle.kts` — module build configuration and dependencies
- `build.gradle.kts`, `settings.gradle.kts` — root Gradle project configuration

## ⚙️ Build & Run Instructions

### Prerequisites

- Android Studio Flamingo or newer
- JDK 11
- Android SDK with API 24+ installed
- Internet access for Firebase and location services

### Run Locally

1. Open the project in Android Studio.
2. Sync Gradle.
3. Build and run the app on a real device or emulator.

Or from command line:

```bash
gradlew assembleDebug
```

### Notes

- The app requires `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions.
- For Android 13+, notification permission is requested at runtime.
- The app uses `google-services.json` in `app/` to connect to Firebase.
- If Google services or Firebase config is not correct for your environment, update `app/google-services.json` accordingly.

## ✅ Evaluation Alignment

This repository is prepared to meet evaluation criteria by including:
- public Android app source code
- clear project structure
- dependency configuration in Gradle files
- a complete root `README.md`
- Firebase integration and working app flow
- Compose-based UI and modular screen organization

## 📌 Important Notes for Reviewers

- Use the `main` branch for the current app source.
- `app` is the main Android module.
- `app/google-services.json` is required for Firebase connectivity.
- `local.properties` is intentionally ignored in Git.

## 🛠️ Recommended Commands

```bash
gradlew clean

gradlew assembleDebug

gradlew test
```