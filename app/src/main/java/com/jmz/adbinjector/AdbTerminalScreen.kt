package com.jmz.adbinjector

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jmz.adbinjector.utils.AdbBinaryManager
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbTerminalScreen(adbManager: AdbBinaryManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pairingPort by remember { mutableStateOf("...") }
    var executionPort by remember { mutableStateOf("...") }
    var command by remember { mutableStateOf("ls -la /sdcard/") }
    var terminalOutput by remember { mutableStateOf("Aguardando comandos...") }

    var connectionStatus by remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    var statusMessage by remember { mutableStateOf("Desconectado do Host") }
    var isPaired by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val terminalScrollState = rememberScrollState()

    DisposableEffect(adbManager) {
        adbManager.startAutoDiscovery(
            onPairingPortFound = { port ->
                scope.launch {
                    pairingPort = port.toString()
                    (context as? MainActivity)?.showPairingNotification(port)
                }
            },
            onExecutionPortFound = { port ->
                scope.launch {
                    executionPort = port.toString()
                }
            }
        )
        onDispose { adbManager.stopAutoDiscovery() }
    }

    val conectarAdb = { portStr: String ->
        scope.launch {
            connectionStatus = ConnectionStatus.CONNECTING
            statusMessage = "Conectando automaticamente em 127.0.0.1:$portStr..."

            val connResult = adbManager.connect(portStr.toIntOrNull() ?: 0)

            if (connResult.contains("already connected") || connResult.contains("connected")) {
                connectionStatus = ConnectionStatus.CONNECTED
                statusMessage = "Conectado com sucesso!"
                isPaired = true
            } else {
                connectionStatus = ConnectionStatus.FAILED
                statusMessage = "Tentativa automática falhou: $connResult"
            }
        }
    }

    LaunchedEffect(executionPort) {
        if (executionPort != "..." && connectionStatus == ConnectionStatus.DISCONNECTED) {
            conectarAdb(executionPort)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Injetor ADB Nativo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            val statusColor by animateColorAsState(
                targetValue = when (connectionStatus) {
                    ConnectionStatus.DISCONNECTED -> Color.Gray
                    ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                    ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                    ConnectionStatus.FAILED -> MaterialTheme.colorScheme.error
                }, label = "statusColor"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(statusColor, shape = CircleShape)
                        )
                        Column {
                            Text("Conexão ADB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (connectionStatus == ConnectionStatus.CONNECTING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Descoberta de Serviço (mDNS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            modifier = Modifier.weight(1f),
                            onClick = { },
                            label = {
                                val textoPareamento = if (isPaired || connectionStatus == ConnectionStatus.CONNECTED) "Concluído" else pairingPort
                                Text(textoPareamento)
                            },
                            leadingIcon = {
                                if (isPaired || connectionStatus == ConnectionStatus.CONNECTED) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                } else if (pairingPort == "...") {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        AssistChip(
                            modifier = Modifier.weight(1f),
                            onClick = { },
                            label = { Text(executionPort) },
                            leadingIcon = {
                                if (executionPort == "...") {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }

                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Opções do Desenvolvedor")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ações de Conexão", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                        val pairBadgeColor = if (isPaired || connectionStatus == ConnectionStatus.CONNECTED) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pairBadgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, pairBadgeColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPaired || connectionStatus == ConnectionStatus.CONNECTED) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = pairBadgeColor
                                )
                                Text(
                                    text = if (isPaired || connectionStatus == ConnectionStatus.CONNECTED) "Dispositivo Pareado" else "Aguardando Pareamento",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pairBadgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        enabled = executionPort != "..." && connectionStatus != ConnectionStatus.CONNECTING,
                        onClick = { conectarAdb(executionPort) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val isConnected = connectionStatus == ConnectionStatus.CONNECTED
                        Icon(if (isConnected) Icons.Default.Refresh else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isConnected) "Reconectar Servidor ADB" else "Conectar ao Dispositivo")
                    }
                }
            }

            AnimatedVisibility(visible = connectionStatus == ConnectionStatus.CONNECTED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Console do Injetor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            label = { Text("Comando de Shell") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    terminalOutput = "Executando comando..."
                                    val shellResult = adbManager.shell(executionPort.toIntOrNull() ?: 0, command)
                                    terminalOutput = shellResult
                                    terminalScrollState.scrollTo(0)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enviar Comando")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Text("Saída do Terminal:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 300.dp)
                                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                                .verticalScroll(terminalScrollState)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = terminalOutput,
                                color = Color(0xFF4AF626),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}