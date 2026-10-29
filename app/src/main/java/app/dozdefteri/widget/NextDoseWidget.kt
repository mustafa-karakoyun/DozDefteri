package app.dozdefteri.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.dozdefteri.MainActivity
import app.dozdefteri.R
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun profileRepository(): ProfileRepository
    fun doseRepository(): DoseRepository
    fun proStatus(): ProStatus
    fun clock(): Clock
}

/** What the widget shows; all texts are resolved before composition. */
private sealed interface WidgetContent {
    data object Locked : WidgetContent
    data class Ready(val primary: String, val secondary: String?, val summary: String?) : WidgetContent
}

/** Pro home-screen widget: the active profile's next dose and today's progress (plan section 9). */
class NextDoseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = load(context)
        val openToday = launchIntent(context, MainActivity.DESTINATION_TODAY)
        val openPro = launchIntent(context, MainActivity.DESTINATION_PRO)
        val lockedText = context.getString(R.string.widget_locked)
        provideContent {
            GlanceTheme {
                val large = LocalSize.current.height >= LARGE.height
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity(if (content is WidgetContent.Locked) openPro else openToday)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (content) {
                        WidgetContent.Locked -> Text(
                            lockedText,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp)
                        )
                        is WidgetContent.Ready -> {
                            Text(
                                content.primary,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            content.secondary?.let {
                                Text(
                                    it,
                                    maxLines = 1,
                                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp)
                                )
                            }
                            if (large && content.summary != null) {
                                Text(
                                    content.summary,
                                    modifier = GlanceModifier.padding(top = 8.dp),
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun load(context: Context): WidgetContent {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        if (!entryPoint.proStatus().current()) return WidgetContent.Locked
        val clock = entryPoint.clock()
        val noMore = context.getString(R.string.widget_no_more_doses)
        val profile = entryPoint.profileRepository().getActiveProfile()
            ?: return WidgetContent.Ready(noMore, null, null)
        val today = clock.instant().atZone(clock.zone).toLocalDate()
        val start = today.atStartOfDay(clock.zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        val doseRepository = entryPoint.doseRepository()
        val todayDoses = doseRepository.getRange(profile.id, start, end)
            .filter { it.scheduleType != ScheduleType.AS_NEEDED }
        val taken = todayDoses.count { it.event.status == DoseStatus.TAKEN }
        val summary = todayDoses.takeIf { it.isNotEmpty() }
            ?.let { context.getString(R.string.widget_summary, taken, it.size) }

        val next = doseRepository.nextOpenForProfile(profile.id)
        val nextTime = next?.let { Instant.ofEpochMilli(it.event.scheduledAt).atZone(clock.zone) }
        return if (next == null || nextTime == null || nextTime.toLocalDate() != today) {
            WidgetContent.Ready(noMore, null, summary)
        } else {
            val time = Formats.time(nextTime.toLocalTime(), DateFormat.is24HourFormat(context))
            WidgetContent.Ready(
                primary = next.medicationName,
                secondary = context.getString(
                    R.string.widget_next_at,
                    time,
                    Formats.dose(next.event.quantity, next.doseUnit)
                ),
                summary = summary
            )
        }
    }

    private fun launchIntent(context: Context, destination: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

    companion object {
        private val SMALL = DpSize(110.dp, 40.dp)
        private val LARGE = DpSize(180.dp, 110.dp)
    }
}

class NextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDoseWidget()
}
