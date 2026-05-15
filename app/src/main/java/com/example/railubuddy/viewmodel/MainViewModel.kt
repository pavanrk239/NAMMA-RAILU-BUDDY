package com.example.railubuddy.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.railubuddy.model.Station
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FirebaseDatabase.getInstance("https://railubuddy-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val stationsRef = database.getReference("stations")
    private val configsRef = database.getReference("trainConfigs")
    private val sharedPrefs = application.getSharedPreferences("railu_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations = _stations.asStateFlow()

    private val _trainConfigs = MutableStateFlow<Map<String, Any>>(emptyMap())
    val trainConfigs = _trainConfigs.asStateFlow()

    private val _selectedStationId = MutableStateFlow("No Station Selected")
    val selectedStationId = _selectedStationId.asStateFlow()

    private val _trackingStationId = MutableStateFlow<String?>(null)
    val trackingStationId = _trackingStationId.asStateFlow()

    private val _userSelectedCoach = MutableStateFlow<String?>(null)
    val userSelectedCoach = _userSelectedCoach.asStateFlow()

    // 1. ALARM DISTANCE: Stays locked to the station where the alarm is set
    val distanceToTrackedStation: StateFlow<String> = combine(_currentLocation, _stations, _trackingStationId) { location, allStations, trackingId ->
        if (location == null || trackingId == null) return@combine "GPS Waiting..."
        allStations.find { it.stationId == trackingId }?.let {
            String.format("%.1f KM", calculateDistance(location, it) / 1000)
        } ?: "Target Lost"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Calculating...")

    // 2. PREVIEW DISTANCE: Changes when you tap/select different stations
    val distanceToDestination: StateFlow<String> = combine(_currentLocation, _stations, _selectedStationId) { location, allStations, selectedId ->
        if (location == null || selectedId == "No Station Selected") return@combine "GPS Waiting..."
        allStations.find { it.stationId == selectedId }?.let {
            String.format("%.1f KM", calculateDistance(location, it) / 1000)
        } ?: "-- KM"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Calculating...")

    val liveProximityAlarm: StateFlow<Station?> = combine(_currentLocation, _stations, _trackingStationId) { location, allStations, trackingId ->
        if (location == null || allStations.isEmpty() || trackingId == null) return@combine null
        allStations.find { it.stationId == trackingId && calculateDistance(location, it) < 2000f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uniqueJourneys: StateFlow<List<Station>> = _stations.map { list ->
        list.distinctBy { it.trainNumber }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isNearDestination: StateFlow<Boolean> = combine(_currentLocation, _stations, _selectedStationId) { location, allStations, selectedId ->
        if (location == null || selectedId == "No Station Selected") return@combine false
        allStations.find { it.stationId == selectedId }?.let { calculateDistance(location, it) <= 5000f } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadOfflineStations()
        fetchStations()
        fetchTrainConfigs()
    }

    private fun fetchStations() {
        stationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Station::class.java)?.copy(stationId = it.key ?: "") }
                _stations.value = list
                sharedPrefs.edit().putString("cached_stations", gson.toJson(list)).apply()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchTrainConfigs() {
        configsRef.addValueEventListener(object : ValueEventListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value
                if (value is Map<*, *>) {
                    _trainConfigs.value = value.filterKeys { it is String } as Map<String, Any>
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setTrackingStatus(id: String?, active: Boolean) { _trackingStationId.value = if (active) id else null }
    fun updateLocation(loc: Location) { _currentLocation.value = loc }
    fun selectStation(id: String) { _selectedStationId.value = id; _userSelectedCoach.value = null }
    fun getCrowdLevel(votes: Int): String = when { votes <= 20 -> "Low"; votes <= 100 -> "Medium"; else -> "High" }

    fun voteForCoach(trainNumber: String, coachName: String) {
        val prev = _userSelectedCoach.value
        val ref = configsRef.child(trainNumber).child("coaches")
        if (prev == coachName) {
            updateVote(ref, coachName, -1)
            _userSelectedCoach.value = null
        } else {
            prev?.let { updateVote(ref, it, -1) }
            updateVote(ref, coachName, 1)
            _userSelectedCoach.value = coachName
        }
    }

    private fun updateVote(ref: DatabaseReference, coach: String, delta: Int) {
        ref.child(coach).child("votes").runTransaction(object : Transaction.Handler {
            override fun doTransaction(m: MutableData): Transaction.Result {
                val currentVotes = m.getValue(Int::class.java) ?: 0
                m.value = (currentVotes + delta).coerceAtLeast(0)
                return Transaction.success(m)
            }
            override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
        })
    }

    private fun calculateDistance(loc: Location, st: Station): Float {
        val r = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, st.latitude, st.longitude, r)
        return r[0]
    }

    private fun loadOfflineStations() {
        sharedPrefs.getString("cached_stations", null)?.let {
            _stations.value = gson.fromJson(it, object : TypeToken<List<Station>>() {}.type)
        }
    }
}