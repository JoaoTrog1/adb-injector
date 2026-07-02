package com.jmz.adbinjector

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.jmz.adbinjector.ui.theme.JMZInjectorTheme
import com.jmz.adbinjector.utils.AdbBinaryManager

class MainActivity : ComponentActivity() {
    private val adbManager by lazy { AdbBinaryManager(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "A permissão é necessária para inserir o código de pareamento.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        checkNotificationPermission()

        setContent {
            JMZInjectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdbTerminalScreen(adbManager = adbManager)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("adb_pairing", "Pareamento ADB", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showPairingNotification(port: Int) {
        val remoteInput = RemoteInput.Builder(AdbPairingReceiver.KEY_TEXT_REPLY).setLabel("Código de 6 dígitos").build()
        val intent = Intent(this, AdbPairingReceiver::class.java).apply { putExtra(AdbPairingReceiver.EXTRA_PORT, port) }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val action = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "Inserir Código", pendingIntent).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, "adb_pairing")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Pareamento Detectado!")
            .setContentText("Porta: $port. Digite o código.")
            .addAction(action)
            .build()

        getSystemService(NotificationManager::class.java).notify(1001, notification)
    }
}