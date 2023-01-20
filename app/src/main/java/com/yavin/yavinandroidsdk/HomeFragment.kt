package com.yavin.yavinandroidsdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yavin.yavinandroidsdk.databinding.FragmentHomeBinding
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.ui.YavinLoggerUI
import com.yavin.yavinandroidsdk.model.Person
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), YavinLoggerUI.YavinLoggerUICallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var yavinLogger: YavinLogger

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
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

        binding.broadcastUploadButton.setOnClickListener {
            broadcastUpload(Date())
        }
    }

    private fun showDatePicker() {
        YavinLoggerUI.buildDatePicker(requireContext(), yavinLogger, this)
            .show(childFragmentManager, "datePicker")
    }

    private fun logClick() {
        yavinLogger.log(Action.ButtonClicked(binding.logButton))
    }

    private fun logPerson() {
        val person = Person("John", "Cena", 21)
        yavinLogger.log(json.encodeToString(person))
    }

    private fun crash() {
        throw IllegalStateException()
    }

    private fun broadcastUpload(date: Date) {
        yavinLogger.broadcastLogsUpload(requireContext(), date)
    }

    override fun onYavinLoggerFileSelected(date: Date) {
        // Get log file from date
    }
}