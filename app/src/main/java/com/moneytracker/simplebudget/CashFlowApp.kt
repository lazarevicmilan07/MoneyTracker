package com.moneytracker.simplebudget

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.moneytracker.simplebudget.data.preferences.LanguagePreferences
import com.moneytracker.simplebudget.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashFlowApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        val lang = LanguagePreferences.getLanguage(base)
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }
}
