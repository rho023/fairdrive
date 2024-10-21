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
import com.google.firebase.firestore.FirebaseFirestore

class CreateAccountActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private lateinit var emailText: EditText
    private lateinit var createAccountButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.create_acct_progress)
        usernameText = findViewById(R.id.username_account)
        emailText = findViewById(R.id.email_account)
        passwordText = findViewById(R.id.password_account)
        createAccountButton = findViewById(R.id.create_acct_button)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        createAccountButton.setOnClickListener {
            val username = usernameText.text.toString().trim()
            val email = emailText.text.toString().trim()
            val password = passwordText.text.toString().trim()

            progressBar.visibility = ProgressBar.VISIBLE

            if(email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressBar.visibility = ProgressBar.INVISIBLE

                            val user = auth.currentUser
                            val userMap = hashMapOf(
                                "username" to username,
                                "email" to email
                            )

                            if (user != null) {
                                db.collection("users").document(user.uid)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        Log.d("SignInActivity", "User data successfully saved!")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("SignInActivity", "Error saving user data", e)
                                    }
                            }

                            startActivity(Intent(this, MapsActivity::class.java))
                        } else {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            Log.e("TAG", "createUserWithEmail: Failed", task.exception)
                            Toast.makeText(this, "Account Creation Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}