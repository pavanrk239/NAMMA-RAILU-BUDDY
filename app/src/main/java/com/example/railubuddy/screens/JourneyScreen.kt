package com.example.railubuddy.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.railubuddy.viewmodel.MainViewModel
import com.example.railubuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(navController: NavController, viewModel: MainViewModel, trainNumber: String) {

    val stations by viewModel.stations.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedStationId by viewModel.selectedStationId.collectAsStateWithLifecycle(initialValue = "No Station Selected")
    val activeTrackingId by viewModel.trackingStationId.collectAsStateWithLifecycle(initialValue = null)
    val trackedDistance by viewModel.distanceToTrackedStation.collectAsStateWithLifecycle(initialValue = "Calculating...")

    val trainStops = remember(stations, trainNumber) {
        stations.filter { it.trainNumber == trainNumber }.sortedBy { it.routeOrder }
    }

    // TRUTH SOURCE: Find where the train actually is
    val liveStationIndex = remember(trainStops, activeTrackingId) {
        trainStops.indexOfFirst { it.stationId == activeTrackingId }.coerceAtLeast(0)
    }

    val isTrackingThisTrain = remember(stations, activeTrackingId, trainNumber) {
        stations.find { it.stationId == activeTrackingId }?.trainNumber == trainNumber
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(Brush.verticalGradient(listOf(RailBlue, Color(0xFF1A237E))))) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Route Timeline", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                            val trainName = trainStops.firstOrNull()?.trainName ?: "Train #$trainNumber"
                            Text(trainName, style = MaterialTheme.typography.labelSmall, color = Color.Cyan.copy(alpha = 0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            }
        },
        containerColor = RailSurface
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- SMART TRACKING DASHBOARD ---
            if (activeTrackingId != null && isTrackingThisTrain) {
                val trackedStationName = remember(stations, activeTrackingId) {
                    stations.find { it.stationId == activeTrackingId }?.name ?: "Target"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = Color.Black,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(color = RailSuccess.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(44.dp)) {}
                            Icon(Icons.Default.Radar, null, tint = RailSuccess, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("DESTINATION MONITOR", color = RailSuccess, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                            Text(text = trackedDistance, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // --- LIVE TIMELINE LIST ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                itemsIndexed(trainStops) { index, station ->
                    val isUserSelected = station.stationId == selectedStationId
                    val isActuallyPassed = index < liveStationIndex
                    val isLiveNow = index == liveStationIndex

                    TimelineItem(
                        name = station.name,
                        arrival = station.scheduledArrival,
                        platform = station.platform,
                        isSelected = isUserSelected,
                        isLiveNow = isLiveNow,
                        isPassed = isActuallyPassed,
                        isLast = index == trainStops.size - 1,
                        onClick = {
                            viewModel.selectStation(station.stationId)
                            navController.navigate("station_screen/${station.stationId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    name: String,
    arrival: String,
    platform: String,
    isSelected: Boolean,
    isLiveNow: Boolean,
    isPassed: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

        // --- THE TRACK (Logic Fixed) ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (isLiveNow) {
                    Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = RailSuccess.copy(alpha = 0.15f)) {}
                }

                Surface(
                    modifier = Modifier.size(if (isLiveNow) 24.dp else 14.dp),
                    shape = CircleShape,
                    color = when {
                        isLiveNow -> RailSuccess
                        isPassed -> RailBlue
                        else -> Color.LightGray.copy(alpha = 0.8f)
                    },
                    border = if (isLiveNow) BorderStroke(2.dp, Color.White) else null,
                    shadowElevation = if (isLiveNow) 6.dp else 0.dp
                ) {
                    if (isLiveNow) Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }

            if (!isLast) {
                // The line below a station should only be Blue if that station is ALREADY passed.
                val connectorColor = if (isPassed) RailBlue else Color.LightGray.copy(alpha = 0.4f)

                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .drawBehind {
                            if (!isPassed) {
                                // Future track: Dashed
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, size.height),
                                    strokeWidth = size.width,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                )
                            } else {
                                // Passed track: Solid
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, size.height),
                                    strokeWidth = size.width
                                )
                            }
                        }
                )
            }
        }

        // --- THE STATION CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, end = 8.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RailBlue else Color.LightGray.copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(if (isSelected) 10.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isPassed) Color.Gray else Color.Black,
                        modifier = Modifier.weight(1f),
                        letterSpacing = 0.5.sp
                    )

                    if (isLiveNow) {
                        Surface(color = RailSuccess, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "AT STATION",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isSelected) RailBlue else RailBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ConfirmationNumber,
                                null,
                                tint = if (isSelected) Color.White else RailBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "PF $platform",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else RailBlue
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(arrival, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.weight(1f))

                    Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}