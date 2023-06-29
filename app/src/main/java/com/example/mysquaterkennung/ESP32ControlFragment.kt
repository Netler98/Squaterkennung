package com.example.mysquaterkennung

import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mysquaterkennung.databinding.FragmentEsp32controlBinding
import com.example.mysquaterkennung.model.ConnectState
import com.example.mysquaterkennung.model.MainViewModel


class ESP32ControlFragment : Fragment() {

    private var _binding: FragmentEsp32controlBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentEsp32controlBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDevice.text = viewModel.getDeviceSelected()

        // Mittels Observer über Änderungen des connect status informieren
        viewModel.connectState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectState.CONNECTED -> {
                    binding.tvIsConnected.text = getString(R.string.connected)
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = true
                    binding.switchDaten.isEnabled = true
                }
                ConnectState.NOT_CONNECTED -> {
                    binding.tvIsConnected.text = getString(R.string.not_connected)
                    binding.btnConnect.isEnabled = true
                    binding.btnDisconnect.isEnabled = false
                    binding.switchDaten.isEnabled = false
                }
                ConnectState.NO_DEVICE -> {
                    binding.tvIsConnected.text = getString(R.string.no_selected_device)
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = false
                    binding.switchDaten.isEnabled = false
                }
                ConnectState.DEVICE_SELECTED -> {
                    binding.tvIsConnected.text = getString(R.string.connecting)
                    binding.btnConnect.isEnabled = true
                    binding.btnDisconnect.isEnabled = false
                    binding.switchDaten.isEnabled = false
                }
            }
        }

        // Observer auf neue Daten vom ESP32
        viewModel.esp32Data.observe(viewLifecycleOwner) { data ->
            binding.tvData.text = data.counter
            val counter = data.counter.toIntOrNull()
            counter?.let {
                viewModel.addToHighscoreList(it)
            }
        }

        binding.btnSelectDevice.setOnClickListener {
            findNavController().navigate(R.id.action_ESP32ControlFragment_to_manageDeviceFragment)
        }

        binding.btnConnect.setOnClickListener {
            viewModel.connect()
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }


        binding.switchDaten.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.startDataLoadJob()
                startTimer()
                binding.tvData.visibility = View.VISIBLE
                binding.timer.visibility = View.VISIBLE
                binding.tvHighscore.visibility = View.INVISIBLE
            } else {
                viewModel.cancelDataLoadJob()
                stopTimer()
                updateHighscoreText()
                binding.tvData.visibility = View.INVISIBLE
                binding.timer.visibility = View.INVISIBLE
                binding.tvHighscore.visibility = View.VISIBLE
            }
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(60000, 10) {
            override fun onTick(remaining: Long) {
                binding.timer.text = remaining.toString()
            }

            override fun onFinish() {
                binding.switchDaten.isChecked = false
            }
        }
        timer?.start()
    }

    private fun stopTimer(){
        timer?.cancel()
    }

    private fun updateHighscoreText() {
        val highscoreList = viewModel.getHighscoreList()
        val highscoreText = highscoreList.joinToString("\n")
        binding.tvHighscore.text = "Highscore:\n$highscoreText"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

