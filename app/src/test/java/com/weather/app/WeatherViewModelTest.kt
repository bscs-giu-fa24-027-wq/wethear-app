package com.weather.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.weather.app.data.model.ForecastResponse
import com.weather.app.data.model.City
import com.weather.app.data.model.ForecastItem
import com.weather.app.data.model.Main
import com.weather.app.data.model.WeatherCondition
import com.weather.app.data.model.WeatherResponse
import com.weather.app.data.model.Sys
import com.weather.app.data.model.Wind
import com.weather.app.data.repository.WeatherRepository
import com.weather.app.viewmodel.ForecastUiState
import com.weather.app.viewmodel.WeatherUiState
import com.weather.app.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [WeatherViewModel].
 *
 * These tests verify that:
 *  1. Searching with a blank city name emits an [Error] state immediately.
 *  2. A successful API response transitions the state to [CurrentWeatherSuccess].
 *  3. A failed API call transitions the state to [Error] with a friendly message.
 *
 * The tests use:
 *  - Mockito-Kotlin to mock [WeatherRepository] without hitting the network.
 *  - [StandardTestDispatcher] to control coroutine execution.
 *  - [InstantTaskExecutorRule] to run LiveData updates synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    // Makes LiveData run synchronously in tests
    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Mocked repository – no real network calls are made
    private val mockRepository: WeatherRepository = mock()

    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WeatherViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------
    // Helpers – build minimal fake API responses
    // -----------------------------------------------------------------

    private fun fakeWeatherResponse(city: String = "London") = WeatherResponse(
        cityName  = city,
        sys       = Sys(country = "GB"),
        main      = Main(
            temperature = 18.5,
            feelsLike   = 16.0,
            tempMin     = 14.0,
            tempMax     = 21.0,
            humidity    = 65,
            pressure    = 1012
        ),
        weather   = listOf(
            WeatherCondition(id = 800, main = "Clear", description = "clear sky", icon = "01d")
        ),
        wind      = Wind(speed = 4.5, degree = 270),
        visibility = 10000,
        timestamp  = 1700000000L,
        cod        = 200
    )

    private fun fakeForecastResponse() = ForecastResponse(
        city      = City(name = "London", country = "GB"),
        forecasts = listOf(
            ForecastItem(
                timestamp = 1700000000L,
                main      = Main(18.0, 16.0, 14.0, 21.0, 60, 1010),
                weather   = listOf(WeatherCondition(800, "Clear", "clear sky", "01d")),
                wind      = Wind(4.0, 270),
                dateText  = "2024-01-15 12:00:00"
            )
        ),
        cod   = "200",
        count = 1
    )

    // -----------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------

    @Test
    fun `initial state is Idle`() {
        // ViewModel state should start as Idle before any action is taken
        assertTrue(viewModel.weatherState.value is WeatherUiState.Idle)
        assertTrue(viewModel.forecastState.value is ForecastUiState.Idle)
    }

    @Test
    fun `blank city name emits Error immediately`() {
        // Act – search with an empty string
        viewModel.fetchWeatherByCity("")

        // Assert – should emit Error synchronously (no coroutine delay)
        val state = viewModel.weatherState.value
        assertTrue("Expected Error state but got $state", state is WeatherUiState.Error)
        val errorMsg = (state as WeatherUiState.Error).message
        assertTrue(errorMsg.contains("city", ignoreCase = true))
    }

    @Test
    fun `successful weather fetch emits CurrentWeatherSuccess`() = runTest {
        // Arrange – make the repository return a fake success
        whenever(mockRepository.getCurrentWeather("London"))
            .thenReturn(Result.success(fakeWeatherResponse()))
        whenever(mockRepository.getForecast("London"))
            .thenReturn(Result.success(fakeForecastResponse()))

        // Act
        viewModel.fetchWeatherByCity("London")
        advanceUntilIdle()

        // Assert
        val state = viewModel.weatherState.value
        assertTrue("Expected CurrentWeatherSuccess but got $state",
            state is WeatherUiState.CurrentWeatherSuccess)
        val data = (state as WeatherUiState.CurrentWeatherSuccess).weather
        assertEquals("London", data.cityName)
        assertEquals(18.5, data.main.temperature, 0.01)
    }

    @Test
    fun `failed weather fetch emits Error with friendly message`() = runTest {
        // Arrange – make the repository return a city-not-found failure
        whenever(mockRepository.getCurrentWeather("InvalidCity"))
            .thenReturn(Result.failure(Exception("HTTP 404 Not Found")))
        whenever(mockRepository.getForecast("InvalidCity"))
            .thenReturn(Result.failure(Exception("HTTP 404 Not Found")))

        // Act
        viewModel.fetchWeatherByCity("InvalidCity")
        advanceUntilIdle()

        // Assert
        val state = viewModel.weatherState.value
        assertTrue("Expected Error state but got $state", state is WeatherUiState.Error)
        val message = (state as WeatherUiState.Error).message
        assertTrue("Expected 404-related message but got: $message",
            message.contains("City not found", ignoreCase = true))
    }

    @Test
    fun `no internet connection produces friendly error message`() = runTest {
        // Arrange
        whenever(mockRepository.getCurrentWeather("London"))
            .thenReturn(Result.failure(Exception("UnknownHostException: Unable to resolve host")))
        whenever(mockRepository.getForecast("London"))
            .thenReturn(Result.failure(Exception("UnknownHostException")))

        // Act
        viewModel.fetchWeatherByCity("London")
        advanceUntilIdle()

        // Assert
        val state = viewModel.weatherState.value as WeatherUiState.Error
        assertTrue(state.message.contains("internet", ignoreCase = true))
    }

    @Test
    fun `lastSearchedCity is updated after a successful search`() = runTest {
        whenever(mockRepository.getCurrentWeather("Paris"))
            .thenReturn(Result.success(fakeWeatherResponse("Paris")))
        whenever(mockRepository.getForecast("Paris"))
            .thenReturn(Result.success(fakeForecastResponse()))

        viewModel.fetchWeatherByCity("Paris")
        advanceUntilIdle()

        assertEquals("Paris", viewModel.lastSearchedCity.value)
    }

    @Test
    fun `forecast success state contains forecast data`() = runTest {
        whenever(mockRepository.getCurrentWeather("Tokyo"))
            .thenReturn(Result.success(fakeWeatherResponse("Tokyo")))
        whenever(mockRepository.getForecast("Tokyo"))
            .thenReturn(Result.success(fakeForecastResponse()))

        viewModel.fetchWeatherByCity("Tokyo")
        advanceUntilIdle()

        val forecastState = viewModel.forecastState.value
        assertTrue(forecastState is ForecastUiState.Success)
        val forecastData = (forecastState as ForecastUiState.Success).forecast
        assertEquals(1, forecastData.forecasts.size)
    }
}
