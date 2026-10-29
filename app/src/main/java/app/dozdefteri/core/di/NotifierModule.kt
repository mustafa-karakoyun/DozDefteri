package app.dozdefteri.core.di

import app.dozdefteri.data.repository.LowStockNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NotifierModule {
    // Replaced by the real notification implementation in phase 2.
    @Provides
    fun provideLowStockNotifier(): LowStockNotifier = LowStockNotifier { }
}
