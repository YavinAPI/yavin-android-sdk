package com.yavin.yavinandroidsdk

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yavin.yavinandroidsdk.databinding.ActivityMainBinding
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.ui.YavinLoggerUI
import java.util.*

class MainActivity : AppCompatActivity(), YavinLoggerUI.YavinLoggerUICallback {

    private lateinit var binding: ActivityMainBinding

    private fun getMyApplication(): MyApplication {
        return (application as MyApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.datePickerButton.setOnClickListener {
            showDatePicker()
        }

        binding.logButton.setOnClickListener {
            logClick()
        }

        binding.crashButton.setOnClickListener {
            crash()
        }
    }

    private fun showDatePicker() {
        YavinLoggerUI.buildDatePicker(this, getMyApplication().logger(), this)
            .show(supportFragmentManager, "datePicker")
    }

    private fun logClick() {
        getMyApplication().logger().log(Action.ButtonClicked(binding.logButton))
    }

    private fun crash() {
        throw IllegalStateException()
    }

    override fun onPositiveYavinLoggerDatePicker(selectedDate: Date) {
        val file = getMyApplication().logger().getLogsFile(this, selectedDate)
        if (file.exists()) {
            file.forEachLine {
                Log.d("File", it)
            }
        }
    }
}