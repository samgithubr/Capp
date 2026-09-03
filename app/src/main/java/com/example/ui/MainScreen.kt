package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CoffeeOrder
import com.example.recorder.RecordingState
import com.example.ui.components.AddEditOrderDialog
import com.example.ui.components.BillingCyclePdfView
import com.example.ui.components.MonthlyExpensesView
import com.example.ui.components.OrderCallRecorderView
import com.example.ui.components.SpreadsheetGridView
import com.example.ui.theme.AmberRoast
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.SheetGreen
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppNavTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    SPREADSHEET(
        "Google Sheet",
        Icons.Filled.GridOn,
        Icons.Outlined.GridOn,
        "tab_spreadsheet"
    ),
    CALL_RECORDER(
        "Call & Record",
        Icons.Filled.PhoneInTalk,
        Icons.Outlined.PhoneInTalk,
        "tab_call_recorder"
    ),
    EXPENSES(
        "Expenses",
        Icons.Filled.Calculate,
        Icons.Outlined.Calculate,
        "tab_expenses"
    ),
    PDF_BILLING(
        "PDF Summary",
        Icons.Filled.PictureAsPdf,
        Icons.Outlined.PictureAsPdf,
        "tab_pdf_billing"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CoffeeTrackerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var orderToEdit by remember { mutableStateOf<CoffeeOrder?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }

    // State collections
    val orders by viewModel.currentMonthOrders.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedBillingMonth.collectAsStateWithLifecycle()
    val billingCycleStartDay by viewModel.billingCycleStartDay.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val amplitude by viewModel.currentAmplitude.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val playingPath by viewModel.currentPlayingPath.collectAsStateWithLifecycle()
    val currentAudioPosMs by viewModel.audioPositionMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    val lastPdf by viewModel.lastGeneratedPdf.collectAsStateWithLifecycle()

    // Permission launcher for audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record coffee order calls", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestPermission(onGranted: () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(AmberRoast, RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Coffee Tracker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Local & Offline • Android 10+",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Recording Indicator in TopBar if active
                    if (recordingState != RecordingState.IDLE) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                AppNavTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab == AppNavTab.CALL_RECORDER && recordingState != RecordingState.IDLE) {
                                BadgedBox(badge = { Badge { Text("REC") } }) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(tab.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = if (tab == AppNavTab.SPREADSHEET) SheetGreen else EspressoBrown
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        orderToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = SheetGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_coffee_order")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Coffee Order")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> SpreadsheetGridView(
                    orders = orders,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    onOrderClick = { order ->
                        orderToEdit = order
                        showAddEditDialog = true
                    },
                    onAddOrderClick = {
                        orderToEdit = null
                        showAddEditDialog = true
                    },
                    onExportCsv = {
                        viewModel.exportGoogleSheetCsv(context)
                    },
                    onCopyTable = {
                        viewModel.copySheetTableToClipboard(context)
                    },
                    onPlayAudio = { audioPath ->
                        viewModel.playAudio(audioPath)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                1 -> OrderCallRecorderView(
                    recordingState = recordingState,
                    recordingDurationSeconds = recordingDuration,
                    currentAmplitude = amplitude,
                    orders = allOrders,
                    isPlaying = isPlaying,
                    playingFilePath = playingPath,
                    currentAudioPosMs = currentAudioPosMs,
                    audioDurationMs = audioDurationMs,
                    onStartCallAndRecord = { shop, phone ->
                        checkAndRequestPermission {
                            viewModel.dialAndRecordCoffeeShop(context, shop, phone)
                        }
                    },
                    onStartDirectRecord = { shop ->
                        checkAndRequestPermission {
                            viewModel.startRecording(shop)
                        }
                    },
                    onPauseRecord = { viewModel.pauseRecording() },
                    onResumeRecord = { viewModel.resumeRecording() },
                    onStopRecordAndSave = { shop, phone, notes ->
                        viewModel.stopRecordingAndCreateOrder(shop, phone, notes)
                        scope.launch {
                            snackbarHostState.showSnackbar("Call order recorded & added to Google Sheet!")
                        }
                    },
                    onSimulatedOrderTest = { transcript, shop, phone ->
                        viewModel.addSimulatedCoffeeOrder(transcript, shop, phone)
                        scope.launch {
                            snackbarHostState.showSnackbar("Spoken order detected & logged to spreadsheet!")
                        }
                    },
                    onPlayAudio = { path -> viewModel.playAudio(path) },
                    onPauseAudio = { viewModel.pauseAudio() },
                    onSeekAudio = { pos -> viewModel.seekAudio(pos) },
                    modifier = Modifier.fillMaxSize()
                )

                2 -> MonthlyExpensesView(
                    summary = monthlySummary,
                    selectedMonth = selectedMonth,
                    onNavigateMonth = { delta -> viewModel.navigateMonth(delta) },
                    modifier = Modifier.fillMaxSize()
                )

                3 -> BillingCyclePdfView(
                    summary = monthlySummary,
                    orders = orders,
                    lastPdfFile = lastPdf,
                    billingCycleStartDay = billingCycleStartDay,
                    monthlyBudget = monthlyBudget,
                    onDownloadPdf = {
                        val file = viewModel.generateAndDownloadPdf(context)
                        if (file != null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("PDF statement saved to device Downloads!")
                            }
                        }
                    },
                    onViewPdf = { viewModel.viewLastPdf(context) },
                    onSendSummary = { viewModel.sharePdfSummary(context) },
                    onUpdateCycleSettings = { day, budget ->
                        viewModel.setBillingCycleStartDay(day)
                        viewModel.setMonthlyBudget(budget)
                        scope.launch {
                            snackbarHostState.showSnackbar("Billing cycle settings updated.")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditOrderDialog(
                order = orderToEdit,
                currentBillingMonth = selectedMonth,
                onDismiss = { showAddEditDialog = false },
                onSave = { order ->
                    if (orderToEdit == null) {
                        viewModel.insertOrder(order)
                        scope.launch {
                            snackbarHostState.showSnackbar("Added order to spreadsheet.")
                        }
                    } else {
                        viewModel.updateOrder(order)
                        scope.launch {
                            snackbarHostState.showSnackbar("Updated order in spreadsheet.")
                        }
                    }
                    showAddEditDialog = false
                },
                onDelete = { order ->
                    viewModel.deleteOrder(order)
                    scope.launch {
                        snackbarHostState.showSnackbar("Deleted order from spreadsheet.")
                    }
                    showAddEditDialog = false
                }
            )
        }
    }
}
