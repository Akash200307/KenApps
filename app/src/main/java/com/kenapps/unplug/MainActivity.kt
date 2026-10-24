package com.kenapps.unplug

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenapps.unplug.data.AppSettings
import com.kenapps.unplug.data.DiagnosticNotes
import com.kenapps.unplug.data.DiagnosticPreferences
import com.kenapps.unplug.data.Palette
import com.kenapps.unplug.data.ThemeMode
import com.kenapps.unplug.monitor.CompatibilityService
import com.kenapps.unplug.monitor.CompatibilityStatus
import com.kenapps.unplug.monitor.NativeAlerts
import com.kenapps.unplug.monitor.TestStatus
import com.kenapps.unplug.ui.KenAppsTheme
import com.kenapps.unplug.ui.accent
import com.kenapps.unplug.ui.description
import com.kenapps.unplug.ui.label
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NativeAlerts(this)
        setContent { KenApps() }
    }

    override fun onResume() {
        super.onResume()
        CompatibilityStatus.read(this)
        if (CompatibilityStatus.state.value.running) command("com.kenapps.unplug.REFRESH")
    }

    private fun command(action: String) {
        val intent = Intent(this, CompatibilityService::class.java).setAction(action)
        try {
            if (action == CompatibilityService.START) startForegroundService(intent)
            else if (CompatibilityStatus.state.value.running) startService(intent)
        } catch (exception: RuntimeException) {
            CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(
                error = "Could not update monitoring: ${exception.message}"
            ))
        }
    }

    private fun enableMonitoring() {
        CompatibilityStatus.read(this)
        if (!CompatibilityStatus.state.value.promotionAllowed) {
            CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(
                error = "Enable Live Alerts for KenApps, then switch reminders on."
            ))
            notificationSettings(true)
        } else command(CompatibilityService.START)
    }

    private fun notificationSettings(promoted: Boolean) {
        val intent = Intent(if (promoted) Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
            else Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    @Composable private fun KenApps() {
        val status by CompatibilityStatus.state.collectAsStateWithLifecycle()
        val preferences = remember { DiagnosticPreferences(applicationContext) }
        val settings by preferences.settings.collectAsStateWithLifecycle(AppSettings())
        val notes by preferences.notes.collectAsStateWithLifecycle(DiagnosticNotes())
        val scope = rememberCoroutineScope()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val scrolls = listOf(rememberScrollState(), rememberScrollState(), rememberScrollState())
        val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            CompatibilityStatus.read(this@MainActivity)
            if (granted) enableMonitoring()
        }
        LaunchedEffect(notes) {
            CompatibilityStatus.update(this@MainActivity, CompatibilityStatus.state.value.copy(
                oxygenVersion = notes.oxygenVersion.ifBlank { "Not recorded" }, observation = notes.observation
            ))
        }
        LaunchedEffect(settings) {
            CompatibilityStatus.update(this@MainActivity, CompatibilityStatus.state.value.copy(
                palette = settings.palette.name, themeMode = settings.themeMode.name, vibration = settings.vibration
            ))
        }
        val toggle: (Boolean) -> Unit = { enabled ->
            if (enabled) {
                if (!status.notificationsAllowed) permissions.launch(Manifest.permission.POST_NOTIFICATIONS)
                else enableMonitoring()
            } else scope.launch {
                preferences.setMonitoringRequested(false)
                command(CompatibilityService.STOP)
            }
        }

        KenAppsTheme(settings) {
            Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    listOf("Home" to R.drawable.ic_home, "Appearance" to R.drawable.ic_palette,
                        "Settings" to R.drawable.ic_settings).forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index },
                            icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer))
                    }
                }
            }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 600.dp).fillMaxSize().imePadding()
                        .verticalScroll(scrolls[tab]).padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Header(tab)
                        when (tab) {
                            0 -> Home(status, settings, toggle,
                                onTest = { command(CompatibilityService.TEST) },
                                onPause = { command(if (status.pausedUntil > 0) CompatibilityService.RESUME else CompatibilityService.PAUSE) },
                                onSettings = { tab = 2 })
                            1 -> Appearance(settings,
                                onPalette = { scope.launch { preferences.savePalette(it) } },
                                onMode = { scope.launch { preferences.saveThemeMode(it) } })
                            2 -> SettingsPage(status, settings, notes,
                                onVibration = { scope.launch { preferences.saveVibration(it) } },
                                onPause = { command(if (status.pausedUntil > 0) CompatibilityService.RESUME else CompatibilityService.PAUSE) },
                                onEnable = { toggle(true) },
                                onSaveVersion = { scope.launch { preferences.saveOxygen(it) } },
                                onObservation = { scope.launch { preferences.saveObservation(it) } })
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable private fun SettingsPage(
        status: TestStatus, settings: AppSettings, notes: DiagnosticNotes,
        onVibration: (Boolean) -> Unit, onPause: () -> Unit, onEnable: () -> Unit,
        onSaveVersion: (String) -> Unit, onObservation: (String) -> Unit,
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var oxygenInput by remember(notes.oxygenVersion) { mutableStateOf(notes.oxygenVersion) }
        var copied by remember { mutableStateOf(false) }
        Section("Make it yours") {
            ToggleRow("Gentle vibration", "Applies to the next reminder. Android channel settings can override it.",
                settings.vibration, onVibration)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Reminders are silent by default. Your phone stays fully usable.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("Monitoring") {
            ResultLine("Status", if (status.running) if (status.pausedUntil > 0) "Paused" else "Active" else "Off")
            if (status.running) OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                Text(if (status.pausedUntil > 0) "Resume reminders" else "Pause for 15 minutes")
            }
            else if (settings.monitoringRequested) {
                Text("Monitoring was interrupted. Restart it after a reboot, force-stop, or permission change.",
                    style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("Restart reminders") }
            }
            Text("An ongoing notification shows when monitoring is active. If OxygenOS stops the app in the background, reopen KenApps and restart reminders. No background battery setting has been changed automatically.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("System permissions") {
            ResultLine("Notifications", if (status.notificationsAllowed) "Allowed" else "Off")
            TextButton(onClick = { notificationSettings(false) }) { Text("Notification settings") }
            ResultLine("Native Live Alerts", if (status.promotionAllowed) "Allowed" else "Off")
            TextButton(onClick = { notificationSettings(true) }) { Text("Live Alerts settings") }
            Text("Enable notifications and Live Alerts for KenApps in the screens above. The native pill’s layout, colors, and placement belong to OxygenOS.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("Your device") {
            ResultLine("Model", "${Build.MANUFACTURER} ${Build.MODEL}")
            ResultLine("Android", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
            Text("Build ${Build.DISPLAY}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = oxygenInput, onValueChange = { oxygenInput = it.take(120) },
                label = { Text("OxygenOS version (optional)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(16.dp))
            TextButton(onClick = { onSaveVersion(oxygenInput.trim()) }) { Text("Save version") }
        }
        Section("Personal build") {
            Text("The native pill was confirmed on OnePlus CPH2691 with Android 17. Android’s Live Updates guidance excludes ordinary alerts, so this personal APK is experimental and may behave differently after OS updates. It is not a Play Store release.",
                style = MaterialTheme.typography.bodyMedium)
            Text("0.2.0 · on-device only · no account or network access", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide diagnostics" else "Device diagnostics") }
            if (expanded) {
                ResultLine("Structure eligible", status.structurallyEligible?.let { yesNo(it) } ?: "Not tested")
                ResultLine("Reminder posted", yesNo(status.posted))
                ResultLine("System promoted flag", yesNo(status.promoted))
                Text(status.lastEvent, style = MaterialTheme.typography.bodySmall)
                Text("Flags alone do not prove native pill visibility. Record what you actually see.", style = MaterialTheme.typography.bodySmall)
                listOf("Native pill visible", "Notification only", "Nothing appeared").forEach { observation ->
                    OutlinedButton(onClick = { onObservation(observation) }, modifier = Modifier.fillMaxWidth()) {
                        Text((if (notes.observation == observation) "✓  " else "") + observation)
                    }
                }
                TextButton(onClick = { CompatibilityStatus.read(this@MainActivity, "Results refreshed") }) { Text("Refresh results") }
                TextButton(onClick = {
                    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(
                        "KenApps diagnostic report", CompatibilityStatus.json().toString(2)))
                    copied = true
                }) { Text(if (copied) "Report copied" else "Copy diagnostic report") }
            }
        }
    }
}

@Composable private fun Header(tab: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_unplug), null, Modifier.padding(14.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(if (tab == 0) "KenApps" else if (tab == 1) "Your kind of calm" else "A few little details",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(if (tab == 0) "UNPLUG REMINDER" else if (tab == 1) "APPEARANCE" else "SETTINGS",
                Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun ColumnScope.Home(
    status: TestStatus, settings: AppSettings, onToggle: (Boolean) -> Unit,
    onTest: () -> Unit, onPause: () -> Unit, onSettings: () -> Unit,
) {
    val color = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BatteryRing(status)
        Text(when {
            status.testMode -> "A little test moment."
            status.pausedUntil > 0 -> "Take your time."
            status.plugged -> "A moment to unplug."
            else -> "You’re free to go."
        }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
        StatusPill(when {
            !status.notificationsAllowed || !status.promotionAllowed -> "Setup needed"
            status.pausedUntil > 0 -> "Reminders paused for 15 minutes"
            status.running && status.plugged -> "Charging · reminders ready"
            !status.plugged -> "Unplugged"
            settings.monitoringRequested -> "Monitoring interrupted"
            else -> "Ready when you are"
        })
    }
    Section {
        ToggleRow("Enable reminders", "A gentle nudge when you unlock while plugged in.", status.running, onToggle)
        if (!status.running && settings.monitoringRequested) {
            Text("Monitoring stopped. Switch on to restart after a reboot, force-stop, or permission change.",
                style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
        }
    }
    if (status.error != null) {
        Surface(shape = RoundedCornerShape(18.dp), color = color.errorContainer) {
            Text(status.error, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = color.onErrorContainer)
        }
    }
    Section {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (status.promotionAllowed) color.primary else color.outline, CircleShape))
            Column(Modifier.weight(1f)) {
                Text("Native OxygenOS Live Alerts", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(if (status.promotionAllowed) "Allowed on this device · personal build" else "Enable Live Alerts for KenApps in Settings",
                    style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
            }
        }
        TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) { Text("View device & permissions") }
    }
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onTest,
            enabled = status.running && status.pausedUntil == 0L && status.plugged && status.unlocked,
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("Test reminder") }
        OutlinedButton(onClick = onPause, enabled = status.running,
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
            Text(if (status.pausedUntil > 0) "Resume" else "Pause 15 min")
        }
    }
    Text(if (status.testMode) "Test mode · the preview clears after 15 seconds. Locking or unplugging also clears it."
        else "To test, enable reminders and keep the charger connected. Your phone stays fully usable.",
        style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
    Text("A small pause. A little more presence.", Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
        style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
}

@Composable private fun Appearance(settings: AppSettings, onPalette: (Palette) -> Unit, onMode: (ThemeMode) -> Unit) {
    Text("Choose the mood you want to come back to.", style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Section("Color palette") {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Palette.entries.forEach { palette ->
                val selected = settings.palette == palette
                Row(Modifier.fillMaxWidth().border(if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(18.dp)).selectable(selected, role = Role.RadioButton, onClick = { onPalette(palette) })
                    .padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(palette.accent, CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(palette.label, fontWeight = FontWeight.SemiBold)
                        Text(palette.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(selected = selected, onClick = null)
                }
            }
        }
    }
    Section("Light & shade") {
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().selectable(settings.themeMode == mode, role = Role.RadioButton,
                    onClick = { onMode(mode) }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.themeMode == mode, onClick = null)
                    Text(mode.label, Modifier.padding(start = 12.dp))
                }
            }
        }
    }
    Section("Live preview") {
        Text("A moment to unplug.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        Text("${settings.palette.label} · ${settings.themeMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusPill("Charging · reminders ready")
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(14.dp)) {
            Text("Your next quiet moment", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimary)
        }
    }
    Text("Changes save immediately. Themes style KenApps; OxygenOS controls the native pill. Animations follow your system animation settings.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun BatteryRing(status: TestStatus) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.primaryContainer
    val percent = status.batteryPercent
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    // Compose's animation clock respects the system duration scale, including 0.
    val fraction by animateFloatAsState(if (entered) (percent ?: 0) / 100f else 0f, tween(650), label = "battery ring")
    val ringSize = 208.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.4f)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.padding(vertical = 12.dp).size(minOf(ringSize, maxWidth))
            .semantics(mergeDescendants = true) {
                contentDescription = "Battery ${percent?.let { "$it percent" } ?: "unknown"}, ${if (status.plugged) "connected to power" else "unplugged"}"
            }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 12.dp.toPx()
                val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                drawArc(track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                drawArc(accent, 135f, 270f * fraction, false, Offset(inset, inset), arcSize,
                    style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(percent?.let { "$it%" } ?: "—", fontSize = 52.sp, fontWeight = FontWeight.Light)
                Text(if (status.plugged) "CONNECTED" else "UNPLUGGED", fontSize = 10.sp, letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun StatusPill(text: String) {
    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable private fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable private fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange)
        .heightIn(min = 64.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun yesNo(value: Boolean) = if (value) "Yes" else "No"
