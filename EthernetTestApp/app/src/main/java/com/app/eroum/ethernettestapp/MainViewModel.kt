package com.app.eroum.ethernettestapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eroum.ethernettestapp.ui.ethernet.LogLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class MainViewModel : ViewModel() {

    var localIp by mutableStateOf("N/A")
        private set

    var isPinging by mutableStateOf(false)
        private set

    // 핑 관련
    val pingLogs = mutableStateListOf<LogLine>()
    private var pingProcess: Process? = null

    private var transmitted = 0
    private var received = 0
    private var errors = 0
    private var rttList = mutableListOf<Double>()
    private var currentTarget = ""


    init {
        loadEthernetIp()
    }

    fun loadEthernetIp() {
        viewModelScope.launch {
            // IO용 백그라운드 스레드
            val ip = withContext(Dispatchers.IO) {
                getEthernetIpAddress()
            } ?: "Not connected"

            // UI용 스레드
            // Dispatchers.MAIN
            localIp = ip
        }
    }

    private fun getEthernetIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue

                val name = intf.name ?: continue
                if (!name.startsWith("eth") && !name.startsWith("en") && !name.startsWith("lan")) {
                    continue
                }

                val addrs = Collections.list(intf.inetAddresses)
                val ipv4 = addrs.firstOrNull { it is Inet4Address && !it.isLoopbackAddress } as? Inet4Address
                if (ipv4 != null) {
                    return ipv4.hostAddress
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun onPingButtonClick(targetIp: String) {
        if (isPinging) {
            stopPing()
        } else {
            startPing(targetIp)
        }
    }

    private fun startPing(targetIp: String) {
        if (targetIp.isBlank()) {
            addLog("Target IP is empty", Color.Red)
            return
        }

        pingLogs.clear()
        addLog("Start ping: $targetIp", Color.Blue)

        isPinging = true
        currentTarget = targetIp

        transmitted = 0
        received = 0
        errors = 0
        rttList.clear()

        viewModelScope.launch(Dispatchers.IO) {
            val rttRegex = Regex("""time=([\d.]+)\s*ms""")

            try {
                val process = Runtime.getRuntime().exec(arrayOf("ping", "-i", "1", "-n", targetIp))
                pingProcess = process

                val inputReader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                var line: String? = null

                while (isPinging && inputReader.readLine().also { line = it } != null) {
                    val text = line ?: break
                    addLog(text, Color.Black)

                    if ("icmp_seq=" in text) transmitted++
                    val rttMatch = rttRegex.find(text)
                    if (rttMatch != null) {
                        val rtt = rttMatch.groupValues[1].toDoubleOrNull()
                        if (rtt != null) {
                            received++
                            rttList.add(rtt)
                        }
                    }
                    if ("Destination Host Unreachable" in text) errors++
                }

                while (isPinging && errorReader.readLine().also { line = it } != null) {
                    val text = line ?: break
                    addLog("[ERR] $text", Color.Red)
                }

                if (isPinging) {
                    addPingStatistics(targetIp, transmitted, received, errors, rttList)
                }

            } catch (e: Exception) {
                // addLog("Ping failed: ${e.message}", Color.Red)
            } finally {
                isPinging = false
                pingProcess = null
            }
        }
    }


    private fun stopPing() {
        // addLog("Stop ping requested", Color.Blue)
        isPinging = false
        pingProcess?.destroy()
        pingProcess = null
        addPingStatistics(currentTarget, transmitted, received, errors, rttList)
    }

    private fun addLog(msg: String, color: Color) {
        // UI 쓰레드로 append
        viewModelScope.launch(Dispatchers.Main) {
            pingLogs.add(LogLine(msg, color))
        }
    }

    private fun addPingStatistics(
        targetIp: String,
        transmitted: Int,
        received: Int,
        errors: Int,
        rttList: List<Double>
    ) {
        val lossCount = transmitted - received
        val lossPercent = if (transmitted > 0) (lossCount * 100) / transmitted else 0


        addLog("--- $targetIp ping statistics (app) ---\n" +
                "$transmitted packets transmitted, " +
                "$received received, " +
                "+$errors errors, " +
                "$lossPercent% packet loss"
            , Color.Blue
        )

        if (rttList.isNotEmpty()) {
            val min = rttList.minOrNull() ?: 0.0
            val max = rttList.maxOrNull() ?: 0.0
            val avg = rttList.average()
            addLog("rtt min/avg/max = " +
                    "%.3f".format(min) + "/" +
                    "%.3f".format(avg) + "/" +
                    "%.3f".format(max) + " ms",
                Color.Blue
            )
        } else {
            // addLog("no rtt data (no reply)", Color.Blue)
        }
    }

    /* onCleared */
    override fun onCleared() {
        super.onCleared()
        pingProcess?.destroy()
    }
}