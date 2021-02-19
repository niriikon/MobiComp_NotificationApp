package com.mobicomp_notificationapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.mobicomp_notificationapp.databinding.ReminderItemBinding
import com.mobicomp_notificationapp.db.ReminderTable

class ReminderAdaptor(context: Context, private val list:List<ReminderTable>): BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val row = ReminderItemBinding.inflate(inflater, parent, false)

        row.txtReminderTitle.text=list[position].title
        row.txtReminderDatetime.text=list[position].datetime
        row.txtReminderDesc.text=list[position].description
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
