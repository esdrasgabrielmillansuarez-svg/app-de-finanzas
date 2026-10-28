package com.suarezmillan.finanzas.ui

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suarezmillan.finanzas.core.export.CsvExporter
import com.suarezmillan.finanzas.core.export.shareCsv
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.ui.ajustes.SettingsScreen
import com.suarezmillan.finanzas.ui.divisas.CurrenciesScreen
import com.suarezmillan.finanzas.ui.home.HomeScreen
import com.suarezmillan.finanzas.ui.movs.MovementsScreen
import com.suarezmillan.finanzas.ui.movs.TxnDetailDialog
import com.suarezmillan.finanzas.ui.plan.PlanScreen
import com.suarezmillan.finanzas.ui.quick.QuickEntrySheet

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
    val lastPaid by vm.lastPaid.collectAsStateWithLifecycle()
    val lastDeleted by vm.lastDeleted.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val receipt by vm.receipt.collectAsStateWithLifecycle()
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val movements by vm.movements.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var showCurrencies by rememberSaveable { mutableStateOf(false) }
    var detailTxn by remember { mutableStateOf<TxnEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(lastSaved) {
        val txn = lastSaved ?: return@LaunchedEffect
        val label = txn.merchant?.takeIf { it.isNotBlank() } ?: "Movimiento"
        val result = snackbar.showSnackbar(
            message = "$label guardado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoLast() else vm.clearLastSaved()
    }

    LaunchedEffect(lastPaid) {
        val paid = lastPaid ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "${paid.payment.name} pagado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoPayment() else vm.clearLastPaid()
    }

    LaunchedEffect(lastDeleted) {
        val txn = lastDeleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "Movimiento eliminado",
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

    if (showCurrencies) {
        CurrenciesScreen(
            quotes = quotes,
            baseCurrency = state.baseCurrency,
            secondaryCurrency = state.secondaryCurrency,
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
                    onSetLimit = { categoryId, limit -> vm.setEnvelopeLimit(categoryId, limit) },
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
                    onOpenCurrencies = { showCurrencies = true },
                    onExportCsv = { exportCsv() },
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
            onDismissRequest = {
                quickOpen = false
                vm.clearReceipt()
            },
            sheetState = sheetState,
        ) {
            QuickEntrySheet(
                state = state,
                receipt = receipt,
                onScan = { vm.analyzeReceipt(it) },
                onClearReceipt = { vm.clearReceipt() },
                onSave = { type, amountMinor, currency, categoryId, merchant ->
                    vm.saveQuickEntry(type, amountMinor, currency, categoryId, merchant)
                    quickOpen = false
                },
            )
        }
    }
}
