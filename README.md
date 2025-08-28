AI Based Exercise Application

## Overview
Gymnify is an Android application (Kotlin) that helps users perform and track exercises using AI-powered analysis. The app aims to provide real-time guidance, count repetitions, and give feedback on form to help users exercise safely and effectively.

> Note: This README is a template and overview. Update sections marked with [TODO] with project-specific details (models, SDK versions, screenshots, etc.).

## Features
- AI-based exercise detection and guidance
- Repetition counting and workout tracking
- Session history and basic progress metrics
- On-device inference (or cloud—configure per project)
- Camera-based pose estimation and form feedback
- Simple, Kotlin-first Android codebase

## Getting Started

### Prerequisites
- Android Studio (recommended latest stable)
- JDK 11+ (or the version required by the project)
- Android SDK (install via Android Studio)
- A connected Android device or emulator

### Clone the repository
git clone https://github.com/HassanRazaSadiq/Gymnify-App.git
cd Gymnify-App

### Open and build
1. Open the project in Android Studio: File → Open → select the project directory.
2. Let Gradle sync and download dependencies.
3. Build and run on an emulator or device:
   - Run > Run 'app'
   - Or use command line: ./gradlew assembleDebug

### Permissions
The app requires runtime permissions for:
- CAMERA (for exercise detection)
- optionally, WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE (if you save media)

Make sure to handle runtime permission requests in the app.

## Configuration
- Model files / assets: If the app uses an on-device model (e.g., TensorFlow Lite), place model files in `app/src/main/assets/` or follow the project's asset loading approach.
- API keys: If using a cloud API (pose estimation / ML), add keys securely (do not commit secrets). Use local properties or Android Gradle properties:
  - Example: add to `local.properties` or `gradle.properties` (not committed)
- Build flavors / Gradle settings: adjust `minSdkVersion`, `targetSdkVersion`, and dependencies as needed.

## Project Structure
- app/ — Android application module (Kotlin)
- app/src/main/java/ — Kotlin source files
- app/src/main/res/ — resources (layouts, drawables, strings)
- app/src/main/assets/ — models and static assets (if used)
- README.md — this file

## Development Notes
- Follow Kotlin coding conventions and Android best practices.
- Prefer on-device ML for privacy and low latency; if using cloud inference, ensure secure key storage.
- Unit tests and instrumentation tests should be added for critical logic (rep counting, pose evaluation).

## Contributing
Contributions are welcome!
- Fork the repository
- Create a branch: git checkout -b feature/your-feature
- Make changes and commit with clear messages
- Open a Pull Request describing the change


## Roadmap / Ideas
- More exercise types and improved pose feedback
- Personalized training plans
- Exercise analytics with charts and progress trends
- Export/import workout history

## Troubleshooting
- Gradle sync issues: try File → Sync Project with Gradle Files, or run ./gradlew clean
- Emulator camera not working: test on a physical device for camera-based features

## Contact
Maintainer: @HassanRazaSadiq  
Repo: https://github.com/HassanRazaSadiq/Gymnify-App

