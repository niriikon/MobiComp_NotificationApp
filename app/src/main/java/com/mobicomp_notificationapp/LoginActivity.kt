package com.mobicomp_notificationapp

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.room.Room
import com.mobicomp_notificationapp.databinding.ActivityLoginBinding
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.ProfileTable

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.progressBar.visibility = View.GONE

        binding.btnLogin.setOnClickListener {
            attemptLogin(binding.txtUserName.text.toString(), binding.txtPassword.text.toString())
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(
                Intent(applicationContext, ProfileActivity::class.java)
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val id = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1)
        if (id > 0) {
            startActivity(
                Intent(applicationContext, MainActivity::class.java)
            )
        }
    }

    private fun attemptLogin(name: String, pass: String) {
        val task = AttemptLogin()
        task.execute(name, pass)
    }

    inner class AttemptLogin: AsyncTask<String?, String?, ProfileTable>() {
        /*
        * Asynchronous operations for checking login credentials and updating
        * user id to login status.
        * */

        private var user: String? = ""
        private var pass: String? = ""

        override fun onPreExecute() {
            binding.progressBar.visibility = View.VISIBLE
        }

        // Get userID by given username. If user does not exist, return blank instance.
        override fun doInBackground(vararg params: String?): ProfileTable {
            val db = Room.databaseBuilder(
                    applicationContext, AppDB::class.java, getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()

            try {
                this.user = params[0]
                this.pass = params[1]
                var profile = ProfileTable(uid=null, username="", password="", realname="")

                if (user != null && pass != null) {
                    Log.d("Login info", "User: " + user + "  pass: " + pass)

                    val id = db.profileDAO().getIdByName(user!!)

                    Log.d("Login info", "UserID is: " + id.toString())
                    if (id == 0) {
                        Log.d("Login info", "No such user")
                    }
                    else {
                        profile = db.profileDAO().getProfile(id)
                    }
                    db.close()
                }
                else {
                    Log.d("Login info", "Not enough parameters given.")
                }
                return profile
            }
            catch (e: IndexOutOfBoundsException) {
                Log.d("Login info", "Not enough parameters given.")
                return ProfileTable(uid=null, username="", password="", realname="")
            }
        }

        // Check credentials and update login status with userID
        override fun onPostExecute(profile: ProfileTable) {
            var userID = -1
            if (profile.uid != null) {
                Log.d("Login DB info", "User: " + profile.username + "  pass: " + profile.password)
                Log.d("Login comparison", "User: " + (this.user == profile.username).toString() + "  pass: " + (this.pass == profile.password).toString())
                if (profile.username == this.user && profile.password == this.pass) {
                    userID = profile.uid!!
                    binding.progressBar.visibility = View.GONE
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", userID).apply()
                    startActivity (
                            Intent(applicationContext, MainActivity::class.java)
                    )
                }
                else {
                    Log.d("Login info", "Invalid password")
                    val toast = Toast.makeText(applicationContext, "Invalid credentials", Toast.LENGTH_LONG)
                    toast.show()
                }
            }
            else {
                Log.d("Login info", "User not found")
                val toast = Toast.makeText(applicationContext, "Invalid credentials", Toast.LENGTH_LONG)
                toast.show()
            }
            applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", userID).apply()
            binding.progressBar.visibility = View.GONE
        }
    }
}