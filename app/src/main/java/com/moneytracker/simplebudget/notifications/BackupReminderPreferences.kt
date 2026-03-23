package com.moneytracker.simplebudget.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupReminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_reminder_settings")

@Singleton
class BackupReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.backupReminderDataStore

    val settings: Flow<ReminderSettings> = store.data.map { p ->
        ReminderSettings(
            enabled       = p[ENABLED]        ?: false,
            frequency     = ReminderFrequency.fromValue(p[FREQUENCY] ?: ReminderFrequency.MONTHLY.value),
            hour          = p[HOUR]           ?: 10,
            minute        = p[MINUTE]         ?: 0,
            dayOfWeek     = p[DAY_OF_WEEK]    ?: 1,
            monthlyOption = MonthlyReminderOption.fromValue(p[MONTHLY_OPTION] ?: MonthlyReminderOption.FIRST_DAY.value)
        )
    }

    suspend fun save(settings: ReminderSettings) {
        store.edit { p ->
            p[ENABLED]        = settings.enabled
            p[FREQUENCY]      = settings.frequency.value
            p[HOUR]           = settings.hour
            p[MINUTE]         = settings.minute
            p[DAY_OF_WEEK]    = settings.dayOfWeek
            p[MONTHLY_OPTION] = settings.monthlyOption.value
        }
    }

    private companion object {
        val ENABLED        = booleanPreferencesKey("backup_reminder_enabled")
        val FREQUENCY      = intPreferencesKey("backup_reminder_frequency")
        val HOUR           = intPreferencesKey("backup_reminder_hour")
        val MINUTE         = intPreferencesKey("backup_reminder_minute")
        val DAY_OF_WEEK    = intPreferencesKey("backup_reminder_day_of_week")
        val MONTHLY_OPTION = intPreferencesKey("backup_reminder_monthly_option")
    }
}
