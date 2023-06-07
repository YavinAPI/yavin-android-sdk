package com.yavin.yavinandroidsdk.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yavin.yavinandroidsdk.demo.databinding.FragmentHomeBinding
import com.yavin.yavinandroidsdk.demo.model.Person
import com.yavin.yavinandroidsdk.demo.model.toText
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.ui.YavinLoggerUI
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
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

    @Inject
    lateinit var connectivityProvider: YavinConnectivityProvider

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

    private val connectivityListener =
        object : YavinConnectivityProvider.ConnectivityStateListener {
            override fun onConnectivityStateChange(state: YavinConnectivityProvider.NetworkState) {
                println("new connectivity state delivered. Internet connected = $state")
                binding.noConnectivityWarning.isVisible = !state.hasInternet
            }
        }

    override fun onStart() {
        super.onStart()
        connectivityProvider.addListener(connectivityListener)
    }

    override fun onStop() {
        super.onStop()
        connectivityProvider.removeListener(connectivityListener)
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

        binding.checkMobileDataButton.setOnClickListener {
            checkMobileConnection()
        }

        binding.hasInternetButton.setOnClickListener {
            checkHasInternet()
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
        val persons = listOf(
            Person("John", "Cena", 21),
            null,
        )

        persons.forEach {
            it!!.toText()
        }
    }

    private fun broadcastUpload(date: Date) {
        yavinLogger.broadcastLogsUpload(requireContext(), date)
    }

    private fun checkMobileConnection() {
        val mobileDataConnected = connectivityProvider.isMobileDataConnected()
        Toast.makeText(requireContext(), "4G enabled: $mobileDataConnected ", Toast.LENGTH_LONG)
            .show()
    }

    private fun checkHasInternet() {
        Toast.makeText(
            requireContext(),
            "Has internet: ${connectivityProvider.hasInternet()} ",
            Toast.LENGTH_LONG
        )
            .show()
    }

    override fun onYavinLoggerFileSelected(date: Date) {
        // Get log file from date
    }
}