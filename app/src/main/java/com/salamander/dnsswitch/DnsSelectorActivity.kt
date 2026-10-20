package com.salamander.dnsswitch

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

// Регулярки компилируются один раз, а не при каждой проверке ввода
private val IP_ADDRESS_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
private val HOSTNAME_REGEX = Regex("^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")


class DnsSelectorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getStringExtra("ACTION") == "DISABLE_DNS") {
            applyDns("off") // Твоя функция выключения
            showDnsNotification("Выключен") // Обновляем текст в шторке
            finish() // Закрываем приложение
            return // Дальше код не выполняем
        }



        setContent {
            var isVisible by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // Запускаем появление при старте
            LaunchedEffect(Unit) { isVisible = true }

            MaterialTheme {
                // Прозрачный контейнер на весь экран
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // Без эффекта при клике по пустоте
                            onClick = {
                                isVisible = false
                                window.decorView.postDelayed({ finish() }, 200)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f, animationSpec = tween(200)),
                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.92f, animationSpec = tween(200))
                    ) {
                        Card(
                            modifier = Modifier
                                .width(300.dp)
                                .heightIn(max = 450.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(28.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            // Явно задаем цвета для всех вложенных элементов
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                                // Это заставит рипл использовать основной цвет темы (обычно синий/фиолетовый)
                            ) {
                                var currentDns by remember { mutableStateOf("off") }
                                LaunchedEffect(Unit) {
                                    // После withContext(IO) корутина автоматически возвращается на главный поток
                                    val result = withContext(Dispatchers.IO) {
                                        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
                                        val dnsAddr = Settings.Global.getString(contentResolver, "private_dns_specifier")
                                        if (dnsMode == "off" || dnsMode == null) "off" else (dnsAddr ?: "off")
                                    }
                                    currentDns = result
                                }
                                DnsList(currentDns = currentDns) { addr ->
                                    scope.launch(Dispatchers.IO) { applyDns(addr) }

                                    // 2. Определяем красивое имя для уведомления
                                    val friendlyName = when(addr) {
                                        "dns.google" -> "Google"
                                        "cloudflare-dns.com"-> "Cloudflare"
                                        "dns.quad9.net"-> "Quad9"
                                        "dns.comss.one" -> "Comss.one"
                                        "dns.adguard.com" -> "AdGuard"
                                        "dns.malw.link" -> "Malw.link"
                                        "dns.astracat.ru" -> "AstraCat"
                                        "xbox-dns.ru" -> "XBoxDNS"
                                        "off" -> "Выключен"
                                        else -> addr // Если забыли прописать, покажет сам адрес вместо пустоты
                                    }


                                    // 3. Показываем статус в шторке
                                    showDnsNotification(friendlyName)

                                    // 4. Закрываем интерфейс
                                    isVisible = false
                                    window.decorView.postDelayed({ finish() }, 200)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ФУНКЦИЯ ДЛЯ УВЕДОМЛЕНИЙ (вставляй сюда)
    @SuppressLint("MissingPermission")
    private fun showDnsNotification(dnsName: String) {
        val channelId = "dns_status_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (dnsName == "Выключен") {
            notificationManager.cancel(1)
            return
        }

        // 1. Готовим клик по самому уведомлению (открыть приложение)
        val openAppIntent = Intent(this, DnsSelectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Готовим клик по кнопке "Выключить"
        val disableIntent = Intent(this, DnsSelectorActivity::class.java).apply {
            putExtra("ACTION", "DISABLE_DNS")
        }
        val disablePendingIntent = PendingIntent.getActivity(
            this, 1, disableIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Создаем канал (для новых Android)
        val channel = NotificationChannel(
            channelId, "Статус DNS",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        // 4. Собираем само уведомление
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dns_tile)
            .setContentTitle("Активный DNS")
            .setContentText("Используется: $dnsName")
            .setContentIntent(openAppPendingIntent) // Клик по тексту
            .setOngoing(true) // Нельзя смахнуть
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Выключить",
                disablePendingIntent // Клик по кнопке
            )

        notificationManager.notify(1, builder.build())
    }


    private fun applyDns(address: String) {
        try {
            if (address == "off") {
                Settings.Global.putString(contentResolver, "private_dns_mode", "off")
            } else {
                Settings.Global.putString(contentResolver, "private_dns_mode", "hostname")
                Settings.Global.putString(contentResolver, "private_dns_specifier", address)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun DnsList(currentDns: String, onSelect: (String) -> Unit) {
//    val topFade = Color.Transparent
//    val centerColor = Color.Black
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
//    val scrollState = rememberScrollState()

    val servers = remember {
        mapOf(
            "Выключить" to "off",
            "Google" to "dns.google",
            "Cloudflare" to "cloudflare-dns.com",
            "Quad9" to "dns.quad9.net",
            "Comss.one" to "dns.comss.one",
            "AdGuard" to "dns.adguard.com",
            "Malw.link" to "dns.malw.link",
            "AstraCat" to "dns.astracat.ru",
            "XBoxDNS" to "xbox-dns.ru"
        )
    }

    // --- Кастомные сервера пользователя ---
    val context = LocalContext.current
    val prefs =
        remember { context.getSharedPreferences("dns_switcher_prefs", Context.MODE_PRIVATE) }
    var customServers by remember {
        mutableStateOf(prefs.getStringSet("custom_servers", emptySet())?.toList() ?: emptyList())
    }
    var newServerInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    fun isValidHostname(host: String): Boolean {
        if (host.isEmpty() || host.length > 253) return false
        // Хост не должен быть голым IP-адресом (Android Private DNS такое не поддерживает)
        if (IP_ADDRESS_REGEX.matches(host)) return false
        // Базовая проверка формата домена: буквы/цифры/дефисы, минимум одна точка
        return HOSTNAME_REGEX.matches(host)
    }

    val allServers = remember(customServers) {
        servers + customServers.associateWith { it } // имя = сам адрес
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- ВЕРХНЯЯ ЧАСТЬ (ЗАФИКСИРОВАНА) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\uD83E\uDD8E DNS Сервера",
                style = MaterialTheme.typography.titleMedium, // Сделал чуть меньше, чтобы было аккуратнее
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }


        // --- СРЕДНЯЯ ЧАСТЬ (СКРОЛЛ) ---
        var isHandlingClick by remember { mutableStateOf(false) }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 8.dp)
        ) {
            items(allServers.toList()) { (name, addr) ->
                var isPressed by remember { mutableStateOf(false) }
                val isSelected = (addr == currentDns)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(36.dp))
                        .background(if (isPressed) Color(0xFF007DFF).copy(alpha = 0.1f) else Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        isPressed = false
                                    }
                                },
                                onTap = {
                                    if (isHandlingClick) return@detectTapGestures
                                    isHandlingClick = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        delay(100.milliseconds)
                                        onSelect(addr)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) Color(0xFF007DFF) else Color.Unspecified
                        )
                        if (addr != "off") {
                            Text(
                                text = addr,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color(0xFF007DFF).copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF007DFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (customServers.contains(addr)) {
                        IconButton(
                            onClick = {
                                val updated = customServers - addr
                                customServers = updated
                                prefs.edit { putStringSet("custom_servers", updated.toSet()) }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить сервер",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }


        // --- ДОБАВИТЬ СВОЙ СЕРВЕР ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newServerInput,
                onValueChange = { newServerInput = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "dns.example.com",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = {
                val trimmed = newServerInput.trim()
                when {
                    trimmed.isEmpty() -> inputError = null
                    allServers.values.contains(trimmed) -> inputError = "Уже есть в списке"
                    !isValidHostname(trimmed) -> inputError = "Некорректный адрес"
                    else -> {
                        val updated = customServers + trimmed
                        customServers = updated
                        prefs.edit { putStringSet("custom_servers", updated.toSet()) }
                        newServerInput = ""
                        inputError = null
                    }
                }
            }) {

                // --- НИЖНЯЯ ЧАСТЬ (СТАТУС — ТЕПЕРЬ ОДИН ФОН) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)) // Единый мягкий фон
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isConnected = currentDns != "off" && currentDns.isNotEmpty()
                    Text(
                        text = if (isConnected) "Активен: $currentDns" else "Приватный DNS выключен",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) Color(0xFF007DFF).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.4f
                        )
                    )
                }
            }
        }
    }
}


