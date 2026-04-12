package com.weather.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for the current weather API response from OpenWeatherMap.
 * Maps to the JSON returned by: GET /data/2.5/weather?q={city}&appid={key}&units=metric
 */
data class WeatherResponse(
    @SerializedName("name")
    val cityName: String,

    @SerializedName("sys")
    val sys: Sys,

    @SerializedName("main")
    val main: Main,

    @SerializedName("weather")
    val weather: List<WeatherCondition>,

    @SerializedName("wind")
    val wind: Wind,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("dt")
    val timestamp: Long,

    @SerializedName("cod")
    val cod: Int
)

data class Sys(
    @SerializedName("country")
    val country: String
)

data class Main(
    @SerializedName("temp")
    val temperature: Double,           // Celsius when units=metric

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("temp_min")
    val tempMin: Double,

    @SerializedName("temp_max")
    val tempMax: Double,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("pressure")
    val pressure: Int
)

data class WeatherCondition(
    @SerializedName("id")
    val id: Int,

    @SerializedName("main")
    val main: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("icon")
    val icon: String        // e.g. "01d" – used to build the icon URL
)

data class Wind(
    @SerializedName("speed")
    val speed: Double,

    @SerializedName("deg")
    val degree: Int
)
