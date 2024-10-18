package com.example.activity_tracker_dv.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.activity_tracker_dv.R
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextRegisterEmail: EditText
    private lateinit var editTextRegisterPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        editTextRegisterEmail = findViewById(R.id.editTextRegisterEmail)
        editTextRegisterPassword = findViewById(R.id.editTextRegisterPassword)
        buttonRegister = findViewById(R.id.buttonRegister)

        buttonRegister.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val email = editTextRegisterEmail.text.toString().trim()
        val password = editTextRegisterPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show()
                    // Redirect to LoginActivity
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish() // Close RegisterActivity
                } else {
                    Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
