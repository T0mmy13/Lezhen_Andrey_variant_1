package com.bignerdranch.android.smartwatches

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var avatarImageView: ImageView
    private val pickImageRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        avatarImageView = findViewById(R.id.avatar)
        val firstNameTextView: TextView = findViewById(R.id.profile_first_name)
        val lastNameTextView: TextView = findViewById(R.id.profile_last_name)
        val emailTextView: TextView = findViewById(R.id.profile_email)

        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")
        val email = intent.getStringExtra("email")
        val login = intent.getStringExtra("login") ?: ""

        firstNameTextView.text = firstName
        lastNameTextView.text = lastName
        emailTextView.text = email

        loadAvatar(login)

        avatarImageView.setOnClickListener {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also {
                startActivityForResult(it, pickImageRequestCode)
            }
        }

        val profileButton: Button = findViewById(R.id.btn_profile)
        val trackerButton: Button = findViewById(R.id.btn_tracker)
        val logoutButton: Button = findViewById(R.id.btn_logout)


        trackerButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java).apply {
                putExtra("firstName", firstName)
                putExtra("lastName", lastName)
                putExtra("email", email)
                putExtra("login", login)
            }
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val login = intent.getStringExtra("login") ?: return
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    avatarImageView.setImageBitmap(bitmap)

                    saveAvatar(bitmap, login)

                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка при выборе изображения", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAvatar(bitmap: Bitmap, login: String) {
        try {
            val file = File(filesDir, "$login-avatar.png")
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()

            val sharedPref = getSharedPreferences("user_avatars", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(login, file.absolutePath)
                apply()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadAvatar(login: String) {
        val sharedPref = getSharedPreferences("user_avatars", Context.MODE_PRIVATE)
        val filePath = sharedPref.getString(login, null)
        filePath?.let {
            val file = File(it)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                avatarImageView.setImageBitmap(bitmap)
            }
        }
    }
}