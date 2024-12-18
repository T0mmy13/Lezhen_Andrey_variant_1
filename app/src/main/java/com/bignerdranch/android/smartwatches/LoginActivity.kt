package com.bignerdranch.android.smartwatches

import android.content.Context
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

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация базы данных
        db = AppDatabase.getDatabase(this)

        // Проверяем, сохранены ли данные пользователя
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedLogin = sharedPref.getString("login", null)
        val savedPassword = sharedPref.getString("password", null)

        if (savedLogin != null && savedPassword != null) {
            loginUser(savedLogin, savedPassword)
        } else {
            setContentView(R.layout.activity_login)

            val loginField: EditText = findViewById(R.id.login)
            val passwordField: EditText = findViewById(R.id.password)
            val loginButton: Button = findViewById(R.id.btn_login)
            val registerButton: Button = findViewById(R.id.btn_register)

            loginButton.setOnClickListener {
                val login = loginField.text.toString()
                val password = passwordField.text.toString()

                loginUser(login, password)
            }

            registerButton.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun loginUser(login: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().findByLogin(login)

            withContext(Dispatchers.Main) {
                if (user == null) {
                    showToast("Такого пользователя нет")
                } else if (user.password != password) {
                    showToast("Введён неверный пароль")
                } else {
                    // Сохраняем данные в SharedPreferences
                    val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("login", login)
                        putString("password", password)
                        apply()
                    }

                    // Переходим на экран профиля
                    val intent = Intent(this@LoginActivity, ProfileActivity::class.java).apply {
                        putExtra("firstName", user.firstName)
                        putExtra("lastName", user.lastName)
                        putExtra("email", user.email)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}