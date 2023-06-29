package com.example.mysquaterkennung.model

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.example.mysquaterkennung.esp32ble.CUSTOM_SERVICE_UUID
import com.example.mysquaterkennung.esp32ble.Esp32Ble
import com.example.mysquaterkennung.esp32ble.Esp32Data
import com.juul.kable.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

object ScanState {
    const val NOT_SCANNING = 0
    const val SCANNING = 1
    const val FAILED = 2
}

object ConnectState {
    const val NO_DEVICE = 0
    const val DEVICE_SELECTED = 1
    const val CONNECTED = 2
    const val NOT_CONNECTED = 3
}

data class Device(val name: String, val address: String) {
    override fun toString(): String = name + ": " + address
}


class MainViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val HIGHSCORE_PREF_KEY = "highscore_list"
    private val _deviceList = MutableLiveData<MutableList<Device>>()
    val deviceList: LiveData<MutableList<Device>>
        get() = _deviceList


    init {
        _deviceList.value = mutableListOf()
        loadHighscoreList()
    }

    fun getDeviceList(): List<Device>? {
        return _deviceList.value
    }


    private var deviceSelected = ""
    fun getDeviceSelected(): String {
        return deviceSelected
    }


    fun setDeviceSelected(devicestring: String) {
        deviceSelected = devicestring
        _connectState.value = ConnectState.DEVICE_SELECTED
    }


    // Scanning
    // ------------------------------------------------------------------------------
    private lateinit var scanJob: Job

    private val scanner = Scanner {
        filters = listOf(
            Filter.Service(uuidFrom(CUSTOM_SERVICE_UUID))
        )
    }

    private var scanState = ScanState.NOT_SCANNING

    fun startScan() {
        Log.i(">>>>", "Start Scanning ...")
        if (scanState == ScanState.SCANNING) return // Scan already in progress.
        scanState = ScanState.SCANNING

        val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)
        scanJob = viewModelScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause ->
                        scanState = ScanState.FAILED
                        Log.i(">>>> Scanning Failed", cause.message.toString())
                    }
                    .onCompletion { cause ->
                        Log.i(">>>> Scanning Completed", cause?.message.toString())
                        if (cause == null || cause is CancellationException)
                            scanState = ScanState.NOT_SCANNING
                    }
                    .collect { advertisement ->
                        val device = Device(
                            name = advertisement.name.toString(),
                            address = advertisement.address.toString()
                        )
                        if (_deviceList.value?.contains(device) == false) {
                            _deviceList.value?.add(device)
                            _deviceList.notifyObserver()
                        }
                        Log.i(">>>>", _deviceList.value.toString())
                    }
            }
        }
    }

    fun stopScan() {
        scanState = ScanState.NOT_SCANNING
        scanJob.cancel()
    }

    // Connecting
    // --------------------------------------------------------------------------
    private lateinit var peripheral: Peripheral
    private lateinit var esp32: Esp32Ble

    private val _connectState = MutableLiveData<Int>(ConnectState.NO_DEVICE)
    val connectState: LiveData<Int>
        get() = _connectState

    fun connect() {
        if (_connectState.value == ConnectState.NO_DEVICE) return
        val macAddress = deviceSelected.substring(deviceSelected.length - 17);
        peripheral = viewModelScope.peripheral(macAddress) {
            onServicesDiscovered {
                requestMtu(517)
            }
        }
        esp32 = Esp32Ble(peripheral)


        viewModelScope.launch {
            peripheral.state.collect { state ->

                Log.i(">>>> Connection State:", state.toString())
                when (state.toString()) {
                    "Connected" -> _connectState.value = ConnectState.CONNECTED
                    "Disconnected(null)" -> _connectState.value = ConnectState.NOT_CONNECTED
                    else -> _connectState.value = ConnectState.NOT_CONNECTED
                }
            }
        }

        viewModelScope.launch {
            esp32.connect()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            // Allow 5 seconds for graceful disconnect before forcefully closing `Peripheral`.
            withTimeoutOrNull(5_000L) {
                esp32.disconnect()
            }
        }
    }


    // Extension Function, um Änderung in den Einträgen von Listen
    // dem Observer anzeigen zu können
    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }


    // Communication
    // ____________________________________________________________________
    private lateinit var dataLoadJob: Job

    private var _esp32Data = MutableLiveData(Esp32Data())
    val esp32Data: LiveData<Esp32Data>
        get() = _esp32Data

    private val highscoreList = mutableListOf<Int>()

    fun startDataLoadJob() {

        dataLoadJob = viewModelScope.launch {
            esp32.incomingMessages.collect { msg ->
                val jsonstring = String(msg)
                Log.i(">>>> msg in", jsonstring)
                _esp32Data.value = jsonParseEsp32Data(jsonstring)
                val counter = _esp32Data.value?.counter?.toIntOrNull()
                counter?.let {
                    addToHighscoreList(it)
                    saveHighscoreList()
                }
            }
        }
    }

    fun cancelDataLoadJob() {
        dataLoadJob.cancel()
    }

    fun jsonParseEsp32Data(jsonString: String): Esp32Data {
        try {
            val obj = JSONObject(jsonString)
            return Esp32Data(
                counter = obj.getString("COUNTER"),
            )
        } catch (e: Exception) {
            Log.i(">>>>", "Error decoding JSON ${e.message}")
            return Esp32Data()
        }
    }

    fun addToHighscoreList(score: Int) {
        highscoreList.add(score)
        highscoreList.sortDescending()
        highscoreList.take(5)
    }

    fun getHighscoreList(): List<Int> {
        return highscoreList
    }

    private fun loadHighscoreList(){
        val highscoreString = sharedPreferences.getString(HIGHSCORE_PREF_KEY, null)
        highscoreString?.let {
            val highscoreArray = it.split(",").map { score -> score.toInt()}
                highscoreList.addAll(highscoreArray)

        }
    }

    private fun saveHighscoreList(){
        val highscoreString = highscoreList.joinToString {","}
        sharedPreferences.edit().putString(HIGHSCORE_PREF_KEY, highscoreString).apply()
    }
}




