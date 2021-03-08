package com.mobicomp_notificationapp

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityReminderBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ReminderTable


class ReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReminderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //Log.d("intent URI", intent.toUri(0))

        // Use intent extra for knowing if user wants to add a reminder, or edit existing one.
        val reminderID = intent.getIntExtra("selectedReminderID", -1)

        // Edit existing reminder. Get reminder instance by its ID and display it for editing.
        if (reminderID > 0) {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                val reminder = db.reminderDAO().getReminder(reminderID)
                db.close()

                binding.txtEditReminderTitle.setText(reminder.title)
                binding.txtEditReminderDate.setText(reminder.datetime)
                binding.txtEditReminderDesc.setText(reminder.description)
            }

            binding.btnEditReminderAccept.setText(R.string.edit_button)
            binding.btnEditReminderDelete.visibility = View.VISIBLE
        }
        // Add new reminder.
        else {
            binding.btnEditReminderAccept.setText(R.string.add_button)
            binding.btnEditReminderDelete.visibility = View.GONE
        }

        binding.btnEditReminderCancel.setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        // Update reminder instance if already exists, otherwise insert new one.
        binding.btnEditReminderAccept.setOnClickListener {
            if (binding.txtEditReminderDate.text.isEmpty()) {
                return@setOnClickListener
            }
            val reminderItem = ReminderTable(
                null,
                title = binding.txtEditReminderTitle.text.toString(),
                description = binding.txtEditReminderDesc.text.toString(),
                datetime = binding.txtEditReminderDate.text.toString()
            )

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                if (reminderID != -1) {
                    reminderItem.id = reminderID
                    db.reminderDAO().update(reminderItem)
                }
                else {
                    val uuid = db.reminderDAO().insert(reminderItem).toInt()
                }
                db.close()
            }
            finish()

            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        // Delete reminder.
        binding.btnEditReminderDelete.setOnClickListener {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    "com.mobicomp_notificationapp"
                ).fallbackToDestructiveMigration().build()
                val uuid = db.reminderDAO().delete(reminderID)
                db.close()
            }
            finish()

            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }
}