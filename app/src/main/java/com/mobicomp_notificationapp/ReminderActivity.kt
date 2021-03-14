package com.mobicomp_notificationapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityReminderBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ReminderTable
import java.text.SimpleDateFormat
import java.util.*


class ReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private lateinit var binding: ActivityReminderBinding
    private lateinit var reminderCalendar: Calendar
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

        binding.txtEditReminderDate.inputType = InputType.TYPE_NULL
        binding.txtEditReminderTime.inputType = InputType.TYPE_NULL
        binding.txtEditReminderDate.isClickable=true
        binding.txtEditReminderTime.isClickable=true

        binding.txtEditReminderDate.setOnClickListener {
            // TODO: Set DatePickerDialog to user firstDayOfWeek=Calendar.MONDAY somehow (or by Locale)
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

                // TODO: Refactor properly, now just testing
                binding.txtEditReminderMsg.setText(reminder.message)
                binding.txtEditReminderDate.setText(dateformatter.format(reminder.reminder_time))
                binding.txtEditReminderTime.setText(timeformatter.format(reminder.reminder_time))
                binding.txtEditReminderX.setText(reminder.location_x)
                binding.txtEditReminderY.setText(reminder.location_y)
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
            startActivity(Intent(applicationContext, MainActivity::class.java))
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

            // TODO: Refactor properly, now just testing
            val reminderItem = ReminderTable(
                    null,
                    profile_id = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1),
                    message = binding.txtEditReminderMsg.text.toString(),
                    reminder_time =  GregorianCalendar(dateparts[2].toInt(), dateparts[1].toInt() - 1, dateparts[0].toInt(), timeparts[0].toInt(), timeparts[1].toInt(), 0).getTime(),
                    //reminder_time = date_time,
                    creation_time = Calendar.getInstance().time,
                    reminder_seen = 0,
                    location_x = binding.txtEditReminderX.text.toString(),
                    location_y = binding.txtEditReminderY.text.toString(),
                    icon = binding.imgSelectIcon.getTag() as Int?
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

            startActivity(Intent(applicationContext, MainActivity::class.java))
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
                val uuid = db.reminderDAO().delete(reminderID)
                db.close()
            }

            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
    }

    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) {
        //val msg = String.format(day.toString(), ".", month.toString(), ".", year.toString())
        val msg = "$day.${month + 1}.$year"
        Log.d("MobiComp_DATE", msg)
        binding.txtEditReminderDate.setText(msg)
    }

    override fun onTimeSet(picker: TimePicker?, hour: Int, minute: Int) {
        //val msg = String.format(hour.toString(), ":", minute.toString())
        val msg = "$hour:$minute"
        Log.d("MobiComp_TIME", msg)
        binding.txtEditReminderTime.setText(msg)
    }
}

/*
class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        // Do something with the time chosen by the user
    }
}

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        // Use the current date as the default date in the picker
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        return DatePickerDialog(activity, this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        // Do something with the date chosen by the user
    }
}
*/
