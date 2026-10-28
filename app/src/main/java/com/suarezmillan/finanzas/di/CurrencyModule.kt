package com.suarezmillan.finanzas.di

import com.suarezmillan.finanzas.core.currency.remote.MarketRateSource
import com.suarezmillan.finanzas.core.currency.remote.RateSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CurrencyModule {

    @Binds
    @Singleton
    abstract fun bindRateSource(impl: MarketRateSource): RateSource
}
