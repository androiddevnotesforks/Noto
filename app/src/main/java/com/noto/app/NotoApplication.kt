package com.noto.app

import android.app.Application
import com.noto.app.di.appModule
import com.noto.app.di.localDataSourceModule
import com.noto.app.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class NotoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        startKoin {
            androidContext(this@NotoApplication)
            androidLogger(Level.ERROR)
            modules(
                appModule,
                repositoryModule,
                localDataSourceModule
            )
        }
    }
}