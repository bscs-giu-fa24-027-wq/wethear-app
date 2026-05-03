package com.weather.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.weather.app.R
import com.weather.app.data.model.ForecastItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter that displays a horizontal list of 5-day / 3-hour forecast slots.
 *
 * Each item shows:
 *  - Day / time label  (e.g. "Mon 15:00")
 *  - A small weather icon fetched from OpenWeatherMap's icon CDN
 *  - Temperature in °C
 */
class ForecastAdapter(
    private var items: List<ForecastItem> = emptyList()
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    /** One forecast slot in the list. */
    inner class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView  = itemView.findViewById(R.id.tvForecastDate)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivForecastIcon)
        val tvTemp: TextView  = itemView.findViewById(R.id.tvForecastTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = items[position]

        // Format the timestamp to "EEE HH:mm" (e.g. "Mon 15:00")
        val date = Date(item.timestamp * 1000L)
        val formatter = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
        holder.tvDate.text = formatter.format(date)

        // Round temperature to one decimal place
        holder.tvTemp.text = "${item.main.temperature.toInt()}°C"

        // Load the weather icon from OpenWeatherMap's CDN
        if (item.weather.isNotEmpty()) {
            val iconCode = item.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            Glide.with(holder.itemView.context)
                .load(iconUrl)
                .placeholder(R.drawable.ic_cloud_placeholder)
                .into(holder.ivIcon)
        }
    }

    /**
     * Replace the dataset and refresh the list.
     * We show one slot per day (noon slot) to keep the list manageable.
     */
    fun submitList(newItems: List<ForecastItem>) {
        // Filter to keep only one entry per calendar day (preferably the noon/12:00 slot)
        val dailyItems = newItems
            .groupBy { it.dateText.substring(0, 10) } // group by "YYYY-MM-DD"
            .values
            .map { dayItems ->
                // Prefer the 12:00:00 slot, otherwise just take the first one
                dayItems.firstOrNull { it.dateText.contains("12:00:00") } ?: dayItems.first()
            }
            .take(5) // maximum 5 days
        items = dailyItems
        notifyDataSetChanged()
    }
}
