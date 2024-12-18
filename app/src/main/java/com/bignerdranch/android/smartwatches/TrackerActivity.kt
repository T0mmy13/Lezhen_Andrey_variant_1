package com.bignerdranch.android.smartwatches

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class TrackerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var db: AppDatabase
    private var accelerometer: Sensor? = null
    private var stepCount: Int = 0
    private lateinit var stepsCountTextView: TextView

    private lateinit var stepsRecyclerView: RecyclerView
    private var stepsList = mutableListOf<Pair<String, Int>>()
    private lateinit var stepsAdapter: StepsAdapter

    // Константы для фильтрации и порогов
    private val alpha = 0.8f
    private var gravity = FloatArray(3)

    // Пороговое значение ускорения
    private val stepThreshold = 7.5

    // Время последнего детектированного шага
    private var lastStepTime: Long = 0

    // Минимальное время между шагами в миллисекундах
    private val stepInterval = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        db = AppDatabase.getDatabase(this)
        stepsCountTextView = findViewById(R.id.steps_count)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(this, "Акселерометр не поддерживается", Toast.LENGTH_SHORT).show()
        }

        val profileButton: Button = findViewById(R.id.btn_profile)
        val logoutButton: Button = findViewById(R.id.btn_logout)

        profileButton.setOnClickListener {
            val firstName = intent.getStringExtra("firstName") ?: ""
            val lastName = intent.getStringExtra("lastName") ?: ""
            val email = intent.getStringExtra("email") ?: ""
            val login = intent.getStringExtra("login") ?: ""

            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("firstName", firstName)
                putExtra("lastName", lastName)
                putExtra("email", email)
                putExtra("login", login)
            }
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            loadStepsFromTextViewIntoDatabase() // Сохраняем шаги в базу данных
            onLogout() // Очистка SharedPreferences для списка шагов

            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Инициализация RecyclerView с реверсированием порядка элементов
        stepsRecyclerView = findViewById(R.id.steps_recycler_view)
        stepsAdapter = StepsAdapter(stepsList)
        stepsRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        stepsRecyclerView.adapter = stepsAdapter

        // Загрузка списка шагов из SharedPreferences
        loadStepsList()

        // Загрузка количества шагов из базы данных
        loadStepsCount()
    }

    private fun loadStepsCount() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val login = sharedPref.getString("login", null) ?: return
        Log.d("TrackerActivity", login)
        CoroutineScope(Dispatchers.IO).launch {
            val steps = db.userDao().getStepsCountForUser(login)
            withContext(Dispatchers.Main) {
                stepCount = steps
                stepsCountTextView.text = "Кол-во шагов: $stepCount"
                Log.d("TrackerActivity", "Steps loaded: $stepCount, user: ${ db.userDao().getStepsCountForUser(login)}")
            }
        }
    }

    private fun loadStepsList() {
        val sharedPref = getSharedPreferences("user_steps", Context.MODE_PRIVATE)
        val stepsMap = sharedPref.all.mapNotNull {
            val date = it.key
            val steps = it.value as? Int ?: return@mapNotNull null
            date to steps
        }.toMutableList()

        stepsList.clear()
        stepsList.addAll(stepsMap)
        stepsAdapter.notifyDataSetChanged()

        // Проверка последней даты
        val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        if (stepsList.isEmpty() || stepsList.last().first != todayDate) {
            stepsList.add(todayDate to 0)
            stepCount = 0 // сбрасываем шаги на сегодняшний день
            stepsAdapter.notifyItemInserted(stepsList.size - 1)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        loadStepsFromTextViewIntoDatabase() // Сохраняем шаги в базу данных
        saveStepsList() // Сохраняем шаги в SharedPreferences
    }

    private fun loadStepsFromTextViewIntoDatabase() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val login = sharedPref.getString("login", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().findByLogin(login)
            user?.let {
                it.stepsCount = stepCount
                db.userDao().updateUser(it)
                withContext(Dispatchers.Main) {
                    Log.d("TrackerActivity", "Steps saved: ${  it.stepsCount}")
                }
            } ?: {
                Log.e("TrackerActivity", "User not found to load steps")
            }
        }
    }

    private fun saveStepsList() {
        val sharedPref = getSharedPreferences("user_steps", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            for ((date, steps) in stepsList) {
                putInt(date, steps)
            }
            apply()
        }
    }

    private fun onLogout() {
        val sharedPref = getSharedPreferences("user_steps", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == accelerometer) {
                gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]

                val linearAccelerationX = it.values[0] - gravity[0]
                val linearAccelerationY = it.values[1] - gravity[1]
                val linearAccelerationZ = it.values[2] - gravity[2]

                val accelerationMagnitude = sqrt(
                    (linearAccelerationX * linearAccelerationX +
                            linearAccelerationY * linearAccelerationY +
                            linearAccelerationZ * linearAccelerationZ).toDouble()
                )

                if (accelerationMagnitude > stepThreshold) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastStepTime > stepInterval) {
                        stepCount++
                        lastStepTime = currentTime
                        stepsCountTextView.text = "Кол-во шагов: $stepCount"
                        updateStepsList()
                    }
                }
            }
        }
    }

    private fun updateStepsList() {
        val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        if (stepsList.isNotEmpty() && stepsList.last().first == todayDate) {
            stepsList[stepsList.size - 1] = todayDate to stepCount
            stepsAdapter.notifyItemChanged(stepsList.size - 1)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не требуется обработки изменений точности
    }
}