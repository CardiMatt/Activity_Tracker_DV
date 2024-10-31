package com.example.activity_tracker_dv.home.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.activity_tracker_dv.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.example.activity_tracker_dv.models.Event
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var totalDistanceKpi: TextView
    private lateinit var totalTimeKpi: TextView
    private lateinit var totalActivitiesKpi: TextView
    private lateinit var eventViewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Configura il ViewModel
        val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
        val repository = EventRepository(eventDao)
        val factory = EventViewModelFactory(repository)
        eventViewModel = ViewModelProvider(this, factory).get(EventViewModel::class.java)

        // Inizializza gli elementi della UI
        mapView = view.findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        pieChart = view.findViewById(R.id.pie_chart)
        barChart = view.findViewById(R.id.bar_chart)
        totalDistanceKpi = view.findViewById(R.id.total_distance_kpi)
        totalTimeKpi = view.findViewById(R.id.total_time_kpi)
        totalActivitiesKpi = view.findViewById(R.id.total_activities_kpi)

        setupStatistics()

        return view
    }

    private fun setupStatistics() {
        eventViewModel.getAllEvents().observe(viewLifecycleOwner) { events ->
            if (events.isNotEmpty()) {
                // Calcolo dei KPI
                val totalDistance = events.sumOf { it.distanceTravelled }
                val totalTime = events.sumOf { TimeUnit.MILLISECONDS.toMinutes(it.end.time - it.launch.time).toDouble() }
                val totalActivities = events.size

                // Imposta i KPI nella UI
                totalDistanceKpi.text = "%.2f km".format(totalDistance)
                totalTimeKpi.text = "${totalTime.toInt()} min"
                totalActivitiesKpi.text = "$totalActivities"

                // Configura il grafico a torta
                setupPieChart(events)

                // Configura il grafico a colonne
                setupBarChart(events)
            } else {
                // Se non ci sono eventi, svuota i grafici e KPI
                totalDistanceKpi.text = "0.00 km"
                totalTimeKpi.text = "0 min"
                totalActivitiesKpi.text = "0"
                pieChart.clear()
                barChart.clear()
            }
        }
    }

    private fun setupPieChart(events: List<Event>) {
        val activityTypeCount = events.groupBy { it.eventType }.mapValues { it.value.size }
        val pieEntries = activityTypeCount.map { PieEntry(it.value.toFloat(), it.key) }

        val pieDataSet = PieDataSet(pieEntries, "Tipo di Attività").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        val pieData = PieData(pieDataSet)

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            legend.textColor = Color.WHITE
            setEntryLabelColor(Color.WHITE)
            setUsePercentValues(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(events: List<Event>) {
        val activityTimeData = events.groupBy { it.eventType }
            .mapValues { entry ->
                entry.value.sumOf { TimeUnit.MILLISECONDS.toMinutes(it.end.time - it.launch.time).toDouble() }.toFloat()
            }
        val barEntries = activityTimeData.keys.mapIndexed { index, key -> BarEntry(index.toFloat(), activityTimeData[key] ?: 0f) }
        val barDataSet = BarDataSet(barEntries, "Tempo per Attività").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }
        val barData = BarData(barDataSet)

        barChart.apply {
            data = barData
            setBackgroundColor(Color.DKGRAY)
            description.isEnabled = false
            xAxis.apply {
                textColor = Color.WHITE
                valueFormatter = IndexAxisValueFormatter(activityTimeData.keys.toList())
                granularity = 1f
            }
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            legend.textColor = Color.WHITE
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        eventViewModel.getAllEvents().observe(viewLifecycleOwner) { events ->
            googleMap.clear() // Clear previous markers
            if (events.isNotEmpty()) {
                events.forEach { event ->
                    val startLatLng = LatLng(event.startLatitude, event.startLongitude)
                    googleMap.addMarker(MarkerOptions().position(startLatLng).title("Inizio: ${event.eventType}"))
                    val endLatLng = LatLng(event.endLatitude, event.endLongitude)
                    googleMap.addMarker(MarkerOptions().position(endLatLng).title("Fine: ${event.eventType}"))
                }
                val firstEvent = events.first()
                val firstLatLng = LatLng(firstEvent.startLatitude, firstEvent.startLongitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 12f))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
