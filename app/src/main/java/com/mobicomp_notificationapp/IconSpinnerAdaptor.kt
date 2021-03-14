package com.mobicomp_notificationapp

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import com.mobicomp_notificationapp.databinding.ActivityReminderBinding

class IconSpinnerAdaptor : Activity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: ActivityReminderBinding
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        binding = ActivityReminderBinding.inflate(layoutInflater)

        when (parent.getItemAtPosition(pos)) {
            "Default" -> binding.imgSelectIcon.setImageResource(R.drawable.ic_baseline_add_circle_24)
            "Important" -> binding.imgSelectIcon.setImageResource(R.drawable.ic_baseline_add_circle_24)
            "Sport" -> binding.imgSelectIcon.setImageResource(R.drawable.sport_icon)
            "Study" -> binding.imgSelectIcon.setImageResource(R.drawable.study_icon)
            "Work" -> binding.imgSelectIcon.setImageResource(R.drawable.work_icon)
            else -> {
                binding.imgSelectIcon.setImageResource(R.drawable.ic_baseline_add_circle_24)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }
}
