package com.weather.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.app.data.model.ForecastResponse
import com.weather.app.data.model.WeatherResponse
import com.weather.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class that represents every possible UI state for the weather screen.
 *
 * Using a sealed class ensures the UI always handles every possible outcome
 * (loading, success, error, idle) at compile time.
 */
sealed class WeatherUiState {
    /** Nothing has been fetched yet – initial state. */
    object Idle : WeatherUiState()

    /** A network request is in progress; show a loading indicator. */
    object Loading : WeatherUiState()

    /** Current weather was fetched successfully. */
    data class CurrentWeatherSuccess(val weather: WeatherResponse) : WeatherUiState()

    /** Something went wrong; [message] describes the problem in plain English. */
    data class Error(val message: String) : WeatherUiState()
}

/**
 * Sealed class for the 5-day forecast UI state.
 */
sealed class ForecastUiState {
    object Idle    : ForecastUiState()
    object Loading : ForecastUiState()
    data class Success(val forecast: ForecastResponse) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

/**
 * WeatherViewModel is the bridge between the UI (MainActivity) and the data layer
 * (WeatherRepository).
 *
 * It exposes two [StateFlow]s that the Activity observes:
 *  - [weatherState]  – current weather for a searched city / GPS location
 *  - [forecastState] – 5-day forecast for the same city / location
 *
 * ViewModels survive screen rotations, so weather data is not re-fetched just
 * because the user rotated their phone.
 *
 * @param repository Injected repository (easy to replace with a fake in tests)
 */
class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    // --- StateFlows -------------------------------------------------------

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    /** Observed by the Activity to react to weather data changes. */
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _forecastState = MutableStateFlow<ForecastUiState>(ForecastUiState.Idle)
    /** Observed by the Activity to react to forecast data changes. */
    val forecastState: StateFlow<ForecastUiState> = _forecastState.asStateFlow()

    // Remembers the last successfully searched city name so the UI can display it.
    private var _lastSearchedCity = MutableStateFlow("")
    val lastSearchedCity: StateFlow<String> = _lastSearchedCity.asStateFlow()

    // --- Public API -------------------------------------------------------

    /**
     * Fetch current weather + 5-day forecast for [cityName].
     * Both requests run concurrently inside [viewModelScope].
     */
    fun fetchWeatherByCity(cityName: String) {
        if (cityName.isBlank()) {
            _weatherState.value = WeatherUiState.Error("Please enter a city name.")
            return
        }
        _lastSearchedCity.value = cityName.trim()
        fetchCurrentWeather(cityName)
        fetchForecast(cityName)
    }

    /**
     * Fetch current weather + 5-day forecast using GPS coordinates.
     */
    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        fetchCurrentWeatherByLocation(lat, lon)
        fetchForecastByLocation(lat, lon)
    }

    // --- Private helpers --------------------------------------------------

    private fun fetchCurrentWeather(cityName: String) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            val result = repository.getCurrentWeather(cityName)
            _weatherState.value = result.fold(
                onSuccess = { WeatherUiState.CurrentWeatherSuccess(it) },
                onFailure = { WeatherUiState.Error(friendlyError(it)) }
            )
        }
    }

    private fun fetchForecast(cityName: String) {
        viewModelScope.launch {
            _forecastState.value = ForecastUiState.Loading
            val result = repository.getForecast(cityName)
            _forecastState.value = result.fold(
                onSuccess = { ForecastUiState.Success(it) },
                onFailure = { ForecastUiState.Error(friendlyError(it)) }
            )
        }
    }

    private fun fetchCurrentWeatherByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            val result = repository.getCurrentWeatherByLocation(lat, lon)
            _weatherState.value = result.fold(
                onSuccess = {
                    _lastSearchedCity.value = it.cityName
                    WeatherUiState.CurrentWeatherSuccess(it)
                },
                onFailure = { WeatherUiState.Error(friendlyError(it)) }
            )
        }
    }

    private fun fetchForecastByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _forecastState.value = ForecastUiState.Loading
            val result = repository.getForecastByLocation(lat, lon)
            _forecastState.value = result.fold(
                onSuccess = { ForecastUiState.Success(it) },
                onFailure = { ForecastUiState.Error(friendlyError(it)) }
            )
        }
    }

    /**
     * Convert raw exceptions into beginner-friendly messages.
     */
    private fun friendlyError(throwable: Throwable): String {
        val msg = throwable.message ?: ""
        return when {
            msg.contains("404")                          -> "City not found. Please check the spelling."
            msg.contains("401")                          -> "Invalid API key. Please check your configuration."
            msg.contains("UnknownHostException") ||
            msg.contains("Unable to resolve host")       -> "No internet connection. Please check your network."
            msg.contains("SocketTimeoutException") ||
            msg.contains("timeout")                      -> "Request timed out. Please try again."
            else                                         -> "Something went wrong: $msg"
        }
    }
}
