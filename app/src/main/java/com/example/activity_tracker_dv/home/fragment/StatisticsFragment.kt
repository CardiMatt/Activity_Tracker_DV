package com.example.activity_tracker_dv.home.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.example.activity_tracker_dv.models.Event
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var temporalBarChart: BarChart
    private lateinit var totalDistanceKpi: TextView
    private lateinit var totalTimeKpi: TextView
    private lateinit var totalActivitiesKpi: TextView
    private lateinit var metricSpinner: Spinner
    private lateinit var timeFilterSpinner: Spinner
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
        temporalBarChart = view.findViewById(R.id.temporal_bar_chart)
        totalDistanceKpi = view.findViewById(R.id.total_distance_kpi)
        totalTimeKpi = view.findViewById(R.id.total_time_kpi)
        totalActivitiesKpi = view.findViewById(R.id.total_activities_kpi)
        metricSpinner = view.findViewById(R.id.metric_spinner)
        timeFilterSpinner = view.findViewById(R.id.time_filter_spinner)

        setupStatistics()

        // Setup listener per il cambio della metrica nel grafico
        metricSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                setupStatistics() // Aggiorna le statistiche in base alla metrica selezionata
                val selectedFilter = timeFilterSpinner.selectedItem.toString()
                setupTemporalChart(selectedFilter) // Aggiorna anche il grafico temporale
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Setup listener per il filtro temporale
        timeFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFilter = parent.getItemAtPosition(position).toString()
                setupTemporalChart(selectedFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }

    private fun setupStatistics() {
        // Recupera l'utente attualmente autenticato
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userUsername = user.email ?: "Unknown"

            // Ottiene gli eventi solo per l'utente autenticato
            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner) { events ->
                if (events.isNotEmpty()) {
                    // Calcola i KPI
                    val totalDistance = events.sumOf { it.distanceTravelled }
                    val totalTimeInMillis = events.sumOf { it.end.time - it.launch.time }
                    val totalActivities = events.size
                    val totalSteps = events.sumOf { it.steps }

                    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeInMillis)
                    val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(totalTimeInMillis) % 60

                    // Imposta i KPI nella UI
                    totalDistanceKpi.text = "%.2f km".format(totalDistance)
                    totalTimeKpi.text = "${totalMinutes} min ${remainingSeconds} sec"
                    totalActivitiesKpi.text = "$totalActivities"

                    // Configura il grafico a torta
                    setupPieChart(events)

                    // Configura il grafico a colonne
                    setupBarChart(events)
                } else {
                    // Se non ci sono eventi, svuota i grafici e KPI
                    totalDistanceKpi.text = "0.00 km"
                    totalTimeKpi.text = "0 min 0 sec"
                    totalActivitiesKpi.text = "0"
                    pieChart.clear()
                    barChart.clear()
                    temporalBarChart.clear()
                }
            }
        } ?: run {
            Log.e("StatisticsFragment", "Utente non autenticato")
        }
    }

    private fun setupPieChart(events: List<Event>) {
        val selectedMetric = metricSpinner.selectedItem.toString()
        val dataMap = when (selectedMetric) {
            "Numero Attività" -> events.groupBy { it.eventType }.mapValues { it.value.size }
            "Tempo Attività" -> events.groupBy { it.eventType }.mapValues { entry ->
                entry.value.sumOf { (TimeUnit.MILLISECONDS.toMinutes(it.end.time - it.launch.time) +
                        TimeUnit.MILLISECONDS.toSeconds(it.end.time - it.launch.time) % 60 / 60.0).toDouble() }.toFloat()
            }
            "Distanza" -> events.groupBy { it.eventType }.mapValues { it.value.sumOf { it.distanceTravelled } }
            "Passi" -> events.groupBy { it.eventType }.mapValues { it.value.sumOf { it.steps }.toFloat() }
            else -> emptyMap()
        }

        val pieEntries = dataMap.map { PieEntry(it.value.toFloat(), it.key) }

        val pieDataSet = PieDataSet(pieEntries, "Tipo di Attività").apply {
            colors = events.groupBy { it.eventType }.keys.map { getColorFromMarker(it) }
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
        val selectedMetric = metricSpinner.selectedItem.toString()
        val dataMap = when (selectedMetric) {
            "Numero Attività" -> events.groupBy { it.eventType }.mapValues { it.value.size.toFloat() }
            "Tempo Attività" -> events.groupBy { it.eventType }.mapValues { entry ->
                entry.value.sumOf { (TimeUnit.MILLISECONDS.toMinutes(it.end.time - it.launch.time) +
                        TimeUnit.MILLISECONDS.toSeconds(it.end.time - it.launch.time) % 60 / 60.0).toDouble() }.toFloat()
            }
            "Distanza" -> events.groupBy { it.eventType }.mapValues { it.value.sumOf { it.distanceTravelled }.toFloat() }
            "Passi" -> events.groupBy { it.eventType }.mapValues { it.value.sumOf { it.steps }.toFloat() }
            else -> emptyMap()
        }

        val barEntries = dataMap.keys.mapIndexed { index, key -> BarEntry(index.toFloat(), dataMap[key] ?: 0f) }
        val barDataSet = BarDataSet(barEntries, "${selectedMetric} per Tipo di Attività").apply {
            colors = events.groupBy { it.eventType }.keys.map { getColorFromMarker(it) }
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
                valueFormatter = IndexAxisValueFormatter(dataMap.keys.toList())
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

    private fun setupTemporalChart(selectedFilter: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userUsername = user.email ?: "Unknown"

            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner) { events ->
                if (events.isNotEmpty()) {
                    // Filtra gli eventi in base al periodo selezionato
                    val filteredEvents = when (selectedFilter) {
                        "Giornaliero" -> events.groupBy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.launch) }
                        "Settimanale" -> events.groupBy { SimpleDateFormat("ww/yyyy", Locale.getDefault()).format(it.launch) }
                        else -> emptyMap()
                    }

                    val selectedMetric = metricSpinner.selectedItem.toString()
                    val barEntries = mutableListOf<BarEntry>()
                    val activityTypes = events.groupBy { it.eventType }.keys.toList()
                    val stackedValues = mutableMapOf<String, MutableList<Float>>()

                    // Inizializza le liste di valori per ogni tipo di attività
                    activityTypes.forEach { activityType ->
                        stackedValues[activityType] = MutableList(filteredEvents.size) { 0f }
                    }

                    filteredEvents.keys.forEachIndexed { index, key ->
                        activityTypes.forEach { activityType ->
                            val totalValue = when (selectedMetric) {
                                "Numero Attività" -> filteredEvents[key]?.filter { it.eventType == activityType }?.size?.toFloat() ?: 0f
                                "Tempo Attività" -> filteredEvents[key]?.filter { it.eventType == activityType }?.sumOf { it.end.time - it.launch.time }?.let { totalTime ->
                                    TimeUnit.MILLISECONDS.toMinutes(totalTime) + (TimeUnit.MILLISECONDS.toSeconds(totalTime) % 60) / 60.0f
                                } ?: 0f
                                "Distanza" -> filteredEvents[key]?.filter { it.eventType == activityType }?.sumOf { it.distanceTravelled }?.toFloat() ?: 0f
                                "Passi" -> filteredEvents[key]?.filter { it.eventType == activityType }?.sumOf { it.steps }?.toFloat() ?: 0f
                                else -> 0f
                            }
                            stackedValues[activityType]?.set(index, totalValue)
                        }
                    }

                    filteredEvents.keys.forEachIndexed { index, key ->
                        val values = activityTypes.map { activityType ->
                            stackedValues[activityType]?.get(index) ?: 0f
                        }.toFloatArray()
                        if (values.isNotEmpty()) { // Check to avoid ArrayIndexOutOfBoundsException
                            barEntries.add(BarEntry(index.toFloat(), values))
                        }
                    }

                    val barDataSet = BarDataSet(barEntries, "Attività per ${selectedFilter}").apply {
                        colors = activityTypes.map { getColorFromMarker(it) }
                        valueTextColor = Color.WHITE
                        valueTextSize = 10f
                    }

                    val barData = BarData(barDataSet)

                    temporalBarChart.apply {
                        data = barData
                        setBackgroundColor(Color.DKGRAY)
                        description.isEnabled = false
                        xAxis.apply {
                            textColor = Color.WHITE
                            valueFormatter = IndexAxisValueFormatter(filteredEvents.keys.toList())
                            granularity = 1f
                        }
                        axisLeft.textColor = Color.WHITE
                        axisRight.isEnabled = false
                        legend.textColor = Color.WHITE
                        animateY(1000)
                        invalidate()
                    }
                } else {
                    temporalBarChart.clear()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userUsername = user.email ?: "Unknown"

            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner) { events ->
                googleMap.clear()
                if (events.isNotEmpty()) {
                    events.forEach { event ->
                        val startLatLng = LatLng(event.startLatitude, event.startLongitude)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(startLatLng)
                                .title("Inizio: ${event.eventType} - ${event.id}")
                                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(event.eventType)))
                        )

                        val endLatLng = LatLng(event.endLatitude, event.endLongitude)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(endLatLng)
                                .title("Fine: ${event.eventType} - ${event.id}")
                                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(event.eventType)))
                        )
                    }
                    val firstEvent = events.first()
                    val firstLatLng = LatLng(firstEvent.startLatitude, firstEvent.startLongitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 10f))
                }
            }
        }
    }

    private fun getMarkerColor(eventType: String): Float {
        return when (eventType) {
            "Camminata" -> BitmapDescriptorFactory.HUE_GREEN
            "Corsa" -> BitmapDescriptorFactory.HUE_RED
            "Bicicletta" -> BitmapDescriptorFactory.HUE_ORANGE
            "Automobile" -> BitmapDescriptorFactory.HUE_MAGENTA
            else -> BitmapDescriptorFactory.HUE_YELLOW
        }
    }

    private fun getColorFromMarker(eventType: String): Int {
        return when (eventType) {
            "Camminata" -> Color.GREEN
            "Corsa" -> Color.RED
            "Bicicletta" -> Color.parseColor("#FFA500") // Orange
            "Automobile" -> Color.MAGENTA
            else -> Color.YELLOW
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
