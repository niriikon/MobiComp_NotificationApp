package com.mobicomp_notificationapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.mobicomp_notificationapp.databinding.ReminderItemBinding
import com.mobicomp_notificationapp.db.ReminderTable
import java.text.SimpleDateFormat

/*
* Example taken from exercises.
* */

class ReminderAdaptor(context: Context, private val list:List<ReminderTable>): BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val formatter = SimpleDateFormat("E d.M.yyyy H:mm")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val row = ReminderItemBinding.inflate(inflater, parent, false)


        // TODO: Refactor properly, now just testing
        row.txtReminderMsg.text=list[position].message
        row.txtReminderTime.text=formatter.format(list[position].reminder_time)
        row.txtReminderX.text=list[position].location_x.toString()
        row.txtReminderY.text=list[position].location_y.toString()
        val icon_id = list[position].icon
        if (icon_id != null) {
            row.imgReminderIcon.setImageResource(icon_id)
        }
        else {
            row.imgReminderIcon.setImageResource(R.drawable.default_icon)
        }

        return row.root
    }
    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }
}
