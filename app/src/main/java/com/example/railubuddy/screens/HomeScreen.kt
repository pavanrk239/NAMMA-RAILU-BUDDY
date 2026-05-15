package com.example.railubuddy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.railubuddy.model.Station
import com.example.railubuddy.viewmodel.MainViewModel
import com.example.railubuddy.ui.theme.*
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {
    val journeys by viewModel.uniqueJourneys.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredTrains = journeys.filter {
        it.trainName.contains(searchQuery, ignoreCase = true) ||
                it.trainNumber.contains(searchQuery, ignoreCase = true)
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(RailBlue, Color(0xFF1A237E))
    )

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(headerGradient)) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Namma Railu Buddy", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                            // FIXED: Just "PASSENGER GUIDE"
                            Text("PASSENGER GUIDE", style = MaterialTheme.typography.labelSmall, color = Color.Cyan.copy(alpha = 0.8f), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = RailSurface
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                placeholder = { Text("Search Train Name or Number...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = RailBlue) },
                trailingIcon = { if(searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RailBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.FlashOn, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Live Board: Active Trains", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.DarkGray)
            }

            if (filteredTrains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(80.dp), tint = Color.LightGray.copy(alpha = 0.5f))
                        Text("No active trains found", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredTrains) { train ->
                        PremiumTrainCard(
                            train = train,
                            onClick = { navController.navigate("journey_screen/${train.trainNumber}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTrainCard(train: Station, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- HEADER: Solid RailBlue to match TopBar ---
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(RailBlue)
                .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = train.trainNumber,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = train.trainName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.weight(1f))

                    // Live Status Dot with BorderStroke fix applied
                    Surface(
                        color = RailSuccess,
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp),
                        border = BorderStroke(1.dp, Color.White)
                    ) {}
                }
            }

            // Body Section
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("FROM", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(train.origin, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(train.destination, fontWeight = FontWeight.Black, fontSize = 18.sp, color = RailBlue)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Journey Progress Line
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(6.dp).background(Color.LightGray.copy(alpha = 0.5f), CircleShape))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color.LightGray.copy(alpha = 0.5f), RailBlue, Color.LightGray.copy(alpha = 0.5f))
                                )
                            )
                    )
                    Icon(Icons.Default.Train, null, tint = RailBlue, modifier = Modifier.size(20.dp).padding(horizontal = 4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color.LightGray.copy(alpha = 0.5f), RailBlue, Color.LightGray.copy(alpha = 0.5f))
                                )
                            )
                    )
                    Box(Modifier.size(6.dp).background(RailBlue, CircleShape))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = RailSuccess, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Community Active", fontSize = 12.sp, color = RailSuccess, fontWeight = FontWeight.ExtraBold)
                    }

                    Surface(color = Color.Black, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "VIEW STOPS",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}