package com.mobicomp_notificationapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/*
* Example from exercises
* */
class ReminderWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext,workerParameters) {

    override fun doWork(): Result {
        val id = inputData.getInt("id", -1)
        val title = inputData.getString("title")
        val message = inputData.getString("message")
        val icon = inputData.getInt("icon", -1)
        var retVal = Result.failure()

        if (id != -1 && title != null && message != null) {
            MainActivity.showNofitication(applicationContext, id, title, message, icon)
            retVal = Result.success()
        }

        return retVal
    }
}