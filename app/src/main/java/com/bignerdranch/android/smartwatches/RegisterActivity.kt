package com.bignerdranch.android.smartwatches

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = AppDatabase.getDatabase(this)

        val firstNameField: EditText = findViewById(R.id.first_name)
        val lastNameField: EditText = findViewById(R.id.last_name)
        val emailField: EditText = findViewById(R.id.email)
        val loginField: EditText = findViewById(R.id.register_login)
        val passwordField: EditText = findViewById(R.id.register_password)
        val registerButton: Button = findViewById(R.id.btn_register)

        registerButton.setOnClickListener {
            val login = loginField.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val existingUser = db.userDao().findByLogin(login)

                withContext(Dispatchers.Main) {
                    if (existingUser != null) {
                        showToast("Логин уже существует")
                    } else {
                        val user = User(
                            firstName = firstNameField.text.toString(),
                            lastName = lastNameField.text.toString(),
                            email = emailField.text.toString(),
                            login = login,
                            password = passwordField.text.toString()
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            db.userDao().insert(user)
                        }

                        showToast("Регистрация успешна")
                        val intent = Intent(this@RegisterActivity, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}