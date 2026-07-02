package com.jmz.adbinjector

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.jmz.adbinjector.utils.AdbBinaryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdbPairingReceiver : BroadcastReceiver() {
    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_PORT = "extra_port"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val pairingCode = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
        val port = intent.getIntExtra(EXTRA_PORT, 0)

        if (!pairingCode.isNullOrBlank() && port != 0) {
            Toast.makeText(context, "Acionando o binário ADB...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val manager = AdbBinaryManager(context)
                val resultado = manager.pair(port, pairingCode)

                withContext(Dispatchers.Main) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(1001)

                    notificationManager.cancelAll()

                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(intent)

                    Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}