package com.mobicomp_notificationapp

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityLoginBinding
import com.mobicomp_notificationapp.db.AppDB

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnLogin.setOnClickListener {

            val userID = attemptLogin(binding.txtUserName.text.toString(), binding.txtPassword.text.toString())
            if (userID != -1){
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", userID).apply()
                startActivity (
                    Intent(applicationContext, MainActivity::class.java)
                )
            }
            else {
                applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", -1).apply()
                val toast = Toast.makeText(this, "Invalid credentials", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(
                Intent(applicationContext, ProfileActivity::class.java)
            )
        }
    }

    private fun attemptLogin(user: String, pass: String): Int {
        // Placeholder until proper login function
        var retVal = -1

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDB::class.java,
                getString(R.string.dbFileName)
            ).build()
            val profile = db.profileDAO().getProfile(db.profileDAO().getIdByName(user))
            db.close()

            if (profile.username == user && profile.password == pass) {
                retVal = profile.uid!!
            }
        }
        return retVal
    }
}