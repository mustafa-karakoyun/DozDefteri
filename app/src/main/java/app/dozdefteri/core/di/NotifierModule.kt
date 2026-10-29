package app.dozdefteri.core.di

import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.data.repository.LowStockNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotifierModule {
    @Binds
    abstract fun bindLowStockNotifier(notifier: DoseNotifier): LowStockNotifier
}
