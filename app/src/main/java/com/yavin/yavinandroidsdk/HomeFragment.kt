package com.yavin.yavinandroidsdk

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.yavin.yavinandroidsdk.databinding.FragmentHomeBinding
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.ui.YavinLoggerUI
import com.yavin.yavinandroidsdk.model.Person
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class HomeFragment : Fragment(), YavinLoggerUI.YavinLoggerUICallback {

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun getMyApplication(): MyApplication {
        return (requireActivity().application as MyApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.goToDetailsScreenButton.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToDetailsFragment())
        }

        binding.datePickerButton.setOnClickListener {
            showDatePicker()
        }

        binding.logButton.setOnClickListener {
            logClick()
        }

        binding.logModel.setOnClickListener {
            logPerson()
        }

        binding.crashButton.setOnClickListener {
            crash()
        }
    }

    private fun showDatePicker() {
        YavinLoggerUI.buildDatePicker(requireContext(), getMyApplication().logger(), this)
            .show(childFragmentManager, "datePicker")
    }

    private fun logClick() {
        getMyApplication().logger().log(Action.ButtonClicked(binding.logButton))
    }

    private fun logPerson() {
        val person = Person("John", "Cena", 21)
        getMyApplication().logger().log(json.encodeToString(person))
    }

    private fun crash() {
        throw IllegalStateException()
    }

    override fun onPositiveYavinLoggerDatePicker(selectedDate: Date) {
        val file = getMyApplication().logger().getLogsFile(requireContext(), selectedDate)
        if (file.exists()) {
            file.forEachLine {
                Log.d("File", it)
            }
        }
    }
}