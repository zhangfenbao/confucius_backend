package co.daily.opensesame.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.Duration

private val rtcFlowSecs = flow {
    while(true) {
        val now = Timestamp.now().toEpochMilli()

        val rounded = ((now + 500) / 1000) * 1000
        emit(Timestamp.ofEpochMilli(rounded))

        val target = rounded + 1000
        delay(target - now)
    }
}

@Composable
fun rtcStateSecs() = rtcFlowSecs.collectAsState(initial = Timestamp.now())

private val rtcFlowHours = flow {
    while(true) {
        val now = Timestamp.now()

        val rounded = now.roundToNearestHour()
        emit(rounded)

        val target = rounded + Duration.ofHours(1)
        delay((target - now).toMillis())
    }
}

@Composable
fun rtcStateHours() = rtcFlowHours.collectAsState(initial = Timestamp.now().roundToNearestHour())

private val rtcFlowMins = flow {
    while(true) {
        val now = Timestamp.now()

        val rounded = now.roundToNearestMinute()
        emit(rounded)

        val target = rounded + Duration.ofMinutes(1)
        delay((target - now).toMillis())
    }
}

@Composable
fun rtcStateMins() = rtcFlowMins.collectAsState(initial = Timestamp.now().roundToNearestMinute())

