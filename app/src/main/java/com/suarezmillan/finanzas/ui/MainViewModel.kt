package com.suarezmillan.finanzas.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suarezmillan.finanzas.core.currency.CurrencyQuote
import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.fx.FxSource
import com.suarezmillan.finanzas.core.receipt.ReceiptAnalysis
import com.suarezmillan.finanzas.core.receipt.ReceiptScanner
import com.suarezmillan.finanzas.core.reminder.ReminderScheduler
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.core.settings.ThemeMode
import com.suarezmillan.finanzas.core.settings.ThemePreset
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.PaymentStatus
import com.suarezmillan.finanzas.data.local.entity.Recurrence
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import com.suarezmillan.finanzas.data.repo.BudgetRepository
import com.suarezmillan.finanzas.data.repo.CategoryRepository
import com.suarezmillan.finanzas.data.repo.MonthSummary
import com.suarezmillan.finanzas.data.repo.ScheduledPaymentRepository
import com.suarezmillan.finanzas.data.repo.TxnRepository
import com.suarezmillan.finanzas.data.repo.monthKeyOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val baseCurrency: String = "ARS",
    val secondaryCurrency: String = "USD",
    val rate: Double = 0.0,
    val rateIsManual: Boolean = false,
    val balanceMinor: Long = 0L,
    val secondaryBalanceMinor: Long = 0L,
    val spentMinor: Long = 0L,
    val incomeMinor: Long = 0L,
    val budgetLimitMinor: Long = 0L,
    val projectedCloseMinor: Long = 0L,
    val envelopes: List<EnvelopeProgress> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val recent: List<TxnEntity> = emptyList(),
) {
    /** "Te queda este mes": lo que resta en cada sobre de la divisa base. */
    val remainingMinor: Long
        get() = envelopes
            .filter { it.currency == baseCurrency }
            .sumOf { it.remainingMinor }

    val usedRatio: Float
        get() = if (budgetLimitMinor <= 0L) 0f
        else (spentMinor.toFloat() / budgetLimitMinor).coerceIn(0f, 1f)
}

sealed interface ReceiptUiState {
    data object Idle : ReceiptUiState
    data object Loading : ReceiptUiState
    data class Done(val analysis: ReceiptAnalysis) : ReceiptUiState
    data class Failed(val message: String) : ReceiptUiState
}

