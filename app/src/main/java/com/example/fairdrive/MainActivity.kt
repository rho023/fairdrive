package com.example.fairdrive

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        emailText = findViewById(R.id.login_email)
        passwordText = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.email_sign_in_button)
        createAccountButton = findViewById(R.id.create_acct_button_login)
        progressBar = findViewById(R.id.login_progress)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if(currentUser != null) {
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            val email = emailText.text.toString().trim()
            val password = passwordText.text.toString().trim()

            progressBar.visibility = ProgressBar.VISIBLE

            if(email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressBar.visibility = ProgressBar.INVISIBLE

                            startActivity(Intent(this, MapsActivity::class.java))

                        } else {
                            Log.e("TAG", "Login Failed", task.exception)
                            Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        }

        createAccountButton.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }
}