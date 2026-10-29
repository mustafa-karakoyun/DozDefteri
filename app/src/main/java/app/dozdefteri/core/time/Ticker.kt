package app.dozdefteri.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

/** Emits the current instant now and then at the start of every following minute. */
fun minuteTicker(clock: Clock): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        val millisToNextMinute = 60_000L - now.toEpochMilli() % 60_000L
        delay(millisToNextMinute)
    }
}
