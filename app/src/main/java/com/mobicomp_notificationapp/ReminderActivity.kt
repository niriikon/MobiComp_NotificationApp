package com.mobicomp_notificationapp

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
//import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityReminderBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ReminderTable
import java.util.*


class ReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReminderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //Log.d("intent URI", intent.toUri(0))

        val reminderID = intent.getIntExtra("selectedReminderID", -1)

        if (reminderID != -1) {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).build()
                val reminder = db.reminderDAO().getReminder(reminderID)
                db.close()

                binding.txtEditReminderTitle.setText(reminder.title)
                binding.txtEditReminderDate.setText(reminder.datetime)
                binding.txtEditReminderDesc.setText(reminder.description)
            }

            binding.btnEditReminderAccept.setText(R.string.edit_button)
            binding.btnEditReminderDelete.visibility = View.VISIBLE
        }
        else {
            binding.btnEditReminderAccept.setText(R.string.add_button)
            binding.btnEditReminderDelete.visibility = View.GONE
        }

        binding.btnEditReminderCancel.setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

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
            val dateparts = reminderItem.datetime.split(".").toTypedArray()
            val calendar = GregorianCalendar(
                dateparts[2].toInt(),
                dateparts[1].toInt() - 1,
                dateparts[0].toInt()
            )

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).build()
                val uuid = db.reminderDAO().insert(reminderItem).toInt()
                db.close()
            }
            finish()

            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        binding.btnEditReminderDelete.setOnClickListener {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    "com.mobicomp_notificationapp"
                ).build()
                val uuid = db.reminderDAO().delete(reminderID)
                db.close()
            }
            finish()

            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }
}