package co.daily.opensesame.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// Wrapper for Compose stability
@Immutable
@JvmInline
@Serializable(with = TimestampSerializer::class)
value class Timestamp(
    val value: Instant
) : Comparable<Timestamp> {
    val isInPast: Boolean
        get() = value < Instant.now()

    val isInFuture: Boolean
        get() = value > Instant.now()

    fun toEpochMilli() = value.toEpochMilli()

    operator fun plus(duration: Duration) = Timestamp(value + duration)

    operator fun minus(duration: Duration) = Timestamp(value - duration)

    operator fun minus(rhs: Timestamp) = Duration.between(rhs.value, value)

    override operator fun compareTo(other: Timestamp) = value.compareTo(other.value)

    fun toISOString(): String = DateTimeFormatter.ISO_INSTANT.format(value)

    override fun toString() = toISOString()

    fun roundToNearestMinute() = roundTo(msInMinute)

    fun roundToNearestHour() = roundTo(msInHour)

    fun roundTo(msMultiple: Long) =
        ofEpochMilli(((toEpochMilli() + msMultiple / 2) / msMultiple) * msMultiple)

    fun asCalendarDate() = value.atZone(ZoneId.systemDefault()).let {
        CalendarDate(
            year = it.year,
            month = it.monthValue,
            monthName = it.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            day = it.dayOfMonth,
            dayOfWeek = it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        )
    }

    companion object {
        fun now() = Timestamp(Instant.now())

        fun ofEpochMilli(value: Long) = Timestamp(Instant.ofEpochMilli(value))

        fun ofEpochSecs(value: Long) = ofEpochMilli(value * 1000)

        fun parse(value: CharSequence) = Timestamp(Instant.parse(value))

        fun from(date: Date) = Timestamp(date.toInstant())
    }
}

@Composable
fun formatTimer(duration: Duration): String {

    if (duration.seconds < 0) {
        return "0s"
    }

    val mins = duration.seconds / 60
    val secs = duration.seconds % 60

    return if (mins == 0L) {
        "${secs}s"
    } else {
        "${mins}m ${secs}s"
    }
}

private val msInHour = 1.hours.inWholeMilliseconds
private val msInMinute = 1.minutes.inWholeMilliseconds

data class CalendarDate(
    val year: Int,
    val month: Int,
    val monthName: String,
    val day: Int,
    val dayOfWeek: String,
) {
    fun toMenuGroup(now: Timestamp): String {

        val nowCalendar = now.asCalendarDate()

        return if (nowCalendar == this) {
            "Today"
        } else if ((now - Duration.ofDays(1)).asCalendarDate() == this) {
            "Yesterday"
        } else if (year == nowCalendar.year) {
            return "$day $monthName"
        } else {
            return "$monthName $year"
        }
    }
}

object TimestampSerializer : KSerializer<Timestamp> {
    override val descriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeString(value.toISOString())
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        return Timestamp.parse(decoder.decodeString())
    }
}
