package com.crystal.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initOnOffButton()
        initChangeAlarmTimeButton()
        val model = fetchDataFromSharedPref()
        renderView(model)
        //데이터 가져오기
        //뷰에 데이터 그리기
    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }
        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun fetchDataFromSharedPref(): AlarmDisplayModel {
        val sharedPref = getSharedPreferences("time", Context.MODE_PRIVATE)
        val timeDBValue = sharedPref.getString(ALAME_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPref.getBoolean(ONOFF_KEY, false)
        val timeDate = timeDBValue.split(":")
        val alarmModel = AlarmDisplayModel(
            hour = timeDate[0].toInt(),
            minute = timeDate[1].toInt(),
            onOff = onOffDBValue
        )

        //보정. 실제 화면과 데이터 다를 경우 예외처리
        // 확인용, No create
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        if ((pendingIntent == null) and alarmModel.onOff) {
            //실제 알람은 꺼져있는데, data 는 켜져있는 경우.
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            //실제 알람은 켜져있는데, data 는 꺼져있는 경우.
            pendingIntent.cancel()
        }

        return alarmModel
    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {
            // 데이터를 확인한다.
            val model = it.tag as? AlarmDisplayModel ?:  return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)
            if(newModel.onOff){
                //알람 실행
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)
                    if(before(Calendar.getInstance())){
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager= getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT) //기존것이 있으면 현재꺼로 업데이트 하겟다.
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

            }else{
                cancelAlarm()
            }
        }
    }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {
            //현재시간을 일단 가져온다.
            val calendar = Calendar.getInstance()
            //time pick dialog 띄어줘서 시간을 설정하도록 하고, 시간을 가져와서
            TimePickerDialog(this,
                { picker, hour, minute ->
                    val model = saveAlarmModel(hour, minute, false)
                    renderView(model)
                    cancelAlarm()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false)
                .show()

        }
    }

    private fun cancelAlarm() {
        // 기존에 있던 알람을 삭제.
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        pendingIntent?.cancel()
    }

    private fun saveAlarmModel(hour: Int, minute: Int, onOff: Boolean): AlarmDisplayModel {
        val model = AlarmDisplayModel(hour, minute, onOff)
        val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(ALAME_KEY, model.makeDateForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }
        return model
    }

    companion object {
        private const val SHARED_PREF_NAME = "time"
        private const val ALAME_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}