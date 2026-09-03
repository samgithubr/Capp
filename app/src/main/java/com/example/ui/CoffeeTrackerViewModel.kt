package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CoffeeOrder
import com.example.data.CoffeeRepository
import com.example.expense.MonthlyExpenseCalculator
import com.example.expense.MonthlyExpenseSummary
import com.example.pdf.CoffeeExpensePdfGenerator
import com.example.recorder.AudioPlayerManager
import com.example.recorder.AudioRecorderManager
import com.example.recorder.OrderAudioParser
import com.example.recorder.RecordingResult
import com.example.recorder.RecordingState
import com.example.sheet.SpreadsheetExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CoffeeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CoffeeRepository
    val audioRecorderManager = AudioRecorderManager(application)
    val audioPlayerManager = AudioPlayerManager(application)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = CoffeeRepository(database.coffeeOrderDao())
    }

    // All orders from Room DB
    val allOrders: StateFlow<List<CoffeeOrder>> = repository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected billing cycle month (e.g. "2026-09")
    private val _selectedBillingMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    )
    val selectedBillingMonth: StateFlow<String> = _selectedBillingMonth.asStateFlow()

    // Configurable billing cycle start day (defaults to 1st of each month)
    private val _billingCycleStartDay = MutableStateFlow(1)
    val billingCycleStartDay: StateFlow<Int> = _billingCycleStartDay.asStateFlow()

    // Monthly coffee budget (defaults to $150.00)
    private val _monthlyBudget = MutableStateFlow(150.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    // Filter/Search query
    val searchQuery = MutableStateFlow("")

    // Filtered orders based on selected month & search query
    val currentMonthOrders: StateFlow<List<CoffeeOrder>> = combine(
        allOrders,
        _selectedBillingMonth,
        searchQuery
    ) { orders, month, query ->
        orders.filter { order ->
            val matchesMonth = order.billingCycleMonth == month
            val matchesQuery = query.isBlank() ||
                    order.shopName.contains(query, ignoreCase = true) ||
                    order.orderItemsSummary.contains(query, ignoreCase = true) ||
                    order.transcriptOrNotes.contains(query, ignoreCase = true)
            matchesMonth && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly expense calculations summary
    val monthlySummary: StateFlow<MonthlyExpenseSummary> = combine(
        currentMonthOrders,
        _selectedBillingMonth,
        _billingCycleStartDay,
        _monthlyBudget
    ) { orders, month, startDay, budget ->
        MonthlyExpenseCalculator.calculateSummary(orders, month, startDay, budget)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyExpenseCalculator.calculateSummary(emptyList(), "2026-09")
    )

    // Recorder State
    val recordingState: StateFlow<RecordingState> = audioRecorderManager.recordingState
    val recordingDurationSeconds: StateFlow<Int> = audioRecorderManager.recordingDurationSeconds
    val currentAmplitude: StateFlow<Float> = audioRecorderManager.currentAmplitude

    // Player State
    val isAudioPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val currentPlayingPath: StateFlow<String?> = audioPlayerManager.currentPlayingPath
    val audioPositionMs: StateFlow<Int> = audioPlayerManager.currentPositionMs
    val audioDurationMs: StateFlow<Int> = audioPlayerManager.durationMs

    // Exported files references
    val lastGeneratedPdf = MutableStateFlow<File?>(null)
    val lastExportedCsv = MutableStateFlow<File?>(null)

    // Active order recording shop draft
    val activeShopName = MutableStateFlow("Blue Bottle Coffee")
    val activePhoneNumber = MutableStateFlow("+1 (555) 432-8765")

    fun setSelectedMonth(month: String) {
        _selectedBillingMonth.value = month
    }

    fun setBillingCycleStartDay(day: Int) {
        _billingCycleStartDay.value = day.coerceIn(1, 28)
    }

    fun setMonthlyBudget(budget: Double) {
        _monthlyBudget.value = budget.coerceAtLeast(10.0)
    }

    fun navigateMonth(delta: Int) {
        val current = _selectedBillingMonth.value
        val parts = current.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 9) - 1

        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        cal.add(Calendar.MONTH, delta)

        val newMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        _selectedBillingMonth.value = newMonth
    }

    fun insertOrder(order: CoffeeOrder) {
        viewModelScope.launch {
            repository.insertOrder(order)
        }
    }

    fun updateOrder(order: CoffeeOrder) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun deleteOrder(order: CoffeeOrder) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    // Call and recording actions
    fun startRecording(shopName: String) {
        audioRecorderManager.startRecording(shopName)
    }

    fun pauseRecording() {
        audioRecorderManager.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorderManager.resumeRecording()
    }

    fun stopRecordingAndCreateOrder(
        shopName: String,
        phoneNumber: String,
        spokenOrderText: String? = null
    ) {
        val result: RecordingResult? = audioRecorderManager.stopRecording()
        val orderText = spokenOrderText ?: OrderAudioParser.sampleVoiceTranscripts.first()
        val parsed = OrderAudioParser.parseOrderText(orderText)

        val order = CoffeeOrder(
            shopName = shopName.ifBlank { "Coffee Shop" },
            phoneNumber = phoneNumber,
            orderItemsSummary = parsed.summaryText,
            itemCount = parsed.items.sumOf { it.quantity },
            unitPriceEstimate = if (parsed.items.isNotEmpty()) parsed.items.first().unitPrice else 5.0,
            subtotal = parsed.subtotal,
            tax = parsed.tax,
            tip = parsed.suggestedTip,
            totalAmount = parsed.total,
            orderTimestamp = System.currentTimeMillis(),
            billingCycleMonth = _selectedBillingMonth.value,
            audioFilePath = result?.filePath,
            audioDurationSeconds = result?.durationSeconds ?: 30,
            transcriptOrNotes = orderText,
            orderStatus = "Confirmed",
            isPaid = true
        )

        insertOrder(order)
    }

    fun dialAndRecordCoffeeShop(context: Context, shopName: String, phone: String) {
        startRecording(shopName)
        if (phone.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${phone.replace(" ", "")}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addSimulatedCoffeeOrder(transcript: String, shopName: String, phone: String = "") {
        val parsed = OrderAudioParser.parseOrderText(transcript)
        val order = CoffeeOrder(
            shopName = shopName,
            phoneNumber = phone,
            orderItemsSummary = parsed.summaryText,
            itemCount = parsed.items.sumOf { it.quantity },
            unitPriceEstimate = if (parsed.items.isNotEmpty()) parsed.items.first().unitPrice else 5.0,
            subtotal = parsed.subtotal,
            tax = parsed.tax,
            tip = parsed.suggestedTip,
            totalAmount = parsed.total,
            orderTimestamp = System.currentTimeMillis(),
            billingCycleMonth = _selectedBillingMonth.value,
            audioFilePath = null,
            audioDurationSeconds = 42,
            transcriptOrNotes = transcript,
            orderStatus = "Confirmed",
            isPaid = true
        )
        insertOrder(order)
    }

    // Spreadsheet Exports
    fun exportGoogleSheetCsv(context: Context) {
        val orders = currentMonthOrders.value
        val file = SpreadsheetExporter.exportToCsvFile(context, orders, _selectedBillingMonth.value)
        if (file != null) {
            lastExportedCsv.value = file
            SpreadsheetExporter.openInGoogleSheets(context, file)
        } else {
            Toast.makeText(context, "Failed to export spreadsheet CSV", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareGoogleSheetCsv(context: Context) {
        val orders = currentMonthOrders.value
        val file = SpreadsheetExporter.exportToCsvFile(context, orders, _selectedBillingMonth.value)
        if (file != null) {
            lastExportedCsv.value = file
            SpreadsheetExporter.shareCsvFile(context, file)
        } else {
            Toast.makeText(context, "Failed to share spreadsheet CSV", Toast.LENGTH_SHORT).show()
        }
    }

    fun copySheetTableToClipboard(context: Context) {
        SpreadsheetExporter.copySheetToClipboard(context, currentMonthOrders.value)
    }

    // PDF Billing Statements
    fun generateAndDownloadPdf(context: Context): File? {
        val orders = currentMonthOrders.value
        val pdfFile = CoffeeExpensePdfGenerator.generateBillingPdf(
            context = context,
            orders = orders,
            billingMonth = _selectedBillingMonth.value,
            billingCycleStartDay = _billingCycleStartDay.value,
            monthlyBudget = _monthlyBudget.value
        )
        if (pdfFile != null) {
            lastGeneratedPdf.value = pdfFile
            Toast.makeText(context, "PDF Statement saved to Downloads: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to generate PDF statement", Toast.LENGTH_SHORT).show()
        }
        return pdfFile
    }

    fun viewLastPdf(context: Context) {
        val file = lastGeneratedPdf.value ?: generateAndDownloadPdf(context)
        if (file != null) {
            CoffeeExpensePdfGenerator.viewPdf(context, file)
        }
    }

    fun sharePdfSummary(context: Context) {
        val file = lastGeneratedPdf.value ?: generateAndDownloadPdf(context)
        if (file != null) {
            CoffeeExpensePdfGenerator.sharePdf(context, file, monthlySummary.value)
        }
    }

    // Audio Playback
    fun playAudio(filePath: String) {
        audioPlayerManager.playAudio(filePath)
    }

    fun pauseAudio() {
        audioPlayerManager.pause()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayerManager.seekTo(positionMs)
    }

    override fun onCleared() {
        audioRecorderManager.stopRecording()
        audioPlayerManager.stop()
        super.onCleared()
    }
}
