package com.dayanand.raw

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.dayanand.raw.dataClass.MainViewModel
import com.dayanand.raw.dataClass.schedule.Schedule
import com.dayanand.raw.ui.theme.RawTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RawTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TeamScheduleScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TeamScheduleScreen(
    modifier: Modifier
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val scheduleResponse by mainViewModel.schudeleResponse.collectAsState()

    when (scheduleResponse) {
        is ApiState.Success -> {
            val schedule = (scheduleResponse as ApiState.Success<*>).data as List<Schedule>
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                TopBar(schedule, mainViewModel)
                //ScheduleContent(schedule, mainViewModel)
            }
        }

        is ApiState.Error -> {
            val error = (scheduleResponse as ApiState.Error).message
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error)
            }
        }

        is ApiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {}
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(schedule: List<Schedule>, mainViewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Schedule", "Games")
    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.background(Color.Black)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.background(Color.Black),
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color.Yellow else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ScheduleContent(schedule, mainViewModel)
            1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Under development", color = Color.White)
            }
        }
    }
}

@Composable
fun ScheduleContent(scheduleResponse: List<Schedule>, mainViewModel: MainViewModel) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    //filter schedule based on search query
    val filteredSchedule by remember(searchQuery, scheduleResponse) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                scheduleResponse
            } else {
                scheduleResponse.filter {
                    it.arena_name.contains(searchQuery, ignoreCase = true) ||
                            it.v.tc.contains(searchQuery, ignoreCase = true) ||
                            it.h.tc.contains(searchQuery, ignoreCase = true) ||
                            it.arena_city.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }
    val (groupedItems, lazyListState, date)
    = prepareHeader(mainViewModel, filteredSchedule)
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(searchQuery) { searchQuery = it }
        ScheduleHeader(date)
        LazyColumn(modifier = Modifier, state = lazyListState) {
            groupedItems.forEach { (_, schedule) ->
                items(schedule) {
                    when (it.st) {
                        2, 3 -> GameCard(it, mainViewModel)
                        1 -> GameCardWithButton(it, mainViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun prepareHeader(
    mainViewModel: MainViewModel,
    filteredSchedule: List<Schedule>
): Triple<Map<String, List<Schedule>>, LazyListState, String> {
    val currentDate = mainViewModel.currentDate
    val groupedItems = filteredSchedule.groupBy { it.gametime }
    val lazyListState = rememberLazyListState()
    var currentHeader by remember { mutableStateOf(groupedItems.keys.firstOrNull() ?: currentDate) }
    SideEffect {Log.d("TAG", "Value increase: $currentHeader")  }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                groupedItems.keys.firstOrNull { dateKey ->
                    groupedItems[dateKey]?.any { schedule ->
                        schedule.uid == filteredSchedule.getOrNull(firstVisibleIndex)?.uid
                    } == true
                }?.let { currentHeader = it }
            }
    }
    val date = mainViewModel.headerDate(currentHeader)
    return Triple(groupedItems, lazyListState, date)
}

@Composable
fun SearchBar(searchQuery: String, onSearchChange: (String) -> Unit) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search by Arena, Team, or City") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
        singleLine = true
    )
}

@Composable
fun ScheduleHeader(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(10.dp),
        color = Color.White,
    )
}

@Composable
fun GameCard(schedule: Schedule, mainViewModel: MainViewModel) {
    var vTeamUrl: Pair<String?, String?>? by remember { mutableStateOf(null) }
    var hTeamUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(schedule.uid) {
        vTeamUrl = mainViewModel.filterTeamAUrl(schedule.v.tid)
        hTeamUrl = mainViewModel.filterTeamBUrl(schedule.h.tid)
    }

    val date = mainViewModel.getFullDate(schedule.gametime)
    val time = mainViewModel.getTime(schedule.gametime)
    val teamA = rememberAsyncImagePainter(
        model = vTeamUrl?.first,
        placeholder = painterResource(R.drawable.ic_launcher_foreground),
        error = painterResource(R.drawable.ic_launcher_foreground)
    )

    val teamB = rememberAsyncImagePainter(
        model = hTeamUrl,
        placeholder = painterResource(R.drawable.ic_launcher_foreground),
        error = painterResource(R.drawable.ic_launcher_foreground)
    )
    val between = if (schedule.h.tid == "1610612748") "vs" to "HOME" else "@" to "AWAY"
    val state by remember {
        mutableStateOf(
            when (schedule.st) {
                1, 3 -> "${between.second} | $date | ${schedule.stt.replace("ET", "")}"
                2 -> " | $time"
                else -> ""
            }
        )
    }
    val color = vTeamUrl?.let { Color(android.graphics.Color.parseColor("#${it.second}")) }
        ?: Color.DarkGray
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
    ) {
        ConstraintLayout(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            val (teamAIcon, teamAText, detailsText, scoreText, liveText, teamBIcons, teamBText) = createRefs()

            Text(
                text = state,
                color = Color.White,
                modifier = Modifier.constrainAs(detailsText) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                fontSize = 14.sp,
            )

            Image(
                painter = teamA,
                contentDescription = "Team 1 Icon",
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(teamAIcon) {
                        top.linkTo(detailsText.bottom, 10.dp)
                        end.linkTo(scoreText.start, 20.dp)
                    }
            )
            if (schedule.st == 2) {
                Text(
                    "LIVE",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.constrainAs(liveText) {
                        top.linkTo(detailsText.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    })
            }

            Text(
                text = schedule.v.ta,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(teamAText) {
                    top.linkTo(teamAIcon.bottom, 5.dp)
                    start.linkTo(teamAIcon.start)
                    end.linkTo(teamAIcon.end)
                })

            Text(
                "${schedule.v.s} ${between.first} ${schedule.h.s}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(scoreText) {
                    top.linkTo(teamAIcon.top)
                    bottom.linkTo(teamAText.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

            Image(
                painter = teamB,
                contentDescription = "Team 2 Icon",
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(teamBIcons) {
                        top.linkTo(detailsText.bottom, 10.dp)
                        start.linkTo(scoreText.end, 20.dp)
                    }
            )
            Text(
                text = schedule.h.ta,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(teamBText) {
                    top.linkTo(teamBIcons.bottom, 5.dp)
                    start.linkTo(teamBIcons.start)
                    end.linkTo(teamBIcons.end)
                })
        }
    }
}

@Composable
fun GameCardWithButton(schedule: Schedule, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    var vTeamUrl: Pair<String?, String?>? by remember { mutableStateOf(null) }
    var hTeamUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(schedule.uid) {
        vTeamUrl = mainViewModel.filterTeamAUrl(schedule.v.tid)
        hTeamUrl = mainViewModel.filterTeamBUrl(schedule.h.tid)
    }

    val date = mainViewModel.getFullDate(schedule.gametime)
    val time = mainViewModel.getTime(schedule.gametime)
    val teamA = rememberAsyncImagePainter(
        model = vTeamUrl?.first,
        placeholder = painterResource(R.drawable.ic_launcher_foreground),
        error = painterResource(R.drawable.ic_launcher_foreground)
    )

    val teamB = rememberAsyncImagePainter(
        model = hTeamUrl,
        placeholder = painterResource(R.drawable.ic_launcher_foreground),
        error = painterResource(R.drawable.ic_launcher_foreground)
    )
    val between = if (schedule.h.tid == "1610612748") "vs" to "HOME" else "@" to "AWAY"
    val state by remember {
        mutableStateOf(
            when (schedule.st) {
                1, 3 -> "${between.second} | $date | ${schedule.stt.replace("ET", "")}"
                2 -> " | $time"
                else -> ""
            }
        )
    }
    val color = vTeamUrl?.let { Color(android.graphics.Color.parseColor("#${it.second}")) }
        ?: Color.DarkGray
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            val (teamAIcon, teamAText, detailsText, scoreText, _, teamBIcons, teamBText, ticketButton) = createRefs()

            Text(
                text = state,
                color = Color.White,
                modifier = Modifier.constrainAs(detailsText) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                fontSize = 14.sp,
            )

            Image(
                painter = teamA,
                contentDescription = "Team 1 Icon",
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(teamAIcon) {
                        top.linkTo(detailsText.bottom, 10.dp)
                        end.linkTo(teamAText.start, 10.dp)
                    }
            )

            Text(
                text = schedule.v.ta,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(teamAText) {
                    top.linkTo(teamAIcon.top)
                    bottom.linkTo(teamAIcon.bottom)
                    end.linkTo(scoreText.start, 16.dp)
                })

            Text(
                between.first,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.constrainAs(scoreText) {
                    top.linkTo(teamBText.top)
                    bottom.linkTo(teamBText.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

            Image(
                painter = teamB,
                contentDescription = "Team 2 Icon",
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(teamBIcons) {
                        top.linkTo(detailsText.bottom, 10.dp)
                        start.linkTo(teamBText.end, 10.dp)
                    }
            )
            Text(
                text = schedule.h.ta,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(teamBText) {
                    top.linkTo(teamBIcons.top)
                    bottom.linkTo(teamBIcons.bottom)
                    start.linkTo(scoreText.end, 16.dp)
                })

            Button(
                onClick = {
                    Toast.makeText(context, "Under development", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.constrainAs(ticketButton) {
                    top.linkTo(teamBIcons.bottom, 10.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
            ) {
                Text("BUY TICKETS ON ticketmaster", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    RawTheme {

    }
}

