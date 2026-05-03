package com.weather.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weather.app.BuildConfig
import com.weather.app.R
import com.weather.app.data.api.RetrofitInstance
import com.weather.app.data.repository.WeatherRepository
import com.weather.app.databinding.ActivityMainBinding
import com.weather.app.viewmodel.ForecastUiState
import com.weather.app.viewmodel.WeatherUiState
import com.weather.app.viewmodel.WeatherViewModel
import com.weather.app.viewmodel.WeatherViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * MainActivity – the single screen of the WeatherApp.
 *
 * Responsibilities:
 *  1. Renders the search bar, current-weather card, and 5-day forecast list.
 *  2. Forwards user input (city search / GPS button) to [WeatherViewModel].
 *  3. Observes [WeatherViewModel.weatherState] and [WeatherViewModel.forecastState]
 *     to update the UI reactively.
 *  4. Handles runtime permissions for ACCESS_FINE_LOCATION.
 *
 * The activity itself contains NO network or business logic – that all lives in
 * WeatherViewModel and WeatherRepository.
 */
class MainActivity : AppCompatActivity() {

    // View Binding – auto-generated from activity_main.xml (no more findViewById!)
    private lateinit var binding: ActivityMainBinding

    // ViewModel that survives screen rotations
    private lateinit var viewModel: WeatherViewModel

    // Google's FusedLocationProviderClient for GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Adapter for the horizontal forecast RecyclerView
    private val forecastAdapter = ForecastAdapter()

    // Permission launcher – Android 6+ requires explicit runtime permission for GPS
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocationWeather()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout via ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupLocationClient()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    /** Wire up ViewModel with its Repository and Factory. */
    private fun setupViewModel() {
        val repository = WeatherRepository(
            apiService = RetrofitInstance.weatherApiService,
            apiKey     = BuildConfig.WEATHER_API_KEY
        )
        val factory = WeatherViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /** Configure the horizontal RecyclerView for the 5-day forecast. */
    private fun setupRecyclerView() {
        binding.rvForecast.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = forecastAdapter
        }
    }

    /** Attach click listeners and keyboard action listeners. */
    private fun setupListeners() {
        // Search button
        binding.btnSearch.setOnClickListener {
            val city = binding.etCityName.text.toString()
            viewModel.fetchWeatherByCity(city)
            hideKeyboard()
        }

        // Allow "Done" / "Search" keyboard action to trigger search
        binding.etCityName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                val city = binding.etCityName.text.toString()
                viewModel.fetchWeatherByCity(city)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // GPS / current-location button
        binding.btnLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    // -------------------------------------------------------------------------
    // ViewModel observation
    // -------------------------------------------------------------------------

    /** Collect StateFlow updates and drive the UI accordingly. */
    private fun observeViewModel() {
        // Current weather
        lifecycleScope.launch {
            viewModel.weatherState.collectLatest { state ->
                when (state) {
                    is WeatherUiState.Idle    -> showIdleState()
                    is WeatherUiState.Loading -> showLoading(true)
                    is WeatherUiState.CurrentWeatherSuccess -> {
                        showLoading(false)
                        showWeatherCard(state.weather)
                    }
                    is WeatherUiState.Error   -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        // 5-day forecast
        lifecycleScope.launch {
            viewModel.forecastState.collectLatest { state ->
                when (state) {
                    is ForecastUiState.Idle    -> hideForecast()
                    is ForecastUiState.Loading -> { /* spinner already showing */ }
                    is ForecastUiState.Success -> {
                        forecastAdapter.submitList(state.forecast.forecasts)
                        binding.cardForecast.visibility = View.VISIBLE
                    }
                    is ForecastUiState.Error   -> hideForecast()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private fun showIdleState() {
        binding.cardWeather.visibility  = View.GONE
        binding.cardForecast.visibility = View.GONE
        binding.tvError.visibility      = View.GONE
        binding.progressBar.visibility  = View.GONE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.cardWeather.visibility  = View.GONE
            binding.tvError.visibility      = View.GONE
        }
    }

    /**
     * Populate the weather card with data from the API response and animate it in.
     */
    private fun showWeatherCard(weather: com.weather.app.data.model.WeatherResponse) {
        val condition = weather.weather.firstOrNull()

        // City & country
        binding.tvCity.text = "${weather.cityName}, ${weather.sys.country}"

        // Temperature – round to nearest integer
        binding.tvTemperature.text = "${weather.main.temperature.roundToInt()}°C"

        // "Feels like" and condition description
        binding.tvFeelsLike.text = getString(
            R.string.feels_like_template,
            weather.main.feelsLike.roundToInt()
        )
        binding.tvCondition.text = condition?.description?.replaceFirstChar { it.uppercase() } ?: ""

        // Extra details
        binding.tvHumidity.text    = getString(R.string.humidity_template, weather.main.humidity)
        binding.tvWindSpeed.text   = getString(R.string.wind_template, weather.wind.speed)
        binding.tvPressure.text    = getString(R.string.pressure_template, weather.main.pressure)

        // Weather icon from OpenWeatherMap CDN
        condition?.icon?.let { iconCode ->
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            Glide.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.ic_cloud_placeholder)
                .into(binding.ivWeatherIcon)
        }

        // Choose a gradient background based on weather condition group
        updateBackground(condition?.main)

        // Show card with a slide-up + fade-in animation
        binding.cardWeather.visibility = View.VISIBLE
        binding.tvError.visibility     = View.GONE
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.cardWeather.startAnimation(anim)
    }

    /**
     * Change the root background gradient based on the broad weather condition.
     *
     * Condition groups used by OpenWeatherMap:
     *  Thunderstorm, Drizzle, Rain, Snow, Atmosphere, Clear, Clouds
     */
    private fun updateBackground(conditionMain: String?) {
        val bgRes = when (conditionMain?.lowercase()) {
            "thunderstorm" -> R.drawable.bg_thunderstorm
            "drizzle",
            "rain"         -> R.drawable.bg_rain
            "snow"         -> R.drawable.bg_snow
            "clear"        -> R.drawable.bg_clear
            "clouds"       -> R.drawable.bg_cloudy
            else           -> R.drawable.bg_default
        }
        binding.rootLayout.setBackgroundResource(bgRes)
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
        binding.cardWeather.visibility  = View.GONE
        binding.cardForecast.visibility = View.GONE
    }

    private fun hideForecast() {
        binding.cardForecast.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCityName.windowToken, 0)
    }

    // -------------------------------------------------------------------------
    // Location / GPS
    // -------------------------------------------------------------------------

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLocationWeather()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, getString(R.string.location_rationale), Toast.LENGTH_LONG).show()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationWeather() {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.fetchWeatherByLocation(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_SHORT).show()
            }
    }
}
