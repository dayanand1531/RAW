package com.dayanand.raw.dataClass

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayanand.raw.ApiState
import com.dayanand.raw.dataClass.schedule.Schedule
import com.dayanand.raw.dataClass.schedule.ScheduleResponse
import com.dayanand.raw.dataClass.team.Team
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val _schudeleResponse = MutableStateFlow<ApiState<List<Schedule>>>(ApiState.Loading)
    val schudeleResponse: StateFlow<ApiState<List<Schedule>>> get() = _schudeleResponse

    private val _teamResponse = MutableStateFlow<Team?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val schedule = convertScheduleJsonToKotlin(context = context)
            _schudeleResponse.value =
                if (schedule == null) ApiState.Error("No data found") else ApiState.Success(schedule.data.schedules.reversed())

            _teamResponse.value = convertTeamJsonToKotlin(context = context)
        }
    }

    internal fun filterTeamAUrl(teamId: String): Pair<String?, String?> {

        val result = _teamResponse.value?.data?.teams
            ?.filter {
                it.tid == teamId
            }
        result?.forEach {
            Log.d("data", "Searching for teamId: $teamId, Found: ${it.logo} teamResponse ")
        }
        val item = result?.firstOrNull()
        val logo = if (item != null) {
            item.logo to item.color
        } else {
            null to null
        }
        return logo
    }

    internal fun filterTeamBUrl(teamId: String): String? {
        val result = _teamResponse.value?.data?.teams?.filter {
            it.tid == teamId
        }
        result?.forEach {
            Log.d("data", "Searching for teamId: $teamId, Found: ${it.logo} teamResponse ")
        }
        val item = result?.firstOrNull()
        val logo = item?.logo
        return logo
    }

    fun formatDateTime(date: String, pattern: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val instant = Instant.parse(date)
                val zoneId = ZoneId.of("Asia/Kolkata")
                val zonedDateTime = instant.atZone(zoneId)
                zonedDateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                    .uppercase(Locale.ENGLISH)
            } catch (e: Exception) {
                Log.d("TAG", "formatDateTime: $e")
                "INVALID DATE"
            }
        } else {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC") // Parse in UTC
                val parsedDate = sdf.parse(date)
                val outputFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
                outputFormat.format(parsedDate ?: Date()).uppercase(Locale.ENGLISH)
            } catch (e: Exception) {
                Log.d("TAG", "formatDateTime: $e")
                "INVALID DATE"
            }
        }
    }

    val currentDate: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH))
    } else {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.format(Date())
    }

    fun getFullDate(date: String): String = formatDateTime(date, "EEE MMM d")

    fun getTime(date: String): String = formatDateTime(date, "h:mm a")

    fun headerDate(date: String): String = formatDateTime(date, "MMMM yyyy")

    fun convertScheduleJsonToKotlin(context: Context): ScheduleResponse? {
        return try {
            val jsonString =
                context.assets.open("Schedule.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<ScheduleResponse>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: IOException) {
            Log.d("TAG", "convertScheduleJsonToKotlin: $e")
            e.printStackTrace()
            null
        }
    }

    fun convertTeamJsonToKotlin(context: Context): Team? {
        return try {
            val jsonString =
                context.assets.open("teams.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<Team>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: IOException) {
            Log.d("TAG", "convertScheduleJsonToKotlin: $e")
            e.printStackTrace()
            null
        }
    }

}