package com.example.railubuddy.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.railubuddy.services.RailuService
import com.example.railubuddy.viewmodel.MainViewModel
import com.example.railubuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(navController: NavController, stationId: String, viewModel: MainViewModel) {
    val context = LocalContext.current
    val stations by viewModel.stations.collectAsStateWithLifecycle(initialValue = emptyList())
    val trainConfigs by viewModel.trainConfigs.collectAsStateWithLifecycle(initialValue = emptyMap())
    val isNearDestination by viewModel.isNearDestination.collectAsStateWithLifecycle(initialValue = false)
    val userSelectedCoach by viewModel.userSelectedCoach.collectAsStateWithLifecycle(initialValue = null)
    val activeTrackingId by viewModel.trackingStationId.collectAsStateWithLifecycle(initialValue = null)
    val distanceText by viewModel.distanceToDestination.collectAsStateWithLifecycle(initialValue = "Calculating...")

    val station = remember(stations, stationId) { stations.find { it.stationId == stationId } }
    val isTrackingThisStation = activeTrackingId == stationId

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(Brush.verticalGradient(listOf(RailBlue, Color(0xFF1A237E))))) {
                CenterAlignedTopAppBar(
                    title = { Text("Journey Dashboard", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            }
        },
        bottomBar = {
            if (isNearDestination && isTrackingThisStation) {
                ProximityAlertOverlay()
            }
        }
    ) { padding ->
        station?.let { curr ->
            val trainConfig = trainConfigs[curr.trainNumber] as? Map<*, *>
            val coachData = trainConfig?.get("coaches") as? Map<*, *> ?: emptyMap<String, Any>()

            // --- FORCE-MERGE LOGIC: Always shows a handful of all classes ---
            val displayedCoachList = remember(coachData) {
                val dbCoaches = coachData.keys.map { it.toString() }
                val placeholders = listOf(
                    "GS1", "GS2", "GS3",
                    "S1", "S2", "S3",
                    "D1", "D2", "C1",
                    "B1", "B2", "B3",
                    "A1", "A2",
                    "H1"
                )
                (dbCoaches + placeholders).distinct().sorted()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(RailSurface)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- TARGET HEADER ---
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Surface(color = RailBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("TARGET STATION", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = RailBlue, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(curr.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.DarkGray)

                    if (isTrackingThisStation) {
                        Text(text = "Distance: $distanceText", color = RailSuccess, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }

                // --- INFO GRID ---
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumInfoCard(Modifier.weight(1f), "Platform", curr.platform.ifEmpty { "TBD" }, Icons.Default.ConfirmationNumber, RailBlue)
                    PremiumInfoCard(Modifier.weight(1f), "Crowd Status", viewModel.getCrowdLevel(curr.votes), Icons.Default.Groups, Color(0xFF673AB7))
                }

                // --- ALARM ACTION ---
                Button(
                    onClick = {
                        if (isTrackingThisStation) {
                            viewModel.setTrackingStatus(null, false)
                            context.stopService(Intent(context, RailuService::class.java))
                        } else {
                            if (userSelectedCoach == null) {
                                Toast.makeText(context, "Identify your coach first", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setTrackingStatus(stationId, true)
                                ContextCompat.startForegroundService(context, Intent(context, RailuService::class.java).apply {
                                    putExtra("STATION_NAME", curr.name)
                                    putExtra("LAT", curr.latitude)
                                    putExtra("LON", curr.longitude)
                                })
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTrackingThisStation) RailAlert else RailBlue),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(if (isTrackingThisStation) Icons.Default.AlarmOff else Icons.Default.NotificationsActive, null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isTrackingThisStation) "STOP ALARM" else "SET ALARM", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                // --- TRENDS & INSIGHTS ---
                val trend = coachData.maxByOrNull { entry ->
                    val data = entry.value as? Map<*, *>
                    (data?.get("votes") as? Long) ?: 0L
                }?.key?.toString() ?: "N/A"

                Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RailSuccess.copy(alpha = 0.2f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TipsAndUpdates, null, tint = RailSuccess)
                        Spacer(Modifier.width(12.dp))
                        Text("Most users spotted: Coach $trend", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // --- COACH SELECTION ---
                Text("COACH IDENTIFICATION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.DarkGray)
                CoachDashboard(displayedCoachList, userSelectedCoach) { viewModel.voteForCoach(curr.trainNumber, it) }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CoachDashboard(coachList: List<String>, selectedCoach: String?, onCoachSelect: (String) -> Unit) {
    val categories = listOf(
        "GENERAL" to coachList.filter { it.startsWith("GS") || it.startsWith("UR") || it.startsWith("L") },
        "SLEEPER" to coachList.filter { it.startsWith("S") && !it.startsWith("SH") },
        "SITTING / CC" to coachList.filter { it.startsWith("D") || it.startsWith("C") },
        "3AC / ECONOMY" to coachList.filter { it.startsWith("B") || it.startsWith("M") },
        "2AC" to coachList.filter { it.startsWith("A") },
        "1AC / EXEC" to coachList.filter { it.startsWith("H") || it.startsWith("E") }
    )

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        categories.forEach { (label, coaches) ->
            if (coaches.isNotEmpty()) {
                SimplifiedCoachRow(label, coaches, selectedCoach, onCoachSelect)
            }
        }
    }
}

@Composable
fun SimplifiedCoachRow(label: String, coaches: List<String>, selectedCoach: String?, onCoachSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(coaches) { coach ->
                val isSelected = selectedCoach == coach
                Surface(
                    modifier = Modifier.size(75.dp, 48.dp).clickable { onCoachSelect(coach) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) RailBlue else Color.White,
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(coach, color = if (isSelected) Color.White else RailBlue, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInfoCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.DarkGray)
        }
    }
}

@Composable
fun ProximityAlertOverlay() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = Color(0xFFFFF1F1),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, RailAlert),
        shadowElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = RailAlert, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Text("🚨 ARRIVING SOON", color = RailAlert, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}