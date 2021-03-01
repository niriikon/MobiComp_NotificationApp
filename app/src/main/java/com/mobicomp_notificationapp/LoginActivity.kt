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
            /*
            val task = AttemptLogin()
            task.execute(binding.txtUserName.text.toString(), binding.txtPassword.text.toString())
            val userID = applicationContext.getSharedPreferences("com.mobicomp_notificationapp", Context.MODE_PRIVATE).getInt("UserID", -1)
            if (userID != -1){
                startActivity (
                    Intent(applicationContext, MainActivity::class.java)
                )
            }
            else {
                val toast = Toast.makeText(this, "Invalid credentials", Toast.LENGTH_LONG)
                toast.show()
            } */

            val task = AttemptLogin()
            task.execute(binding.txtUserName.text.toString(), binding.txtPassword.text.toString())
            // val userID = applicationContext.getSharedPreferences("com.mobicomp_notificationapp", Context.MODE_PRIVATE).getInt("UserID", -1)
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(
                Intent(applicationContext, ProfileActivity::class.java)
            )
        }
    }

    inner class AttemptLogin: AsyncTask<String?, String?, ProfileTable>() {
        private var user: String? = ""
        private var pass: String? = ""

        override fun onPreExecute() {
            binding.progressBar.visibility = View.VISIBLE
        }

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
                Log.d("Login info", "No user found")
                val toast = Toast.makeText(applicationContext, "Invalid credentials", Toast.LENGTH_LONG)
                toast.show()
            }
            applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("UserID", userID).apply()
            binding.progressBar.visibility = View.GONE
        }
    }

    /*
    private fun attemptLogin(user: String, pass: String): Int {
        // Placeholder until proper login function
        Log.d("Login info", "User: " + user + "  pass: " + pass)

        /*
        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDB::class.java,
                getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()
            val profile = db.profileDAO().getProfile(db.profileDAO().getIdByName(user))
            db.close()
            Log.d("Login DB info", "User: " + profile.username + "  pass: " + profile.password)
            Log.d("Login comparison", "User: " + (user == profile.username).toString() + "  pass: " + (pass == profile.password).toString())
            if (profile.username == user && profile.password == pass) {
                //retVal = profile.uid!!
                return profile.uid
            }
            else return -1
        }
         */
        var retval = -1
        val task = RetrieveLoginProfile()
        val profile = task.execute(user)
        if (profile != null) {
            Log.d("Login DB info", "User: " + profile.username + "  pass: " + profile.password)
            Log.d("Login comparison", "User: " + (user == profile.username).toString() + "  pass: " + (pass == profile.password).toString())
            if (profile.uid != null) {
                if (profile.username == user && profile.password == pass) {
                    retval = profile.uid!!
                }
            }
            return retval
        }
    }
     */
}