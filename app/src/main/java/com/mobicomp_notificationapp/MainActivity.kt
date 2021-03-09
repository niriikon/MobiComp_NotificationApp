package com.mobicomp_notificationapp

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityMainBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ProfileTable
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

        // Check status and update list.
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

        // Select longpressed item for edit. Reminders ID given as extra information.
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
            val db = Room.databaseBuilder(
                applicationContext, AppDB::class.java, getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()
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
                startActivity(Intent(applicationContext, LoginActivity::class.java))
                finish()
                return ProfileTable(uid=null, username="", password="", realname="")
            }
        }

        override fun onPostExecute(profile: ProfileTable) {
            if (profile.uid != null) {
                binding.showUsername.text = profile.username
            }
            else {
                binding.showUsername.text = "No user"
            }
        }
    }



}
