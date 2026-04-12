package com.weather.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.weather.app.data.repository.WeatherRepository

/**
 * Factory that creates a [WeatherViewModel] with the required [WeatherRepository] dependency.
 *
 * Android's [ViewModelProvider] needs a factory when the ViewModel has constructor arguments.
 * Without this factory, the framework can only create ViewModels with a no-arg constructor.
 */
class WeatherViewModelFactory(
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
