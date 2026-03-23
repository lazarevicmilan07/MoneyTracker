package com.moneytracker.simplebudget.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BackupReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val backupReminderManager: BackupReminderManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = backupReminderPreferences.settings.first()
        if (settings.enabled) {
            BackupReminderNotificationHelper.showNotification(appContext, settings.frequency)
            backupReminderManager.scheduleReminder(settings)
        }
        return Result.success()
    }
}
