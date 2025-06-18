# 🛡️ GateKeep

> *A mindful digital wellbeing app that gently interrupts your app usage with breathing exercises and reflection prompts*

<div align="center">

# 🛡️ **GateKeep**
<!-- Logo will appear here once you add gatekeep-logo.png to the assets folder -->
<img src="Logo.png" alt="GateKeep Logo" width="200"/>
</div>

## 🧠 Research-Backed Approach

**Summary**: Although no study has tested "one deep breath on Instagram launch" exactly, converging evidence shows that brief mindfulness or breath breaks can break compulsive scrolling. "Pause-and-breathe" interventions reduce actual social-media use (one sec app halved usage), likely by jolting users into awareness. More broadly, mindfulness training and breath exercises improve attention, self-control and mood, which should make it easier to resist automatic doomscrolling. Taken together, the research supports the idea that a deliberate breath pause at app-opening – a momentary mindful break – can curb impulsive social-media use and reduce anxiety in the moment.

## 📖 Overview

GateKeep is a mindful digital wellbeing application designed to help users develop healthier relationships with their mobile devices. Instead of completely blocking apps, GateKeep creates gentle interruptions that encourage self-reflection and mindfulness when opening selected applications.

### 🎯 Core Philosophy

- **Mindful Intervention**: Rather than forcefully blocking apps, GateKeep provides gentle, mindful interruptions
- **Self-Reflection**: Encourages users to pause and reflect on their intentions before using apps
- **Breathing Exercises**: Incorporates calming breathing animations to reduce impulsive app usage
- **Journaling**: Provides space for users to document their thoughts and feelings


## 🚀 Technical Architecture

### Technology Stack
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose + XML layouts (hybrid approach)
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Async Operations**: Kotlin Coroutines + Flow
- **Navigation**: Navigation Component
- **Charts**: Vico Charts + MPAndroidChart

### Features Walkthrough

#### 🏠 Home Screen
- View daily usage statistics
- See your most gatekept apps
- Quick access to app management
- Motivational quotes and insights

#### 📱 Apps Screen
- Browse all installed apps
- Toggle gatekeeping for specific apps
- Search and filter functionality
- See which apps are currently monitored

#### 📊 Stats Screen
- Detailed usage analytics
- Visual charts and trends
- Progress tracking over time
- Insights into usage patterns

### Key Components

#### 🔧 Core Services
- **AppMonitoringService**: Accessibility service for app launch detection
- **OverlayService**: Manages system overlay for mindful interruptions
- **AppRepository**: Centralized data management with Room database

#### 🎯 Data Management
```kotlin
@Entity(tableName = "gatekeep_apps")
data class GateKeepApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0
)
```

### 📁 Project Structure
```
app/src/main/java/com/example/gatekeep/
├── 📱 Activities & Navigation
│   ├── MainActivity.kt              # Compose-based main activity
│   ├── HomeActivity.kt              # Home screen with statistics
│   ├── AppsActivity.kt              # App selection and management
│   └── PermissionsActivity.kt       # Permission setup flow
├── 🎨 UI Components
│   ├── ui/screens/                  # Compose screens
│   ├── ui/theme/                    # Material 3 theming
│   └── views/                       # Custom views (OrbitalView, ParticleView)
├── 🔧 Services
│   ├── AppMonitoringService.kt      # Accessibility service
│   └── OverlayService.kt            # System overlay management
├── 💾 Data Layer
│   ├── data/AppRepository.kt        # Data repository
│   ├── data/AppDatabase.kt          # Room database
│   ├── data/Entities.kt             # Data entities
│   └── data/Daos.kt                 # Database access objects
└── 🎯 Utilities
    ├── adapters/                    # RecyclerView adapters
    └── utils/                       # Helper utilities
```

## 🛠️ Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0+)
- Kotlin 1.8+

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Milind-Ranjan/GateKeep.git
   cd gatekeep
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio

### Required Permissions

GateKeep requires the following permissions to function:

- **Usage Access Permission**: To monitor app launches
- **Accessibility Service**: To detect when gatekept apps are opened
- **System Alert Window**: To display mindful overlay interruptions

## 📱 User Guide

### First Time Setup

1. **Welcome Screen**: Introduction to GateKeep's philosophy
2. **Permission Setup**: Grant required system permissions
3. **App Selection**: Choose which apps to gatekeep
4. **Start Monitoring**: Begin your mindful journey

### Daily Usage

1. **Open a Gatekept App**: When you try to open a selected app
2. **Mindful Pause**: GateKeep presents a breathing exercise
3. **Reflection Prompt**: Answer questions about your intentions
4. **Conscious Choice**: Decide whether to continue or close the app

<div align="center">

*GateKeep is not just an app blocker - it's a companion for developing healthier digital habits through mindfulness and self-reflection.*

</div> 
