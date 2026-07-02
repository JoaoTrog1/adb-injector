package com.jmz.adbinjector.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AdbBinaryManager(private val context: Context) {
    val adbFile = File(context.applicationInfo.nativeLibraryDir, "libadb.so")
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var pairingListener: NsdManager.DiscoveryListener? = null
    private var connectionListener: NsdManager.DiscoveryListener? = null

    private val adbDataDir = File(context.filesDir, ".android").apply { if (!exists()) mkdirs() }

    init {
        if (!adbFile.exists()) {
            val assetPath = "${android.os.Build.SUPPORTED_ABIS[0]}/libadb.so"
            context.assets.open(assetPath).use { input ->
                context.openFileOutput("libadb.so", Context.MODE_PRIVATE).use { output ->
                    input.copyTo(output)
                }
            }
        }
        adbFile.setExecutable(true, true)
    }

    fun startAutoDiscovery(onPairingPortFound: (Int) -> Unit, onExecutionPortFound: (Int) -> Unit) {
        pairingListener = createDiscoveryListener(onPairingPortFound)
        nsdManager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, pairingListener)
        connectionListener = createDiscoveryListener(onExecutionPortFound)
        nsdManager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, connectionListener)
    }

    fun stopAutoDiscovery() {
        try { pairingListener?.let { nsdManager.stopServiceDiscovery(it) } } catch (e: Exception) {}
        try { connectionListener?.let { nsdManager.stopServiceDiscovery(it) } } catch (e: Exception) {}
    }

    private fun createDiscoveryListener(onPortResolved: (Int) -> Unit): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) { onPortResolved(resolvedInfo.port) }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(regType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
    }

    private fun runAdbCommand(vararg args: String): String {
        return try {
            val command = mutableListOf(adbFile.absolutePath)
            command.addAll(args)

            val pb = ProcessBuilder(command)

            if (!adbDataDir.exists()) adbDataDir.mkdirs()

            pb.environment()["HOME"] = adbDataDir.absolutePath
            pb.environment()["ADB_VENDOR_KEYS"] = File(adbDataDir, "adbkey").absolutePath

            pb.environment()["USER"] = "jmz"
            pb.environment()["LOGNAME"] = "jmz"
            pb.environment()["NAME"] = "jmz"
            pb.environment()["HOSTNAME"] = "jmz"

            pb.redirectErrorStream(true)
            val p = pb.start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor()
            out.trim()
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    suspend fun pair(port: Int, code: String): String = withContext(Dispatchers.IO) {
        val res = runAdbCommand("pair", "127.0.0.1:$port", code)
        if (res.contains("Successfully")) "Pareado com sucesso!" else res
    }

    suspend fun connect(port: Int): String = withContext(Dispatchers.IO) { runAdbCommand("connect", "127.0.0.1:$port") }

    suspend fun shell(port: Int, command: String): String = withContext(Dispatchers.IO) {
        val customCommand = "export PS1='jmz:~# '; $command"
        val rawOutput = runAdbCommand("-s", "127.0.0.1:$port", "shell", customCommand)
        return@withContext rawOutput
    }
}