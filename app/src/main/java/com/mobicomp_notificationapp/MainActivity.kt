package com.mobicomp_notificationapp

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityMainBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ReminderTable

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

        checkLoginStatus()

        refreshListView()

        /*
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("LoginStatus", 0).apply()
            startActivity (
                Intent(applicationContext, LoginActivity::class.java)
            )
        }
        */
        binding.btnLogout.setOnClickListener {
            applicationContext.getSharedPreferences(
                getString(R.string.sharedPreference),
                Context.MODE_PRIVATE
            ).edit().putInt("UserID", -1).apply()

            startActivity(
                Intent(applicationContext, LoginActivity::class.java)
            )
        }

        binding.btnNewItem.setOnClickListener {
            startActivity(
                Intent(applicationContext, ReminderActivity::class.java)
            )
        }

        listView.setOnItemLongClickListener(AdapterView.OnItemLongClickListener { _, _, position, id ->
            val selectedReminder = listView.adapter.getItem(position) as ReminderTable
            startActivity(Intent(this, ReminderActivity::class.java).apply {putExtra("selectedReminderID", selectedReminder.id)})
            false
        })
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
        refreshListView()
    }

    private fun refreshListView() {
        var task = LoadReminderEntries()
        task.execute()
    }

    inner class LoadReminderEntries : AsyncTask<String?, String?, List<ReminderTable>>() {
        override fun doInBackground(vararg params: String?): List<ReminderTable> {
            val db = Room.databaseBuilder(
                applicationContext, AppDB::class.java, getString(R.string.dbFileName)
            ).build()
            val reminderItems = db.reminderDAO().getAll()
            db.close()
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

    private fun checkLoginStatus() {
        val userID = applicationContext.getSharedPreferences(getString(R.string.sharedPreference),
        Context.MODE_PRIVATE).getInt("UserID", -1)

        if (userID == 0) {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
    }
}
