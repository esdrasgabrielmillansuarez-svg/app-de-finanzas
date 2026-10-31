package com.suarezmillan.finanzas.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suarezmillan.finanzas.core.coach.CoachAction
import com.suarezmillan.finanzas.core.coach.CoachContext
import com.suarezmillan.finanzas.core.coach.CoachEngine
import com.suarezmillan.finanzas.core.coach.DuePayment
import com.suarezmillan.finanzas.core.export.CsvExporter
import com.suarezmillan.finanzas.core.export.buildMonthlyReportPdf
import com.suarezmillan.finanzas.core.export.shareCsv
import com.suarezmillan.finanzas.core.export.sharePdf
import com.suarezmillan.finanzas.core.receipt.ReceiptSource
import com.suarezmillan.finanzas.core.report.ReportEnvelope
import com.suarezmillan.finanzas.core.report.buildMonthlyReport
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.ui.ajustes.RatesSettingsScreen
import com.suarezmillan.finanzas.ui.ajustes.SettingsScreen
import com.suarezmillan.finanzas.ui.divisas.CurrenciesScreen
import com.suarezmillan.finanzas.ui.home.HomeScreen
import com.suarezmillan.finanzas.ui.movs.MovementsScreen
import com.suarezmillan.finanzas.ui.movs.TxnDetailDialog
import com.suarezmillan.finanzas.ui.plan.PlanScreen
import com.suarezmillan.finanzas.ui.quick.QuickEntrySheet
import com.suarezmillan.finanzas.ui.receipt.ReceiptBatchSheet
import com.suarezmillan.finanzas.ui.receipt.ReceiptCameraDialog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Hoy", Icons.Rounded.Home),
    Tab("Movs", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab("Plan", Icons.Rounded.CalendarMonth),
    Tab("Ajustes", Icons.Rounded.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanzasRoot(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    vm: MainViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lastSaved by vm.lastSaved.collectAsStateWithLifecycle()
    val lastSavedCount by vm.lastSavedCount.collectAsStateWithLifecycle()
    val lastPaid by vm.lastPaid.collectAsStateWithLifecycle()
    val lastDeleted by vm.lastDeleted.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val receipts by vm.receipts.collectAsStateWithLifecycle()
    val lastBatch by vm.lastBatch.collectAsStateWithLifecycle()
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val vesReferences by vm.vesReferences.collectAsStateWithLifecycle()
    val activeVesReference by vm.activeVesReference.collectAsStateWithLifecycle()
    val vesManualRates by vm.vesManualRates.collectAsStateWithLifecycle()
    val liveRates by vm.liveRates.collectAsStateWithLifecycle()
    val manualRates by vm.manualRates.collectAsStateWithLifecycle()
    val rateRefresh by vm.rateRefresh.collectAsStateWithLifecycle()
    val movements by vm.movements.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val themePreset by vm.themePreset.collectAsStateWithLifecycle()
    val remindersEnabled by vm.remindersEnabled.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var showCurrencies by rememberSaveable { mutableStateOf(false) }
    var showRates by rememberSaveable { mutableStateOf(false) }
    var coachVariant by rememberSaveable { mutableIntStateOf(0) }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    var detailTxn by remember { mutableStateOf<TxnEntity?>(null) }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    var batchOpen by rememberSaveable { mutableStateOf(false) }
    var pickImages by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ------------------------------------------------------------------ sticker contextual
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    fun daysUntil(millis: Long): Int = ChronoUnit.DAYS
        .between(today, Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
        .toInt()

    val dueSoon = payments
        .map { it to daysUntil(it.dueAt) }
        .filter { it.second <= 3 }
        .sortedBy { it.second }

    val coachMessage = CoachEngine.message(
        context = CoachContext(
            movementCount = movements.size,
            spentMinor = state.spentMinor,
            // El "presupuesto" del mes ahora son los ingresos reales registrados.
            budgetLimitMinor = state.incomeMinor,
            daysLeftInMonth = today.lengthOfMonth() - today.dayOfMonth + 1,
            overBudgetEnvelopes = state.envelopes
                .filter { it.currency == state.baseCurrency && it.isOver }
                .map { it.name },
            nearLimitEnvelopes = state.envelopes
                .filter { it.currency == state.baseCurrency && it.isWarning }
                .map { it.name },
            duePayments = dueSoon.map { (payment, days) ->
                DuePayment(payment.name, days, payment.currency)
            },
            currency = state.baseCurrency,
        ),
        variant = coachVariant,
    )

    // ------------------------------------------------------------------ facturas
    // Hasta 10 fotos por tanda: el OCR las lee en cola, una detrás de otra.
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            vm.addReceipts(uris, ReceiptSource.GALLERY)
            batchOpen = true
        }
    }

    LaunchedEffect(pickImages) {
        if (!pickImages) return@LaunchedEffect
        pickImages = false
        galleryPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    LaunchedEffect(lastBatch) {
        if (lastBatch.isEmpty()) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = if (lastBatch.size == 1) {
                "🧾 1 gasto guardado"
            } else {
                "🧾 ${lastBatch.size} gastos guardados"
            },
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoLastBatch() else vm.clearLastBatch()
    }

    // ------------------------------------------------------------------ permiso de notificaciones
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { notificationAsked = true }

    LaunchedEffect(dueSoon.isNotEmpty(), remindersEnabled) {
        if (dueSoon.isEmpty() || !remindersEnabled || notificationAsked) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(lastSaved) {
        val txn = lastSaved ?: return@LaunchedEffect
        val label = txn.merchant?.takeIf { it.isNotBlank() } ?: "Movimiento"
        val milestone = CoachEngine.milestone(lastSavedCount ?: 0)
        val result = snackbar.showSnackbar(
            message = milestone ?: "🧾 $label guardado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoLast() else vm.clearLastSaved()
    }

    LaunchedEffect(lastPaid) {
        val paid = lastPaid ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "✅ ${paid.payment.name} pagado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoPayment() else vm.clearLastPaid()
    }

    LaunchedEffect(lastDeleted) {
        val txn = lastDeleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "🗑️ Movimiento eliminado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoDelete() else vm.clearLastDeleted()
    }

    fun exportCsv() {
        val names = state.categories.associate { it.id to it.name }
        val csv = CsvExporter.build(
            txns = movements,
            categoryName = { id -> id?.let { names[it] } ?: "" },
        )
        shareCsv(
            context = context,
            csv = csv,
            fileName = "movimientos-${java.time.LocalDate.now()}.csv",
        )
    }

    /** Reporte del mes en curso, con la tasa histórica de cada movimiento. */
    fun exportPdf() {
        val month = YearMonth.now()
        val names = state.categories.associate { it.id to it.name }
        val report = buildMonthlyReport(
            month = month,
            baseCurrency = state.baseCurrency,
            txns = movements,
            categoryName = { id -> id?.let { names[it] } ?: "" },
            envelopes = state.envelopes.map { envelope ->
                ReportEnvelope(
                    name = envelope.name,
                    emoji = envelope.emoji,
                    currency = envelope.currency,
                    limitMinor = envelope.limitMinor,
                    spentMinor = envelope.spentMinor,
                    remainingMinor = envelope.remainingMinor,
                    windowLabel = envelope.windowLabel,
                )
            },
            currentRate = vm::rateBetween,
        )
        sharePdf(
            context = context,
            bytes = buildMonthlyReportPdf(report),
            fileName = "reporte-$month.pdf",
        )
    }

    if (showRates) {
        RatesSettingsScreen(
            activeVesReference = activeVesReference,
            vesReferences = vesReferences,
            vesManualRates = vesManualRates,
            liveRates = liveRates,
            manualRates = manualRates,
            refreshState = rateRefresh,
            onBack = { showRates = false },
            onSelectVesReference = { vm.setActiveVesReference(it) },
            onSetVesManualRate = { reference, value ->
                vm.setVesManualRate(reference, value)
            },
            onClearVesManualRate = { vm.clearVesManualRate(it) },
            onSetManualRate = { code, value -> vm.setManualRate(code, value) },
            onClearManualRate = { vm.clearManualRate(it) },
            onRefresh = { vm.refreshRates(force = true) },
        )
        return
    }

    if (showCurrencies) {
        CurrenciesScreen(
            quotes = quotes,
            baseCurrency = state.baseCurrency,
            secondaryCurrency = state.secondaryCurrency,
            activeVesReference = activeVesReference,
            vesReferences = vesReferences,
            vesManualRates = vesManualRates,
            refreshState = rateRefresh,
            onRefreshRate = { vm.refreshRates(force = true) },
            onSelectVesReference = { vm.setActiveVesReference(it) },
            onBack = { showCurrencies = false },
            onSetBase = { vm.setBaseCurrency(it) },
            onSetSecondary = { vm.setSecondaryCurrency(it) },
            onSetManualRate = { code, value -> vm.setManualRate(code, value) },
            onClearManualRate = { vm.clearManualRate(it) },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = item.label,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { quickOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Registrar movimiento")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    userName = userName,
                    coach = coachMessage,
                    onAnotherCoach = { coachVariant++ },
                    onCoachAction = { action ->
                        when (action) {
                            CoachAction.OPEN_PLAN -> tab = 2
                            CoachAction.OPEN_QUICK -> quickOpen = true
                        }
                    },
                    onOpenPlan = { tab = 2 },
                    onOpenCurrencies = { showCurrencies = true },
                    onTxnClick = { detailTxn = it },
                )
                1 -> MovementsScreen(
                    txns = movements,
                    categories = state.categories,
                    onTxnClick = { detailTxn = it },
                )
                2 -> PlanScreen(
                    state = state,
                    payments = payments,
                    onSaveEnvelope = { vm.saveEnvelope(it) },
                    onRemoveEnvelope = { vm.removeEnvelope(it) },
                    onCreateCategory = { name, emoji, kind -> vm.createCategory(name, emoji, kind) },
                    onAddPayment = { name, amount, dueAt, recurrence, categoryId ->
                        vm.addScheduledPayment(name, amount, dueAt, recurrence, categoryId)
                    },
                    onPay = { vm.markPaymentPaid(it) },
                    onDeletePayment = { vm.deletePayment(it) },
                )
                else -> SettingsScreen(
                    state = state,
                    userName = userName,
                    userEmail = userEmail,
                    movementCount = movements.size,
                    themeMode = themeMode,
                    themePreset = themePreset,
                    remindersEnabled = remindersEnabled,
                    vesReferenceLabel = activeVesReference.label,
                    onThemeMode = { vm.setThemeMode(it) },
                    onThemePreset = { vm.setThemePreset(it) },
                    onReminders = { vm.setRemindersEnabled(it) },
                    onOpenCurrencies = { showCurrencies = true },
                    onOpenRates = { showRates = true },
                    onExportCsv = { exportCsv() },
                    onExportPdf = { exportPdf() },
                    onClearMovements = { vm.clearAllMovements() },
                    onLogout = onLogout,
                )
            }
        }
    }

    detailTxn?.let { txn ->
        TxnDetailDialog(
            txn = txn,
            categoryName = state.categories.firstOrNull { it.id == txn.categoryId }?.name,
            onDuplicate = {
                vm.duplicateTxn(txn)
                detailTxn = null
            },
            onDelete = {
                vm.deleteTxn(txn)
                detailTxn = null
            },
            onDismiss = { detailTxn = null },
        )
    }

    if (quickOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { quickOpen = false },
            sheetState = sheetState,
        ) {
            QuickEntrySheet(
                state = state,
                pendingReceipts = receipts.size,
                onOpenCamera = {
                    quickOpen = false
                    cameraOpen = true
                },
                onPickImages = {
                    quickOpen = false
                    pickImages = true
                },
                onOpenReceipts = {
                    quickOpen = false
                    batchOpen = true
                },
                onCreateCategory = { name, emoji, kind -> vm.createCategory(name, emoji, kind) },
                onSave = { type, amountMinor, currency, categoryId, merchant ->
                    vm.saveQuickEntry(type, amountMinor, currency, categoryId, merchant)
                    quickOpen = false
                },
            )
        }
    }

    if (cameraOpen) {
        ReceiptCameraDialog(
            onCaptured = { uri -> vm.addReceipts(listOf(uri), ReceiptSource.CAMERA) },
            onDismiss = {
                cameraOpen = false
                if (vm.receipts.value.isNotEmpty()) batchOpen = true
            },
        )
    }

    if (batchOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { batchOpen = false },
            sheetState = sheetState,
        ) {
            ReceiptBatchSheet(
                drafts = receipts,
                categories = state.categories.filter { it.kind == CategoryKind.EXPENSE },
                baseCurrency = state.baseCurrency,
                secondaryCurrency = state.secondaryCurrency,
                onAmount = { id, minor -> vm.updateReceiptAmount(id, minor) },
                onStartBreakdown = { vm.startReceiptBreakdown(it) },
                onAddPart = { vm.addReceiptPart(it) },
                onPartAmount = { id, partId, minor ->
                    vm.updateReceiptPart(id, partId, minor)
                },
                onRemovePart = { id, partId -> vm.removeReceiptPart(id, partId) },
                onMerchant = { id, text -> vm.updateReceiptMerchant(id, text) },
                onCurrency = { id, code -> vm.updateReceiptCurrency(id, code) },
                onCategory = { id, categoryId -> vm.updateReceiptCategory(id, categoryId) },
                onToggle = { vm.toggleReceipt(it) },
                onRemove = { vm.removeReceipt(it) },
                onCreateCategory = { name, emoji ->
                    vm.createCategory(name, emoji, CategoryKind.EXPENSE)
                },
                onCamera = {
                    batchOpen = false
                    cameraOpen = true
                },
                onGallery = {
                    batchOpen = false
                    pickImages = true
                },
                onClear = { vm.clearReceipts() },
                onSave = {
                    vm.saveReceipts()
                    batchOpen = false
                },
            )
        }
    }
}
