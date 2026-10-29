package com.suarezmillan.finanzas.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suarezmillan.finanzas.core.currency.CurrencyQuote
import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.VesReference
import com.suarezmillan.finanzas.core.currency.remote.ExchangeRateUpdater
import com.suarezmillan.finanzas.core.currency.remote.RateRefreshState
import com.suarezmillan.finanzas.core.fx.FxSource
import com.suarezmillan.finanzas.core.receipt.ReceiptAnalysis
import com.suarezmillan.finanzas.core.receipt.ReceiptScanner
import com.suarezmillan.finanzas.core.reminder.ReminderScheduler
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.core.settings.ThemeMode
import com.suarezmillan.finanzas.core.settings.ThemePreset
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import com.suarezmillan.finanzas.data.local.entity.PaymentStatus
import com.suarezmillan.finanzas.data.local.entity.Recurrence
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import com.suarezmillan.finanzas.data.repo.AccountRepository
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

/** Equivalencia de un importe en las dos divisas de referencia del producto. */
data class AmountEquivalence(
    val usdMinor: Long? = null,
    val vesMinor: Long? = null,
)

data class HomeUiState(
    val baseCurrency: String = "VES",
    val secondaryCurrency: String = "USD",
    val rate: Double = 0.0,
    val rateIsManual: Boolean = false,
    val balanceMinor: Long = 0L,
    val secondaryBalanceMinor: Long = 0L,
    val spentMinor: Long = 0L,
    val incomeMinor: Long = 0L,
    /** Suma de los límites de sobres del mes; no es el dinero disponible. */
    val budgetLimitMinor: Long = 0L,
    val projectedCloseMinor: Long = 0L,
    val envelopes: List<EnvelopeProgress> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val recent: List<TxnEntity> = emptyList(),
    /** Gastos del mes que no se pudieron convertir porque falta una tasa. */
    val missingRateExpenses: Int = 0,
    /** 1 USD expresado en VES con la referencia activa; `null` si no hay tasa. */
    val usdToVesRate: Double? = null,
    val remainingEquivalent: AmountEquivalence = AmountEquivalence(),
    val spentEquivalent: AmountEquivalence = AmountEquivalence(),
    val envelopeEquivalents: Map<String, AmountEquivalence> = emptyMap(),
) {
    /**
     * "Te queda este mes": ingresos reales menos todos los gastos del mes.
     *
     * Los gastos en otras divisas se convierten a [baseCurrency]; si falta la
     * tasa, [missingRateExpenses] lo avisa y ese gasto no se resta. Los límites
     * de sobres no se suman aquí: son topes por categoría, no ingresos.
     */
    val remainingMinor: Long
        get() = incomeMinor - spentMinor

    val hasIncome: Boolean get() = incomeMinor > 0L

    val hasMonthActivity: Boolean get() = incomeMinor > 0L || spentMinor > 0L

    /** Proporción del ingreso mensual ya gastada. */
    val usedRatio: Float
        get() = if (incomeMinor <= 0L) 0f
        else (spentMinor.toFloat() / incomeMinor).coerceIn(0f, 1f)
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
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentRepository: ScheduledPaymentRepository,
    private val settings: AppSettings,
    private val rates: CurrencyRates,
    private val receiptScanner: ReceiptScanner,
    private val reminderScheduler: ReminderScheduler,
    private val rateUpdater: ExchangeRateUpdater,
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

    /** Catálogo completo con equivalentes en USD y EUR, recalculado si cambia una tasa. */
    val quotes: StateFlow<List<CurrencyQuote>> = rates.quotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), rates.quotes())

    /** Referencias del bolívar: BCV, paralelo y USDT. */
    val vesReferences: StateFlow<Map<VesReference, LiveRate>> = settings.vesReferences
    val activeVesReference: StateFlow<VesReference> = settings.activeVesReference
    val vesManualRates: StateFlow<Map<VesReference, Double>> = settings.vesManualRates

    /** Otras referencias en vivo (EUR, ARS) y sus overrides manuales. */
    val liveRates: StateFlow<Map<String, LiveRate>> = settings.liveRates
    val manualRates: StateFlow<Map<String, Double>> = settings.manualUnitsPerUsd

    val rateRefresh: StateFlow<RateRefreshState> = rateUpdater.state

    init {
        // Intento silencioso al abrir: si la caché es reciente o no hay red, no molesta.
        viewModelScope.launch { rateUpdater.refresh(force = false) }
    }

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
        settings.ratesRevision,
    ) { base, secondary, _ ->
        CurrencyContext(
            base = base,
            secondary = secondary,
            rate = rates.rate(secondary, base) ?: 0.0,
            manual = settings.isManual(base) || settings.isManual(secondary),
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
        val base = snap.context.base
        val limit = snap.envelopes.filter { it.currency == base }.sumOf { it.limitMinor }
        val remaining = snap.summary.incomeMinor - snap.summary.spentMinor
        HomeUiState(
            baseCurrency = base,
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
            missingRateExpenses = snap.summary.missingRateExpenses,
            usdToVesRate = rates.rate(CurrencyRates.USD, CurrencyRates.VES),
            remainingEquivalent = equivalence(remaining, base),
            spentEquivalent = equivalence(snap.summary.spentMinor, base),
            envelopeEquivalents = snap.envelopes.associate { envelope ->
                envelope.categoryId to equivalence(envelope.remainingMinor, envelope.currency)
            },
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

    /** Equivalencia actual; cada valor es `null` si falta la tasa correspondiente. */
    private fun equivalence(minor: Long, from: String): AmountEquivalence =
        AmountEquivalence(
            usdMinor = rates.convert(minor, from, CurrencyRates.USD),
            vesMinor = rates.convert(minor, from, CurrencyRates.VES),
        )

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

    /** Crea una categoría propia; no se siembran categorías de ejemplo. */
    fun createCategory(name: String, emoji: String, kind: CategoryKind) {
        val cleanName = name.trim().take(28)
        if (cleanName.isBlank()) return
        val category = CategoryEntity(
            name = cleanName,
            emoji = emoji.ifBlank { "💸" },
            kind = kind,
            sortOrder = (state.value.categories.maxOfOrNull { it.sortOrder } ?: -1) + 1,
        )
        viewModelScope.launch { categoryRepository.add(category) }
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
            val now = System.currentTimeMillis()
            val base = settings.baseCurrency.value
            val isBase = txn.currency.equals(base, ignoreCase = true)
            val fx = if (isBase) null else rates.rate(txn.currency, base)
            val copy = txn.copy(
                id = java.util.UUID.randomUUID().toString(),
                occurredAt = now,
                createdAt = now,
                fxRateToBase = fx,
                fxBaseCurrency = base,
                fxSource = when {
                    isBase || fx == null -> FxSource.NONE
                    settings.isManual(txn.currency) || settings.isManual(base) -> FxSource.MANUAL
                    else -> FxSource.AUTO
                },
                fxTimestamp = if (isBase || fx == null) null else now,
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

    private suspend fun buildTxn(
        type: TxnType,
        amountMinor: Long,
        currency: String,
        categoryId: String?,
        merchant: String?,
        accountId: String? = null,
    ): TxnEntity {
        val base = settings.baseCurrency.value
        val isBase = currency.equals(base, ignoreCase = true)
        val signed = if (type == TxnType.EXPENSE) -amountMinor else amountMinor
        val account = accountId ?: accountRepository.ensure(currency).id
        val fx = if (isBase) null else rates.rate(currency, base)
        return TxnEntity(
            type = type,
            amountMinor = signed,
            currency = currency,
            accountId = account,
            categoryId = categoryId,
            merchant = merchant,
            occurredAt = System.currentTimeMillis(),
            fxRateToBase = fx,
            fxBaseCurrency = base,
            fxSource = when {
                isBase || fx == null -> FxSource.NONE
                settings.isManual(currency) || settings.isManual(base) -> FxSource.MANUAL
                else -> FxSource.AUTO
            },
            fxTimestamp = if (isBase || fx == null) null else System.currentTimeMillis(),
        )
    }

    // ------------------------------------------------------------------ divisas

    fun setBaseCurrency(code: String) = settings.setBaseCurrency(code)

    fun setSecondaryCurrency(code: String) = settings.setSecondaryCurrency(code)

    fun setManualRate(code: String, unitsPerUsd: Double) = settings.setManualRate(code, unitsPerUsd)

    fun clearManualRate(code: String) = settings.clearManualRate(code)

    fun setActiveVesReference(reference: VesReference) = settings.setActiveVesReference(reference)

    fun setVesManualRate(reference: VesReference, unitsPerUsd: Double) =
        settings.setVesManualRate(reference, unitsPerUsd)

    fun clearVesManualRate(reference: VesReference) = settings.clearVesManualRate(reference)

    /** Fuerza (o deja pasar) una actualización de las referencias de cambio. */
    fun refreshRates(force: Boolean = true) {
        viewModelScope.launch { rateUpdater.refresh(force) }
    }

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
            val account = accountRepository.ensure(base)
            paymentRepository.add(
                ScheduledPaymentEntity(
                    name = name.trim(),
                    amountMinor = amountMinor,
                    currency = base,
                    accountId = account.id,
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
                accountId = payment.accountId,
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
}
