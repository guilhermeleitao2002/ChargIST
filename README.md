# ChargIST

An Android application for electric vehicle charging station management.

## Features

All of the mandatory features specified for the project checkpoint were implemented.
Additionally, the extra components "User Accounts" and "Social Sharing" were also implemented, allowing users to create accounts, log in, and share their charging stations with others.

## Prerequisites

- Android Studio
- Android SDK API 24+ (Android 7.0)
- Google Maps API key
- Firebase project setup

## Installation

1. **Open Android Studio**

2. **Click Open** to open a new project **-> Select the folder with the project's contents**

3. To enable ease of usability, we purposefully left the API Keys - for the **Google Maps SDK**, **Places**, and **Firestore** - hardcoded in the project so that the staff could just run the application point blank.

4. **Build and Run**
   - Open the project
   - Wait for Gradle sync to complete
   - Connect an Android device or start an emulator device
   - Click Run

## Usage

1. **First Launch**: Grant location permissions when prompted
2. **User Account**: Create an account and log in, or use the app without signing in (but then you won't have access to the Favorites functionality)
3. **Browse Stations**: View charging stations on the map, tap markers for details
4. **Add Stations**: Use the floating action button to add new charging stations
5. **Search**: Use the search function to filter and sort stations by various criteria
6. **Change Details**: tap any given Charging Slot to change its speed and connector type information

## Project Structure

```
app/src/main/java/pt/ist/cmu/chargist/
├── data/
│   ├── model/          # Data classes (Charger, ChargingSlot, etc.)
│   └── repository/     # Repository implementations (Firebase, local cache)
├── di/                 # Dependency injection modules
├── ui/
│   ├── navigation/     # Navigation graph
│   ├── screens/        # Composable screens
│   └── viewmodel/      # ViewModels for MVVM architecture
└── util/               # Utility classes and helpers
```

## Dependencies

Key dependencies include:
- Jetpack Compose BOM
- Firebase SDK
- Google Maps & Places
- Koin for DI
- Room for local storage
- Retrofit for networking
