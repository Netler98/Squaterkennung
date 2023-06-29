package com.example.mysquaterkennung

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mysquaterkennung.databinding.FragmentManageDeviceBinding
import com.example.mysquaterkennung.model.MainViewModel


class ManageDeviceFragment : Fragment() {

    private var _binding: FragmentManageDeviceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentManageDeviceBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter für den ListView
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1,   // Layout zur Darstellung der ListItems
            viewModel.getDeviceList()!!)   // Liste, die Dargestellt werden soll

        // Adapter an den ListView koppeln
        binding.listview.adapter = adapter

        // Mittels Observer den Adapter über Änderungen in der Liste informieren
        viewModel.deviceList.observe(viewLifecycleOwner) { adapter.notifyDataSetChanged() }

        binding.listview.setOnItemClickListener { _, _, i, _ ->
            // i ist der Index des geklickten Eintrags
            viewModel.setDeviceSelected(binding.listview.getItemAtPosition(i).toString())
            // Navigiere zurück zum ESP32ControlFragment
            findNavController().navigate(R.id.action_manageDeviceFragment_to_ESP32ControlFragment)
        }

        binding.buttonSecond.setOnClickListener {
            viewModel.startScan()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopScan()
        _binding = null
    }
}