package com.moneytracker.simplebudget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moneytracker.simplebudget.notifications.BackupReminderManager
import com.moneytracker.simplebudget.notifications.BackupReminderPreferences
import com.moneytracker.simplebudget.notifications.ReminderManager
import com.moneytracker.simplebudget.notifications.ReminderPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderPreferences: ReminderPreferences
    @Inject lateinit var reminderManager: ReminderManager
    @Inject lateinit var backupReminderPreferences: BackupReminderPreferences
    @Inject lateinit var backupReminderManager: BackupReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = reminderPreferences.settings.first()
                if (settings.enabled) reminderManager.scheduleReminder(settings)

                val backupSettings = backupReminderPreferences.settings.first()
                if (backupSettings.enabled) backupReminderManager.scheduleReminder(backupSettings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
