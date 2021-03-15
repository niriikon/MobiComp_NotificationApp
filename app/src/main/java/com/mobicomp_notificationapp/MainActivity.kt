package com.mobicomp_notificationapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobicomp_notificationapp.databinding.ActivityMainBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ProfileTable
import com.mobicomp_notificationapp.db.ReminderTable
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        listView = binding.mainListView
        listView.isLongClickable = true

        // If application was started from a notification, set reminder status to seen.
        if (intent.getBooleanExtra("from_notification", false)) {
            val notif_id = intent.getIntExtra("notification_id", -1)
            if (notif_id > 0) {
                AsyncTask.execute {
                    val db = Room.databaseBuilder(
                        applicationContext,
                        AppDB::class.java,
                        "com.mobicomp_notificationapp"
                    ).fallbackToDestructiveMigration().build()
                    db.reminderDAO().setSeenStatus(notif_id, 1)
                    db.close()
                }
            }
        }

        // Check status and update list.
        checkLoginStatus()
        refreshListView()

        // Assign login status to -1, signifying status of no logged in users.
        binding.btnLogout.setOnClickListener {
            applicationContext.getSharedPreferences(
                getString(R.string.sharedPreference),
                Context.MODE_PRIVATE
            ).edit().putInt("UserID", -1).apply()

            startActivity(
                Intent(applicationContext, LoginActivity::class.java)
            )
            finish()
        }

        // Add new reminder. No extra information given.
        binding.btnNewItem.setOnClickListener {
            startActivity(
                Intent(applicationContext, ReminderActivity::class.java)
            )
        }

        // Edit username.
        binding.showUsername.setOnClickListener {
            startActivity(
                Intent(applicationContext, ProfileActivity::class.java)
            )
        }

        binding.btnShowAll.setOnClickListener {
            val visib = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityAll", -1)
            if (visib == 1) {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityAll", 0).apply()
                binding.btnShowAll.setBackgroundColor(getResources().getColor(R.color.teal_700))
                if (applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityTimed", 1) == 1) {
                    binding.btnShowTimed.setBackgroundColor(getResources().getColor(R.color.grey_600))
                }
                if (applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityArea", -1) == 1) {
                    binding.btnShowByArea.setBackgroundColor(getResources().getColor(R.color.grey_600))
                }
            }
            else {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityAll", 1).apply()
                binding.btnShowAll.setBackgroundColor(getResources().getColor(R.color.grey_600))
                binding.btnShowTimed.setBackgroundColor(getResources().getColor(R.color.teal_700))
                binding.btnShowByArea.setBackgroundColor(getResources().getColor(R.color.teal_700))
            }
            refreshListView()
        }

        binding.btnShowTimed.setOnClickListener {
            val visib = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityTimed", 1)
            if (visib == 1) {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityTimed", 0).apply()
                binding.btnShowTimed.setBackgroundColor(getResources().getColor(R.color.teal_700))
            }
            else {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityTimed", 1).apply()
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityAll", 0).apply()
                binding.btnShowAll.setBackgroundColor(getResources().getColor(R.color.teal_700))
                binding.btnShowTimed.setBackgroundColor(getResources().getColor(R.color.grey_600))
            }
            refreshListView()
        }

        binding.btnShowByArea.setOnClickListener {
            val visib = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityArea", -1)
            if (visib == 1) {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityArea", 0).apply()
                binding.btnShowByArea.setBackgroundColor(getResources().getColor(R.color.teal_700))
            }
            else {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityArea", 1).apply()
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("visibilityAll", 0).apply()
                binding.btnShowAll.setBackgroundColor(getResources().getColor(R.color.teal_700))
                binding.btnShowByArea.setBackgroundColor(getResources().getColor(R.color.grey_600))
            }
            refreshListView()
        }

        // Select longpressed item for edit. Reminders ID given as extra information.
        listView.setOnItemLongClickListener(AdapterView.OnItemLongClickListener { _, _, position, id ->
            val selectedReminder = listView.adapter.getItem(position) as ReminderTable
            startActivity(Intent(this, ReminderActivity::class.java).apply {putExtra("selectedReminderID", selectedReminder.id)})
            true
        })
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
        refreshListView()
    }

    private fun checkLoginStatus() {
        val task = CheckLoginStatus()
        task.execute()
    }

    private fun refreshListView() {
        val task = LoadReminderEntries()
        task.execute()
    }

    /*
    * Get reminder items for the listview. Example taken from exercise.
    * */
    inner class LoadReminderEntries : AsyncTask<String?, String?, List<ReminderTable>>() {
        override fun doInBackground(vararg params: String?): List<ReminderTable> {
            val userID = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1)
            val visibAll = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityAll", -1)
            val visibTimed = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityTimed", 1)
            val visibByArea = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("visibilityArea", -1)

            var reminderItems = emptyList<ReminderTable>()
            if (userID > 0) {
                val db = Room.databaseBuilder(
                    applicationContext, AppDB::class.java, getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                //val reminderItems = db.reminderDAO().getAll()

                if (visibAll == 1 || (visibTimed != 1 && visibByArea != 1)) {
                    reminderItems = db.reminderDAO().getRemindersByUser(userID)
                }
                else if (visibTimed == 1 && visibByArea != 1) {
                    reminderItems = db.reminderDAO().getRemindersBeforeDate(userID, GregorianCalendar.getInstance().time)
                }
                else if (visibTimed == 1 && visibByArea == 1) {
                    reminderItems = db.reminderDAO().getRemindersBeforeDate(userID, GregorianCalendar.getInstance().time)
                    // reminderItems = db.reminderDAO().getRemindersBeforeDateInArea(userID, GregorianCalendar.getInstance().time, areaLimit)
                }
                else {
                    reminderItems = db.reminderDAO().getRemindersByUser(userID)
                    // reminderItems = db.reminderDAO().getRemindersInArea(userID, areaLimit)
                }
                db.close()
            }
            else {
                Log.d("DB operations", "No user signed in, cannot load reminder entries.")
            }
            return reminderItems
        }

        override fun onPostExecute(reminderItems: List<ReminderTable>?) {
            super.onPostExecute(reminderItems)
            if (reminderItems != null) {
                if (reminderItems.isNotEmpty()) {
                    val adaptor = ReminderAdaptor(applicationContext, reminderItems)
                    listView.adapter = adaptor
                }
                else {
                    listView.adapter = null
                }
            }
        }
    }

    /*
    * Check login status from shared preferences.
    * -1 signifies no logged in users. If user is logged in, show username at top bar.
    * */
    inner class CheckLoginStatus: AsyncTask<String?, String?, ProfileTable>() {

        override fun doInBackground(vararg params: String?): ProfileTable {
            val userID = applicationContext.getSharedPreferences(getString(R.string.sharedPreference),
                    Context.MODE_PRIVATE).getInt("UserID", -1)

            if (userID != -1) {
                val db = Room.databaseBuilder(
                        applicationContext, AppDB::class.java, getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                val profile = db.profileDAO().getProfile(userID)
                db.close()
                return profile
            }
            else {
                return ProfileTable(id=null, username="", password="", realname="")
            }
        }

        override fun onPostExecute(profile: ProfileTable) {
            if (profile.id != null) {
                binding.showUsername.text = profile.username
            }
            else {
                binding.showUsername.text = "No user"
                startActivity(Intent(applicationContext, LoginActivity::class.java))
                finish()
            }
        }
    }

    companion object {

        fun showNofitication(context: Context, notificationId: Int, title: String, message: String, icon: Int) {

            val CHANNEL_ID = "MOBICOMP-REMINDER_NOTIFICATION_CHANNEL"


            //startActivity(Intent(this, ReminderActivity::class.java).apply {putExtra("selectedReminderID", selectedReminder.id)})
            // Example from developer.android.com
            val mainIntent = Intent(context, MainActivity::class.java)
            mainIntent.putExtra("from_notification", true)
            mainIntent.putExtra("reminder_id", notificationId)
            val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(mainIntent)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }


            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.calendar_notification)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(CHANNEL_ID)

            if (icon != -1) {
                notificationBuilder.setLargeIcon(AppCompatResources.getDrawable(context, icon)!!.toBitmap())
            }

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Notification channel needed since Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        // notificationId: Int, title: String, message: String, icon: Int?
        fun setReminderWithWorkManager(
                context: Context,
                id: Int,
                timeInMillis: Long,
                message: String,
                title: String,
                icon: Int?
        ): UUID {

            val icon_in: Int
            if (icon == null) {
                icon_in = -1
            }
            else {
                icon_in = icon
            }

            val reminderParameters = Data.Builder()
                    .putString("message", message)
                    .putString("title", title)
                    .putInt("id", id)
                    .putInt("icon", icon_in)
                    .build()

            var millisFromNow = 0L
            if (timeInMillis > System.currentTimeMillis())
                millisFromNow = timeInMillis - System.currentTimeMillis()

            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(reminderParameters)
                    .setInitialDelay(millisFromNow, TimeUnit.MILLISECONDS)
                    .build()

            val reminder_uuid = reminderRequest.getId()
            WorkManager.getInstance(context).enqueue(reminderRequest)
            return reminder_uuid
        }

        /*
        fun setReminder(context: Context, uid: Int, timeInMillis: Long, message: String) {
            val intent = Intent(context, ReminderReceiver::class.java)
            intent.putExtra("uid", uid)
            intent.putExtra("message", message)

            // create a pending intent to a  future action with a uniquie request code i.e uid
            val pendingIntent =
                    PendingIntent.getBroadcast(context, uid, intent, PendingIntent.FLAG_ONE_SHOT)

            //create a service to moniter and execute the fure action.
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC, timeInMillis, pendingIntent)
        }
        */

        fun cancelReminder(context: Context, notificationId: UUID) {
            WorkManager.getInstance(context).cancelWorkById(notificationId)
        }
    }
}
