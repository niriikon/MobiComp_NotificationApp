package com.mobicomp_notificationapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityReminderBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.LocationTable
import com.mobicomp_notificationapp.db.ReminderTable
import java.text.SimpleDateFormat
import java.util.*


class ReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private lateinit var binding: ActivityReminderBinding
    private lateinit var reminderCalendar: Calendar
    private lateinit var locations: List<LocationTable>
    private lateinit var locationNames: MutableList<String>
    private val dateformatter = SimpleDateFormat("dd.MM.yyyy")
    private val timeformatter = SimpleDateFormat("HH:mm")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val spinner = binding.iconSpinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.icons_array))
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (parent.getItemAtPosition(position)) {
                    "Default" -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.default_icon)
                        binding.imgSelectIcon.setTag(R.drawable.default_icon)
                    }
                    "Important" -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.important_icon)
                        binding.imgSelectIcon.setTag(R.drawable.important_icon)
                    }
                    "Sport" -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.sport_icon)
                        binding.imgSelectIcon.setTag(R.drawable.sport_icon)
                    }
                    "Study" -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.study_icon)
                        binding.imgSelectIcon.setTag(R.drawable.study_icon)
                    }
                    "Work" -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.work_icon)
                        binding.imgSelectIcon.setTag(R.drawable.work_icon)
                    }
                    else -> {
                        binding.imgSelectIcon.setImageResource(R.drawable.default_icon)
                        binding.imgSelectIcon.setTag(R.drawable.default_icon)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        locations = listOf<LocationTable>()
        locationNames = mutableListOf()
        val locationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            locationNames)
        binding.txtEditReminderLocation.setAdapter(locationAdapter)

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDB::class.java,
                getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()
            val uid = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1)
            locations = db.locationDAO().getLocationsByUserSorted(uid)
            locationNames = db.locationDAO().getNames(uid)
            db.close()
            locationAdapter.clear()
            locationAdapter.addAll(locationNames)
            locationAdapter.notifyDataSetChanged()
            /*
            this@ReminderActivity.runOnUiThread(java.lang.Runnable {
                locationAdapter.clear()
                locationAdapter.addAll(locations)
                locationAdapter.notifyDataSetChanged()
            })
            */
        }


        binding.txtEditReminderDate.inputType = InputType.TYPE_NULL
        binding.txtEditReminderTime.inputType = InputType.TYPE_NULL
        binding.txtEditReminderDate.isClickable=true
        binding.txtEditReminderTime.isClickable=true

        binding.txtEditReminderDate.setOnClickListener {
            // TODO: Set DatePickerDialog to use firstDayOfWeek=Calendar.MONDAY somehow (or by Locale)
            reminderCalendar = GregorianCalendar.getInstance()
            if (binding.txtEditReminderDate.text.toString() != "") {
                val dateparts = binding.txtEditReminderDate.text.split(".")
                reminderCalendar.set(dateparts[2].toInt(), dateparts[1].toInt() -1, dateparts[0].toInt())
            }
            DatePickerDialog(this, this,
                    reminderCalendar.get(Calendar.YEAR),
                    reminderCalendar.get(Calendar.MONTH),
                    reminderCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.txtEditReminderTime.setOnClickListener {
            reminderCalendar = GregorianCalendar.getInstance()
            if (binding.txtEditReminderTime.text.toString() != "") {
                val timeparts = binding.txtEditReminderTime.text.split(":")
                reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())
            }
            TimePickerDialog(this, this,
                    reminderCalendar.get(Calendar.HOUR_OF_DAY),
                    reminderCalendar.get(Calendar.MINUTE),
                    true).show()
        }

        binding.txtEditReminderLocation.setOnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            val itemIndex = locationNames.indexOf(parent?.getItemAtPosition(position).toString())
            if (itemIndex >= 0) {
                val parsedLocation = "${locations[itemIndex].latitude}, ${locations[itemIndex].longitude}"
                Log.d("MobiComp_LOCATION", "parsedLocation: $parsedLocation")
                binding.txtLocationInfo.text = parsedLocation
            }
        }

        binding.imgOpenMaps.setOnClickListener {
            startActivity(Intent(applicationContext, MapsActivity::class.java))
        }

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

                binding.txtEditReminderMsg.setText(reminder.message)
                if (reminder.reminder_time != null) {
                    binding.txtEditReminderDate.setText(dateformatter.format(reminder.reminder_time!!))
                    binding.txtEditReminderTime.setText(timeformatter.format(reminder.reminder_time!!))
                }
                if (reminder.latitude != null && reminder.longitude != null) {
                    val parseLocation = "${reminder.latitude}, ${reminder.longitude}"
                    binding.txtLocationInfo.setText(parseLocation)
                }
                val icon_id = reminder.icon
                if (icon_id != null) {
                    binding.imgSelectIcon.setImageResource(icon_id)
                    binding.imgSelectIcon.tag = icon_id
                }
                else {
                    binding.imgSelectIcon.setImageResource(R.drawable.default_icon)
                    binding.imgSelectIcon.tag = R.drawable.default_icon
                }
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
            // startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        // Update reminder instance if already exists, otherwise insert new one.
        binding.btnEditReminderAccept.setOnClickListener {
            if (binding.txtEditReminderTime.text.isEmpty() || binding.txtEditReminderMsg.text.isEmpty()) {
                val toast = Toast.makeText(applicationContext, "Required field empty!", Toast.LENGTH_LONG)
                toast.show()
                return@setOnClickListener
            }

            val dateparts = binding.txtEditReminderDate.text.split(".")
            val timeparts = binding.txtEditReminderTime.text.split(":")
            reminderCalendar = GregorianCalendar(dateparts[2].toInt(), dateparts[1].toInt() - 1, dateparts[0].toInt(), timeparts[0].toInt(), timeparts[1].toInt(), 0)
            val locationparts = binding.txtLocationInfo.text.split(", ")

            val reminderItem = ReminderTable(
                null,
                profile_id = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1),
                message = binding.txtEditReminderMsg.text.toString(),
                reminder_time =  reminderCalendar.getTime(),
                creation_time = Calendar.getInstance().time,
                reminder_seen = 0,
                latitude = locationparts[0].toFloat(),
                longitude = locationparts[1].toFloat(),
                icon = binding.imgSelectIcon.getTag() as Int?,
                workmanager_uuid = null
            )

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                if (reminderID != -1) {
                    reminderItem.id = reminderID
                    if (reminderCalendar.getTime() == db.reminderDAO().getTime(reminderID)) {
                        reminderItem.workmanager_uuid = db.reminderDAO().getNotificationUUID(reminderID)
                    }
                    else {
                        var parsedMessage = "${reminderItem.message} on ${dateformatter.format(reminderCalendar.getTime())} ${timeformatter.format(reminderCalendar.getTime())}"
                        if (reminderItem.latitude != null && reminderItem.longitude != null) {
                            parsedMessage = "$parsedMessage at ${reminderItem.latitude}, ${reminderItem.longitude}"
                        }
                        MainActivity.cancelReminder(applicationContext, db.reminderDAO().getNotificationUUID(reminderID))
                        reminderItem.workmanager_uuid = MainActivity.setReminderWithWorkManager(
                            applicationContext,
                            reminderID,
                            reminderCalendar.timeInMillis,
                            parsedMessage,
                            "Upcoming event:",
                            reminderItem.icon)
                    }
                    db.reminderDAO().update(reminderItem)
                }
                else {
                    val uuid = db.reminderDAO().insert(reminderItem).toInt()
                    var parsedMessage = "${reminderItem.message} on ${dateformatter.format(reminderCalendar.getTime())} ${timeformatter.format(reminderCalendar.getTime())}"
                    if (reminderItem.latitude != null && reminderItem.longitude != null) {
                        parsedMessage = "$parsedMessage at ${reminderItem.latitude}, ${reminderItem.longitude}"
                    }
                    db.reminderDAO().setNotificationUUID(uuid,
                        MainActivity.setReminderWithWorkManager(applicationContext,
                            uuid,
                            reminderCalendar.timeInMillis, parsedMessage, "Upcoming event:", reminderItem.icon))
                }
                db.close()
            }

            // Finish current activity, and therefore return to MainActivity?
            // startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        // Delete reminder.
        binding.btnEditReminderDelete.setOnClickListener {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    "com.mobicomp_notificationapp"
                ).fallbackToDestructiveMigration().build()
                MainActivity.cancelReminder(applicationContext, db.reminderDAO().getNotificationUUID(reminderID))
                val uuid = db.reminderDAO().delete(reminderID)
                db.close()
            }

            // startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.txtLocationInfo.text = ""

        locations = listOf<LocationTable>()
        locationNames = mutableListOf()
        val locationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            locationNames)
        binding.txtEditReminderLocation.setAdapter(locationAdapter)

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDB::class.java,
                getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()
            val uid = applicationContext.getSharedPreferences(
                getString(R.string.sharedPreference),
                Context.MODE_PRIVATE
            ).getInt("UserID", -1)
            locations = db.locationDAO().getLocationsByUserSorted(uid)
            locationNames = db.locationDAO().getNames(uid)
            db.close()
            locationAdapter.clear()
            locationAdapter.addAll(locationNames)
            locationAdapter.notifyDataSetChanged()
        }
    }

    // Put selected date to EditText
    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) {
        val msg = "$day.${month + 1}.$year"
        //Log.d("MobiComp_DATE", msg)
        binding.txtEditReminderDate.setText(msg)
    }

    // Put selected time to EditText
    override fun onTimeSet(picker: TimePicker?, hour: Int, minute: Int) {
        val msg: String
        if (minute < 10) {
            msg = "$hour:0$minute"
        }
        else {
            msg = "$hour:$minute"
        }
        //Log.d("MobiComp_TIME", msg)
        binding.txtEditReminderTime.setText(msg)
    }
}
