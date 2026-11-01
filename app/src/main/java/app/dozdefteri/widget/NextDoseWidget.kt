package app.dozdefteri.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.dozdefteri.MainActivity
import app.dozdefteri.R
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.core.format.DoseText
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.OPEN_DOSE_STATUSES
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.theme.DarkDozColors
import app.dozdefteri.ui.theme.LightDozColors
import app.dozdefteri.ui.theme.toColorScheme
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
    fun doseSync(): DoseSync
    fun clock(): Clock
}

/** What the widget shows; texts are resolved before composition. */
private sealed interface WidgetState {
    data class Locked(val text: String, val action: String) : WidgetState

    data class Next(
        val header: String,
        val wideHeader: String,
        val time: String,
        val compactNames: String,
        val lines: List<String>,
        val summary: String?,
        val eventIds: String,
        val single: Boolean
    ) : WidgetState

    data class Done(val title: String, val subtitle: String?) : WidgetState
}

/** Pro home-screen widget: only answers "what is next?" with one action (plan UI section 4, W). */
class NextDoseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = load(context)
        val openToday = launchIntent(context, MainActivity.DESTINATION_TODAY)
        val openPro = launchIntent(context, MainActivity.DESTINATION_PRO)
        val takeLabel = context.getString(R.string.action_taken)
        val takeAllLabel = context.getString(R.string.notif_action_take_all)
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                val wide = LocalSize.current.width >= WIDE.width
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(28.dp)
                        .padding(16.dp)
                        .clickable(actionStartActivity(if (state is WidgetState.Locked) openPro else openToday)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (state) {
                        is WidgetState.Locked -> LockedContent(state, openPro)
                        is WidgetState.Done -> DoneContent(state)
                        is WidgetState.Next -> if (wide) {
                            WideContent(state, if (state.single) takeLabel else takeAllLabel)
                        } else {
                            CompactContent(state, takeLabel)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LockedContent(state: WidgetState.Locked, openPro: Intent) {
        Text(state.text, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
        Spacer(GlanceModifier.height(8.dp))
        Button(
            text = state.action,
            onClick = actionStartActivity(openPro),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary
            )
        )
    }

    @Composable
    private fun DoneContent(state: WidgetState.Done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(32.dp)
            )
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text(
                    state.title,
                    style = titleStyle(16)
                )
                state.subtitle?.let {
                    Text(it, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
                }
            }
        }
    }

    @Composable
    private fun CompactContent(state: WidgetState.Next, takeLabel: String) {
        Text(state.header, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        Text(
            state.time,
            style = titleStyle(34)
        )
        Text(
            state.compactNames,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp)
        )
        Spacer(GlanceModifier.height(8.dp))
        TakeButton(takeLabel, state.eventIds)
    }

    @Composable
    private fun WideContent(state: WidgetState.Next, takeLabel: String) {
        Text(state.wideHeader, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        Spacer(GlanceModifier.height(4.dp))
        state.lines.take(MAX_LINES).forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.padding(vertical = 2.dp)) {
                Image(
                    provider = ImageProvider(R.drawable.ic_notification),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    line,
                    maxLines = 1,
                    style = titleStyle(15)
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            state.summary?.let {
                Text(
                    it,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            TakeButton(takeLabel, state.eventIds)
        }
    }

    @Composable
    private fun TakeButton(label: String, eventIds: String) {
        Button(
            text = label,
            onClick = actionRunCallback<TakeDosesAction>(actionParametersOf(TakeDosesAction.EVENT_IDS to eventIds)),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary
            )
        )
    }

    @Composable
    private fun titleStyle(size: Int) =
        TextStyle(color = GlanceTheme.colors.onSurface, fontSize = size.sp, fontWeight = FontWeight.Medium)

    private suspend fun load(context: Context): WidgetState {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        if (!entryPoint.proStatus().current()) {
            return WidgetState.Locked(
                context.getString(R.string.widget_locked),
                context.getString(R.string.notif_action_open)
            )
        }
        val clock = entryPoint.clock()
        val is24Hour = DateFormat.is24HourFormat(context)
        val profile = entryPoint.profileRepository().getActiveProfile()
            ?: return WidgetState.Done(context.getString(R.string.widget_no_more_doses), null)
        val today = clock.instant().atZone(clock.zone).toLocalDate()
        val start = today.atStartOfDay(clock.zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        val repository = entryPoint.doseRepository()
        val todayDoses = repository.getRange(profile.id, start, end)
            .filter { it.scheduleType != ScheduleType.AS_NEEDED }
        val taken = todayDoses.count { it.event.status == DoseStatus.TAKEN }
        val summary = todayDoses.takeIf { it.isNotEmpty() }
            ?.let { context.getString(R.string.widget_summary_today, taken, it.size) }

        val nextMinute = todayDoses.filter { it.event.status in OPEN_DOSE_STATUSES }
            .minOfOrNull { it.event.scheduledAt / MINUTE }
        if (nextMinute == null) {
            val upcoming = repository.nextOpenForProfile(profile.id)
            val subtitle = upcoming?.let {
                val time = Instant.ofEpochMilli(it.event.scheduledAt).atZone(clock.zone)
                if (time.toLocalDate() == today.plusDays(1)) {
                    context.getString(R.string.widget_tomorrow_first, Formats.time(time.toLocalTime(), is24Hour))
                } else {
                    null
                }
            }
            return WidgetState.Done(context.getString(R.string.today_all_done), subtitle)
        }
        val due = todayDoses.filter {
            it.event.status in OPEN_DOSE_STATUSES && it.event.scheduledAt / MINUTE == nextMinute
        }
        val time = Formats.time(Instant.ofEpochMilli(nextMinute * MINUTE).atZone(clock.zone).toLocalTime(), is24Hour)
        val first = due.first().medicationName
        return WidgetState.Next(
            header = context.getString(R.string.widget_header, profile.name),
            wideHeader = context.getString(R.string.widget_header_wide, profile.name, time),
            time = time,
            compactNames = if (due.size > 1) context.getString(R.string.widget_more, first, due.size - 1) else first,
            lines = due.map {
                DoseText.title(it.medicationName, DoseText.strength(it.strengthAmount, it.strengthUnit))
            },
            summary = summary,
            eventIds = due.joinToString(",") { it.event.id.toString() },
            single = due.size == 1
        )
    }

    private fun launchIntent(context: Context, destination: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

    companion object {
        private val SMALL = DpSize(130.dp, 130.dp)
        private val WIDE = DpSize(250.dp, 130.dp)
        private const val MINUTE = 60_000L
        private const val MAX_LINES = 3
        private val WidgetColors = ColorProviders(
            light = LightDozColors.toColorScheme(),
            dark = DarkDozColors.toColorScheme()
        )
    }
}

/** "Aldım" / "Hepsini aldım" on the widget: takes every dose of the shown time, then refreshes everything. */
class TakeDosesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ids = parameters[EVENT_IDS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (ids.isEmpty()) return
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val now = entryPoint.clock().instant()
        ids.forEach { entryPoint.doseRepository().take(it, now) }
        entryPoint.doseSync().run("widget")
        NextDoseWidget().updateAll(context)
    }

    companion object {
        val EVENT_IDS = ActionParameters.Key<String>("event_ids")
    }
}

class NextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDoseWidget()
}
