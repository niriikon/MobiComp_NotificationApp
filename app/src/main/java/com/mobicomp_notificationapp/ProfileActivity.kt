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

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Logged in user
        val userID = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference),
            Context.MODE_PRIVATE
        ).getInt("UserID", -1)

        // If logged in, edit current user.
        if (userID != -1) {
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
            }

            binding.btnEditUserAccept.setText(R.string.edit_button)
            binding.btnEditUserDelete.visibility = View.VISIBLE
        }

        // No user logged in; add new user.
        else {
            binding.btnEditUserAccept.setText(R.string.add_button)
            binding.btnEditUserDelete.visibility = View.GONE
        }

        binding.btnEditUserCancel.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }

        // If user exists, update instance. Otherwise insert a new one.
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
            Log.d("DB actions", "Attempting to insert/update " + userItem.username + ", " + userItem.realname + ", " + userItem.password)

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
                ).fallbackToDestructiveMigration().build()

                // Check is user with the same username already exists.
                val users = db.profileDAO().getUsers()
                if (userItem.username in users) {

                    // TODO: Toast cannot be used within another thread, must be handled differently
                    //val toast = Toast.makeText(applicationContext, "Username already taken", Toast.LENGTH_LONG)
                    //toast.show()

                    Log.d("DB actions", "Username already taken, insert aborted")
                } else {
                    if (userID != -1) {
                        userItem.id = userID
                        db.profileDAO().update(userItem)
                    } else {
                        val uuid = db.profileDAO().insert(userItem).toInt()
                    }
                }
                db.close()
            }

            Log.d("DB actions", "Inserted/Updated row")

            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }

        // Delete user and set login status to -1
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
            applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", -1).apply()

            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }
    }
}