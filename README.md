# WeatherApp 🌤️

A complete Android weather application built with **Kotlin**, **MVVM architecture**, and
**Retrofit** for real-time weather data from the [OpenWeatherMap API](https://openweathermap.org/api).

---

## Features

| Feature | Details |
|---|---|
| City search | Type any city name and tap **Search** |
| GPS weather | Tap the 📍 button to get weather for your current location |
| Current weather | Temperature (°C), condition, feels-like, humidity, wind, pressure |
| 5-day forecast | Horizontal scrollable forecast list with icons |
| Dynamic backgrounds | Gradient changes based on weather condition (clear / cloudy / rain / snow / thunderstorm) |
| Smooth animations | Weather card slides up & fades in when data loads |
| Error handling | Friendly messages for invalid city, no internet, timeout, or bad API key |
| Loading indicator | ProgressBar shown while data is being fetched |

---

## Architecture – MVVM

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer  (app/src/main/java/.../ui)                       │
│   └── MainActivity  ──observes──►  WeatherViewModel         │
├─────────────────────────────────────────────────────────────┤
│  ViewModel Layer  (.../viewmodel)                           │
│   └── WeatherViewModel  ──calls──►  WeatherRepository       │
├─────────────────────────────────────────────────────────────┤
│  Data Layer  (.../data)                                     │
│   ├── WeatherRepository  (wraps API calls in Result<T>)     │
│   ├── WeatherApiService  (Retrofit interface)               │
│   ├── RetrofitInstance   (OkHttp + Gson singleton)          │
│   └── Models: WeatherResponse, ForecastResponse             │
└─────────────────────────────────────────────────────────────┘
```

- **StateFlow** (`weatherState`, `forecastState`) carries UI state from ViewModel to Activity.
- **WeatherUiState** is a sealed class: `Idle | Loading | CurrentWeatherSuccess | Error`.
- The Activity only *observes* state – it never calls the network directly.

---

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/weather/app/
│   ├── data/
│   │   ├── api/
│   │   │   ├── RetrofitInstance.kt      ← Retrofit singleton
│   │   │   └── WeatherApiService.kt     ← Retrofit interface
│   │   ├── model/
│   │   │   ├── WeatherResponse.kt       ← Current weather data model
│   │   │   └── ForecastResponse.kt      ← 5-day forecast data model
│   │   └── repository/
│   │       └── WeatherRepository.kt     ← Data source abstraction
│   ├── ui/
│   │   ├── MainActivity.kt              ← Single-activity UI
│   │   └── ForecastAdapter.kt           ← RecyclerView adapter
│   └── viewmodel/
│       ├── WeatherViewModel.kt          ← Business logic + StateFlow
│       └── WeatherViewModelFactory.kt   ← ViewModel factory
└── res/
    ├── layout/
    │   ├── activity_main.xml            ← Main screen layout
    │   └── item_forecast.xml            ← Forecast list item
    ├── drawable/                        ← Vector icons & gradient backgrounds
    ├── anim/slide_up_fade_in.xml        ← Card entrance animation
    └── values/                          ← colors, strings, themes
```

---

## Getting started

### 1. Get an OpenWeatherMap API key (free)
1. Sign up at <https://openweathermap.org/api>
2. Go to **API keys** and copy your key.

### 2. Set your API key

Open `app/build.gradle` and replace `YOUR_API_KEY_HERE`:

```groovy
buildConfigField "String", "WEATHER_API_KEY", "\"YOUR_REAL_KEY_HERE\""
```

> **Never commit real API keys to public repositories.**
> Consider using a `local.properties` file and reading it in `build.gradle`.

### 3. Build & run

Open the project in **Android Studio Hedgehog (2023.1.1)** or later and press **Run ▶**.

Requirements:
- Android SDK 34
- Gradle 8.2
- JDK 17

---

## Running tests

```bash
./gradlew test          # JVM unit tests (WeatherViewModelTest)
./gradlew connectedAndroidTest   # Instrumented tests (needs a device/emulator)
```

---

## Dependencies

| Library | Purpose |
|---|---|
| Retrofit 2.9 | HTTP client for OpenWeatherMap API |
| OkHttp logging interceptor | Logs network requests in Logcat |
| Gson converter | JSON → Kotlin data class |
| Kotlin Coroutines | Async/suspend functions |
| ViewModel + StateFlow | MVVM lifecycle-aware state |
| Glide | Loading weather icons from CDN |
| Google Play Services Location | GPS / FusedLocationProvider |

---

## How each part works (beginner-friendly)

| Part | What it does |
|---|---|
| **WeatherApiService** | Defines the two API endpoints using Retrofit `@GET` annotations. Retrofit auto-generates the actual HTTP calls. |
| **RetrofitInstance** | Creates a single Retrofit object (singleton) configured with the base URL and Gson for JSON parsing. |
| **WeatherRepository** | Calls the API inside a coroutine on the IO thread and wraps the result in `Result<T>`. The ViewModel never needs to know about exceptions. |
| **WeatherViewModel** | Holds `StateFlow` that the Activity observes. Calls the repository and converts results into UI states. Survives screen rotation. |
| **WeatherViewModelFactory** | Required by Android because our ViewModel takes constructor arguments. |
| **MainActivity** | Observes the ViewModel's StateFlow and updates views. Zero network code here. |
| **ForecastAdapter** | Standard RecyclerView adapter. Filters API's 3-hourly slots down to one per day (the noon slot). |

