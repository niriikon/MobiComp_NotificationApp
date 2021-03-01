package com.mobicomp_notificationapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import android.view.View
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityProfileBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ProfileTable
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val userID = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference),
            Context.MODE_PRIVATE
        ).getInt("UserID", -1)

        if (userID != -1) {
            // valitse muokkaamiseen/poistamiseen
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()
                val user = db.profileDAO().getProfile(userID)
                db.close()

                binding.txtEditUsername.setText(user.username)
                binding.txtEditUserRealname.setText(user.realname)
                //binding.txtEditUserPassword.setText(user.password)
            }

            binding.btnEditUserAccept.setText(R.string.edit_button)
            binding.btnEditUserDelete.visibility = View.VISIBLE
        } else {
            // Valitse napit lisäämiseen
            binding.btnEditUserAccept.setText(R.string.add_button)
            binding.btnEditUserDelete.visibility = View.GONE
        }

        binding.btnEditUserCancel.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }

        binding.btnEditUserAccept.setOnClickListener {
            if (binding.txtEditUsername.text.isEmpty() || binding.txtEditUserRealname.text.isEmpty() || binding.txtEditUserPassword.text.isEmpty()) {
                return@setOnClickListener
            }
            val userItem = ProfileTable(
                null,
                username = binding.txtEditUsername.text.toString(),
                realname = binding.txtEditUserRealname.text.toString(),
                password = binding.txtEditUserPassword.text.toString()
            )
            // TODO: Check for unique username (or change ProfileTable.username to have UNIQUE constraint)
            Log.d("DB actions", "Attempting to insert " + userItem.username + ", " + userItem.realname + ", " + userItem.password)

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()

                if (userID != -1) {
                    userItem.uid = userID
                    db.profileDAO().update(userItem)
                }
                else {
                    val uuid = db.profileDAO().insert(userItem).toInt()
                }
                db.close()
            }
            Log.d("DB actions", "Inserted row")
            finish()

            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }

        binding.btnEditUserDelete.setOnClickListener {
            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    "com.mobicomp_notificationapp"
                ).fallbackToDestructiveMigration().build()
                val uuid = db.profileDAO().delete(userID)
                db.close()
            }
            finish()

            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
    }
}