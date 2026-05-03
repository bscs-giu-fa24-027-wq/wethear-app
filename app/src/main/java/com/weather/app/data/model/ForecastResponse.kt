package com.weather.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for the 5-day / 3-hour forecast API response from OpenWeatherMap.
 * Maps to the JSON returned by: GET /data/2.5/forecast?q={city}&appid={key}&units=metric
 */
data class ForecastResponse(
    @SerializedName("city")
    val city: City,

    @SerializedName("list")
    val forecasts: List<ForecastItem>,

    @SerializedName("cod")
    val cod: String,

    @SerializedName("cnt")
    val count: Int
)

data class City(
    @SerializedName("name")
    val name: String,

    @SerializedName("country")
    val country: String
)

/**
 * A single forecast slot (every 3 hours).
 */
data class ForecastItem(
    @SerializedName("dt")
    val timestamp: Long,

    @SerializedName("main")
    val main: Main,                       // Reuses the Main model from WeatherResponse

    @SerializedName("weather")
    val weather: List<WeatherCondition>,  // Reuses WeatherCondition from WeatherResponse

    @SerializedName("wind")
    val wind: Wind,                       // Reuses Wind from WeatherResponse

    @SerializedName("dt_txt")
    val dateText: String                  // e.g. "2024-01-15 12:00:00"
)