data class PaidPayment(
    val payment: ScheduledPaymentEntity,
    val txn: TxnEntity,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val txnRepository: TxnRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentRepository: ScheduledPaymentRepository,
    private val settings: AppSettings,
    private val rates: CurrencyRates,
    private val receiptScanner: ReceiptScanner,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val monthKey = monthKeyOf(System.currentTimeMillis())
    private val month = YearMonth.now()
    private val zone = ZoneId.systemDefault()
    private val fromMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val toMillis = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private val _lastSaved = MutableStateFlow<TxnEntity?>(null)
    val lastSaved: StateFlow<TxnEntity?> = _lastSaved.asStateFlow()

    /** Total de movimientos al momento de guardar (mensajes de hito). */
    private val _lastSavedCount = MutableStateFlow<Int?>(null)
    val lastSavedCount: StateFlow<Int?> = _lastSavedCount.asStateFlow()

    private val _lastPaid = MutableStateFlow<PaidPayment?>(null)
    val lastPaid: StateFlow<PaidPayment?> = _lastPaid.asStateFlow()

    /** Para el "Deshacer" después de eliminar un movimiento. */
    private val _lastDeleted = MutableStateFlow<TxnEntity?>(null)
    val lastDeleted: StateFlow<TxnEntity?> = _lastDeleted.asStateFlow()

    /** Lista completa (pantalla Movimientos, filtros y exportación CSV). */
    val movements: StateFlow<List<TxnEntity>> = txnRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _receipt = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Idle)
    val receipt: StateFlow<ReceiptUiState> = _receipt.asStateFlow()

    /** Catálogo completo con equivalentes en USD y EUR, recalculado si hay tasas manuales. */
    val quotes: StateFlow<List<CurrencyQuote>> = rates.quotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rates.quotes())

    private data class CurrencyContext(
        val base: String,
        val secondary: String,
        val rate: Double,
        val manual: Boolean,
    )

    private data class Snapshot(
        val context: CurrencyContext,
        val balanceMinor: Long,
        val secondaryBalanceMinor: Long,
        val summary: MonthSummary,
        val envelopes: List<EnvelopeProgress>,
        val categories: List<CategoryEntity>,
    )

    private val context = combine(
        settings.baseCurrency,
        settings.secondaryCurrency,
        settings.manualUnitsPerUsd,
    ) { base, secondary, _ ->
        CurrencyContext(
            base = base,
            secondary = secondary,
            rate = rates.rate(secondary, base),
            manual = settings.isManual(secondary),
        )
    }

    private val snapshot: Flow<Snapshot> = context.flatMapLatest { ctx ->
        combine(
            txnRepository.observeNetBalance(ctx.base),
            txnRepository.observeNetBalance(ctx.secondary),
            txnRepository.observeMonthSummary(ctx.base, fromMillis, toMillis),
            budgetRepository.observeEnvelopes(monthKey),
            categoryRepository.observeAll(),
        ) { balance, secondaryBalance, summary, envelopes, categories ->
            Snapshot(ctx, balance, secondaryBalance, summary, envelopes, categories)
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        snapshot,
        txnRepository.observeRecent(20),
    ) { snap, recent ->
        val limit = snap.envelopes.filter { it.currency == snap.context.base }.sumOf { it.limitMinor }
        HomeUiState(
            baseCurrency = snap.context.base,
            secondaryCurrency = snap.context.secondary,
            rate = snap.context.rate,
            rateIsManual = snap.context.manual,
            balanceMinor = snap.balanceMinor,
            secondaryBalanceMinor = snap.secondaryBalanceMinor,
            spentMinor = snap.summary.spentMinor,
            incomeMinor = snap.summary.incomeMinor,
            budgetLimitMinor = limit,
            projectedCloseMinor = projectClose(snap),
            envelopes = snap.envelopes,
            categories = snap.categories,
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val payments: StateFlow<List<ScheduledPaymentEntity>> = paymentRepository.observeUnpaid()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun projectClose(snap: Snapshot): Long {
        val today = java.time.LocalDate.now()
        val elapsed = today.dayOfMonth.coerceAtLeast(1)
        val daysInMonth = today.lengthOfMonth()
        val burnPerDay = snap.summary.spentMinor / elapsed
        val projectedSpend = burnPerDay * daysInMonth
        return snap.balanceMinor - (projectedSpend - snap.summary.spentMinor)
    }

    // ------------------------------------------------------------------ movimientos

    fun saveQuickEntry(
        type: TxnType,
        amountMinor: Long,
        currency: String,
        categoryId: String,
        merchant: String?,
    ) {
        if (amountMinor <= 0L) return
        viewModelScope.launch {
            val txn = buildTxn(type, amountMinor, currency, categoryId, merchant)
            publishSaved(txn)
            _receipt.value = ReceiptUiState.Idle
        }
    }

    /** Inserta y publica el conteo exacto antes de avisar (para el mensaje de hito). */
    private suspend fun publishSaved(txn: TxnEntity) {
        txnRepository.add(txn)
        _lastSavedCount.value = txnRepository.count()
        _lastSaved.value = txn
    }

    fun undoLast() {
        val txn = _lastSaved.value ?: return
        viewModelScope.launch {
            txnRepository.delete(txn)
            _lastSaved.value = null
            _lastSavedCount.value = null
        }
    }

    fun clearLastSaved() {
        _lastSaved.value = null
        _lastSavedCount.value = null
    }

    /** Repite un movimiento con la fecha de hoy (mismo importe, categoría y divisa). */
    fun duplicateTxn(txn: TxnEntity) {
        viewModelScope.launch {
            val copy = txn.copy(
                id = java.util.UUID.randomUUID().toString(),
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                transferGroupId = null,
            )
            publishSaved(copy)
        }
    }

    fun deleteTxn(txn: TxnEntity) {
        viewModelScope.launch {
            txnRepository.delete(txn)
            _lastDeleted.value = txn
        }
    }

    fun undoDelete() {
        val txn = _lastDeleted.value ?: return
        viewModelScope.launch {
            txnRepository.add(txn)
            _lastDeleted.value = null
        }
    }

    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    fun clearAllMovements() {
        viewModelScope.launch { txnRepository.deleteAll() }
    }

    private fun buildTxn(
        type: TxnType,
        amountMinor: Long,
        currency: String,
        categoryId: String?,
        merchant: String?,
    ): TxnEntity {
        val base = settings.baseCurrency.value
        val isBase = currency.equals(base, ignoreCase = true)
        val signed = if (type == TxnType.EXPENSE) -amountMinor else amountMinor
        return TxnEntity(
            type = type,
            amountMinor = signed,
            currency = currency,
            accountId = if (isBase) ACCOUNT_CASH else ACCOUNT_USD,
            categoryId = categoryId,
            merchant = merchant,
            occurredAt = System.currentTimeMillis(),
            fxRateToBase = if (isBase) null else rates.rate(currency, base),
            fxSource = when {
                isBase -> FxSource.NONE
                settings.isManual(currency) -> FxSource.MANUAL
                else -> FxSource.AUTO
            },
            fxTimestamp = if (isBase) null else System.currentTimeMillis(),
        )
    }

    // ------------------------------------------------------------------ divisas

    fun setBaseCurrency(code: String) = settings.setBaseCurrency(code)

    fun setSecondaryCurrency(code: String) = settings.setSecondaryCurrency(code)

    fun setManualRate(code: String, unitsPerUsd: Double) = settings.setManualRate(code, unitsPerUsd)

    fun clearManualRate(code: String) = settings.clearManualRate(code)

    // ------------------------------------------------------------------ apariencia

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val themePreset: StateFlow<ThemePreset> = settings.themePreset

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun setThemePreset(preset: ThemePreset) = settings.setThemePreset(preset)

    // ------------------------------------------------------------------ recordatorios

    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled

    fun setRemindersEnabled(enabled: Boolean) {
        settings.setRemindersEnabled(enabled)
        reminderScheduler.sync(enabled)
    }

    // ------------------------------------------------------------------ presupuesto

    fun setEnvelopeLimit(categoryId: String, limitMinor: Long) {
        viewModelScope.launch {
            budgetRepository.setLimit(
                categoryId = categoryId,
                monthKey = monthKey,
                limitMinor = limitMinor.coerceAtLeast(0L),
                currency = settings.baseCurrency.value,
            )
        }
    }

    // ------------------------------------------------------------------ pagos programados

    fun addScheduledPayment(
        name: String,
        amountMinor: Long,
        dueAtMillis: Long,
        recurrence: Recurrence,
        categoryId: String?,
    ) {
        if (name.isBlank() || amountMinor <= 0L) return
        viewModelScope.launch {
            val base = settings.baseCurrency.value
            paymentRepository.add(
                ScheduledPaymentEntity(
                    name = name.trim(),
                    amountMinor = amountMinor,
                    currency = base,
                    accountId = ACCOUNT_CASH,
                    categoryId = categoryId,
                    dueAt = dueAtMillis,
                    recurrence = recurrence,
                    reminderDaysBefore = 1,
                ),
            )
        }
    }

    fun markPaymentPaid(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            val txn = buildTxn(
                type = TxnType.EXPENSE,
                amountMinor = payment.amountMinor,
                currency = payment.currency,
                categoryId = payment.categoryId,
                merchant = payment.name,
            )
            txnRepository.add(txn)
            paymentRepository.setStatus(payment.id, PaymentStatus.PAID, txn.id)
            _lastPaid.value = PaidPayment(payment, txn)
        }
    }

    fun undoPayment() {
        val paid = _lastPaid.value ?: return
        viewModelScope.launch {
            txnRepository.delete(paid.txn)
            paymentRepository.setStatus(paid.payment.id, PaymentStatus.PENDING, null)
            _lastPaid.value = null
        }
    }

    fun clearLastPaid() {
        _lastPaid.value = null
    }

    fun deletePayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch { paymentRepository.delete(payment) }
    }

    // ------------------------------------------------------------------ facturas

    fun analyzeReceipt(uri: Uri) {
        _receipt.value = ReceiptUiState.Loading
        viewModelScope.launch {
            runCatching { receiptScanner.scan(uri, settings.baseCurrency.value) }
                .onSuccess { _receipt.value = ReceiptUiState.Done(it) }
                .onFailure { _receipt.value = ReceiptUiState.Failed(it.message ?: "No se pudo leer la imagen") }
        }
    }

    fun clearReceipt() {
        _receipt.value = ReceiptUiState.Idle
    }

    companion object {
        // TODO(Sprint 3): reemplazar por selección real de cuenta (AccountDao).
        const val ACCOUNT_CASH = "acc-cash"
        const val ACCOUNT_USD = "acc-usd"
    }
}
