package com.yavin.yavinandroidsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.yavin.yavinandroidsdk.databinding.ActivityMainBinding
import com.yavin.yavinandroidsdk.logger.actions.Action

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private fun getMyApplication(): MyApplication {
        return (application as MyApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.crashButton.setOnClickListener {
            crash()
        }
    }

    private fun crash() {
        getMyApplication().logger().log(Action.ButtonClicked(binding.crashButton))
        getMyApplication().logger().log("Message")
        throw IllegalStateException()
    }
}