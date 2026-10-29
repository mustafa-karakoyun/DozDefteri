package app.dozdefteri.domain

import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DoseGeneratorTest {

    private val generator = DoseGenerator()
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val berlin = ZoneId.of("Europe/Berlin")
    private val newYork = ZoneId.of("America/New_York")

    private fun schedule(
        type: ScheduleType = ScheduleType.DAILY,
        times: List<String> = listOf("08:00"),
        mask: Int = 0,
        interval: Int = 1,
        start: LocalDate = LocalDate.of(2026, 1, 1),
        end: LocalDate? = null
    ) = ScheduleEntity(
        id = 1,
        medicationId = 1,
        type = type,
        times = times.map(LocalTime::parse),
        daysOfWeekMask = mask,
        intervalDays = interval,
        startDate = start,
        endDate = end
    )

    private fun at(text: String, zone: ZoneId): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun localDates(result: List<Instant>, zone: ZoneId) = result.map { it.atZone(zone).toLocalDate() }

    @Test
    fun daily_twoTimes_threeDays_producesSixDoses() {
        val result = generator.generate(
            schedule(times = listOf("08:00", "20:00")),
            at("2026-11-02T00:00", istanbul),
            at("2026-11-05T00:00", istanbul),
            istanbul
        )
        assertThat(result).hasSize(6)
        assertThat(result.first()).isEqualTo(at("2026-11-02T08:00", istanbul))
        assertThat(result.last()).isEqualTo(at("2026-11-04T20:00", istanbul))
        assertThat(result).isInOrder()
    }

    @Test
    fun weekdays_mondayWednesdayFriday_onlyThoseDays() {
        val mask = DoseGenerator.maskOf(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        // 2026-11-02 is a Monday.
        val result = generator.generate(
            schedule(type = ScheduleType.WEEKDAYS, mask = mask),
            at("2026-11-02T00:00", istanbul),
            at("2026-11-16T00:00", istanbul),
            istanbul
        )
        assertThat(result.map { it.atZone(istanbul).dayOfWeek }.toSet())
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertThat(result).hasSize(6)
    }

    @Test
    fun everyNDays_countsFromStartDate_andExcludesDaysBeforeStart() {
        val start = LocalDate.of(2026, 11, 3)
        val result = generator.generate(
            schedule(type = ScheduleType.EVERY_N_DAYS, interval = 3, start = start),
            at("2026-11-01T00:00", istanbul),
            at("2026-11-13T00:00", istanbul),
            istanbul
        )
        assertThat(localDates(result, istanbul)).containsExactly(
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 6),
            LocalDate.of(2026, 11, 9),
            LocalDate.of(2026, 11, 12)
        ).inOrder()
    }

    @Test
    fun endDate_isInclusive_andLaterDaysExcluded() {
        val result = generator.generate(
            schedule(start = LocalDate.of(2026, 11, 1), end = LocalDate.of(2026, 11, 3)),
            at("2026-11-01T00:00", istanbul),
            at("2026-11-10T00:00", istanbul),
            istanbul
        )
        assertThat(localDates(result, istanbul)).containsExactly(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3)
        ).inOrder()
    }

    @Test
    fun asNeeded_isAlwaysEmpty() {
        val result = generator.generate(
            schedule(type = ScheduleType.AS_NEEDED, times = emptyList()),
            at("2026-11-01T00:00", istanbul),
            at("2026-11-10T00:00", istanbul),
            istanbul
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun fromIsInclusive_toIsExclusive() {
        val result = generator.generate(
            schedule(times = listOf("08:00")),
            at("2026-11-02T08:00", istanbul),
            at("2026-11-03T08:00", istanbul),
            istanbul
        )
        assertThat(result).containsExactly(at("2026-11-02T08:00", istanbul))
    }

    @Test
    fun monthEnd_andLeapDay2028() {
        val monthEnd = generator.generate(
            schedule(),
            at("2026-01-30T00:00", istanbul),
            at("2026-02-02T00:00", istanbul),
            istanbul
        )
        assertThat(localDates(monthEnd, istanbul)).containsExactly(
            LocalDate.of(2026, 1, 30),
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 1)
        ).inOrder()

        val leap = generator.generate(
            schedule(start = LocalDate.of(2028, 1, 1)),
            at("2028-02-28T00:00", istanbul),
            at("2028-03-02T00:00", istanbul),
            istanbul
        )
        assertThat(localDates(leap, istanbul)).containsExactly(
            LocalDate.of(2028, 2, 28),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(2028, 3, 1)
        ).inOrder()
    }

    @Test
    fun berlin_springForward_gapTimeMovesForwardOnce() {
        // 2026-03-29 02:00 -> 03:00 in Europe/Berlin.
        val result = generator.generate(
            schedule(times = listOf("02:30")),
            at("2026-03-29T00:00", berlin),
            at("2026-03-30T00:00", berlin),
            berlin
        )
        assertThat(result).hasSize(1)
        val local = result.single().atZone(berlin)
        assertThat(local.toLocalTime()).isEqualTo(LocalTime.of(3, 30))
        assertThat(local.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 29))
    }

    @Test
    fun berlin_fallBack_ambiguousTimeOnceWithEarlierOffset() {
        // 2026-10-25 03:00 -> 02:00 in Europe/Berlin; 02:30 happens twice.
        val result = generator.generate(
            schedule(times = listOf("02:30")),
            at("2026-10-25T00:00", berlin),
            at("2026-10-26T00:00", berlin),
            berlin
        )
        assertThat(result).hasSize(1)
        val expected = ZonedDateTime.of(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30), berlin)
        assertThat(result.single()).isEqualTo(expected.withEarlierOffsetAtOverlap().toInstant())
        assertThat(expected.withEarlierOffsetAtOverlap().offset.totalSeconds).isEqualTo(2 * 3600)
    }

    @Test
    fun newYork_springForwardAndFallBack_eachDoseOnce() {
        val spring = generator.generate(
            schedule(times = listOf("02:30", "08:00")),
            at("2026-03-08T00:00", newYork),
            at("2026-03-09T00:00", newYork),
            newYork
        )
        assertThat(spring.map { it.atZone(newYork).toLocalTime() })
            .containsExactly(LocalTime.of(3, 30), LocalTime.of(8, 0)).inOrder()

        val fall = generator.generate(
            schedule(times = listOf("01:30")),
            at("2026-11-01T00:00", newYork),
            at("2026-11-02T00:00", newYork),
            newYork
        )
        assertThat(fall).hasSize(1)
    }

    @Test
    fun istanbul_noDst_simpleCase() {
        val result = generator.generate(
            schedule(times = listOf("09:00")),
            at("2026-03-29T00:00", istanbul),
            at("2026-03-30T00:00", istanbul),
            istanbul
        )
        assertThat(result).containsExactly(Instant.parse("2026-03-29T06:00:00Z"))
    }

    @Test
    fun result_isSortedAndDistinct() {
        val result = generator.generate(
            schedule(times = listOf("20:00", "08:00", "08:00")),
            at("2026-11-02T00:00", istanbul),
            at("2026-11-04T00:00", istanbul),
            istanbul
        )
        assertThat(result).containsNoDuplicates()
        assertThat(result).isInOrder()
        assertThat(result).hasSize(4)
    }
}
