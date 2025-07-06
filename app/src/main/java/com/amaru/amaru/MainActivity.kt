package com.amaru.amaru

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Main activity for network monitoring application
 * Provides UI for starting/stopping monitoring and viewing collected data
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+), we don't need external storage permissions
            // We'll use internal storage instead
            arrayOf<String>()
        } else {
            // For older Android versions, we need storage permissions
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        // Server configuration
        private const val SERVER_URL = "https://myServer.status.com"
        private const val PREFS_NAME = "amaru_preferences"
        private const val DEVICE_ID_KEY = "unique_device_id"
    }
    
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var flowAnalyzer: FlowAnalyzer
    private lateinit var statusText: TextView
    private lateinit var networkInfoText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var exportButton: Button
    private lateinit var statsContainer: LinearLayout
    private lateinit var flowsRecyclerView: RecyclerView
    private lateinit var flowsAdapter: FlowsAdapter
    
    // PyTorch Model UI elements
    private lateinit var modelStatusText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var riskLevelText: TextView
    private lateinit var memoryText: TextView
    private lateinit var modelStatsText: TextView
    private lateinit var serverStatusText: TextView
    
    private var isMonitoring = false
    private val handler = Handler(Looper.getMainLooper())
    private val flows = mutableListOf<NetworkFlowStats>()
    
    // PyTorch analysis batching (analyze every 30 records)
    private val flowBatch = mutableListOf<NetworkFlowStats>()
    private var batchCount = 0
    private val BATCH_SIZE = 30
    
    // HTTP client for server communication
    private lateinit var httpClient: OkHttpClient
    private lateinit var gson: Gson
    private lateinit var deviceId: String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUI()
        
        // Initialize network monitor
        networkMonitor = NetworkMonitor(this)
        
        // Initialize flow analyzer with PyTorch model
        try {
            flowAnalyzer = FlowAnalyzer(this)
            Log.d(TAG, "✅ FlowAnalyzer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing FlowAnalyzer", e)
            Toast.makeText(this, "Warning: AI model initialization failed", Toast.LENGTH_LONG).show()
        }
        
        // Initialize HTTP client and device ID
        initializeHttpClient()
        initializeDeviceId()
        
        // Check permissions
        checkPermissions()
        
        // Setup UI
        setupUI()
        
        // Initialize PyTorch model results UI with safe defaults
        initializeModelResultsUI()
        
        // Start updating network info
        startNetworkInfoUpdates()
        
        // Check model initialization status after a delay
        handler.postDelayed({
            checkModelInitializationStatus()
        }, 3000) // Check after 3 seconds
    }
    
    /**
     * Create the UI programmatically with modern design
     */
    private fun createUI() {
        // Create main layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#f5f5f5"))
        }
        
        // Title
        val titleText = TextView(this).apply {
            text = "Amaru Network Monitor"
            textSize = 24f
            setTextColor(Color.parseColor("#2c3e50"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        mainLayout.addView(titleText)
        
        // Status card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }
        
        statusText = TextView(this).apply {
            text = "Status: Stopped"
            textSize = 16f
            setTextColor(Color.parseColor("#e74c3c"))
            setPadding(0, 0, 0, 8)
        }
        statusCard.addView(statusText)
        
        networkInfoText = TextView(this).apply {
            text = "Network: Unknown"
            textSize = 14f
            setTextColor(Color.parseColor("#7f8c8d"))
        }
        statusCard.addView(networkInfoText)
        
        mainLayout.addView(statusCard)
        
        // Add spacing
        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }
        mainLayout.addView(spacer1)
        
        // Control buttons
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        
        startButton = Button(this).apply {
            text = "Start Monitoring"
            setBackgroundColor(Color.parseColor("#27ae60"))
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, 8, 0)
            }
        }
        buttonLayout.addView(startButton)
        
        stopButton = Button(this).apply {
            text = "Stop Monitoring"
            setBackgroundColor(Color.parseColor("#e74c3c"))
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            isEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(8, 0, 0, 0)
            }
        }
        buttonLayout.addView(stopButton)
        
        mainLayout.addView(buttonLayout)
        
        // Export button
        exportButton = Button(this).apply {
            text = "Export Data"
            setBackgroundColor(Color.parseColor("#3498db"))
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
        }
        mainLayout.addView(exportButton)
        
        // Add spacing
        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }
        mainLayout.addView(spacer2)
        
        // PyTorch Model Results container
        val modelResultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }
        
        val modelTitle = TextView(this).apply {
            text = "🧠 PyTorch AI Model Results"
            textSize = 18f
            setTextColor(Color.parseColor("#2c3e50"))
            setPadding(0, 0, 0, 16)
        }
        modelResultsContainer.addView(modelTitle)
        
        // Model status and confidence
        modelStatusText = TextView(this).apply {
            text = "Model Status: Initializing..."
            textSize = 14f
            setTextColor(Color.parseColor("#7f8c8d"))
            setPadding(0, 0, 0, 8)
        }
        modelResultsContainer.addView(modelStatusText)
        
        confidenceText = TextView(this).apply {
            text = "Confidence: N/A"
            textSize = 14f
            setTextColor(Color.parseColor("#7f8c8d"))
            setPadding(0, 0, 0, 8)
        }
        modelResultsContainer.addView(confidenceText)
        
        riskLevelText = TextView(this).apply {
            text = "Risk Level: SAFE"
            textSize = 16f
            setTextColor(Color.parseColor("#27ae60"))
            setPadding(0, 0, 0, 8)
        }
        modelResultsContainer.addView(riskLevelText)
        
        memoryText = TextView(this).apply {
            text = "Memory Usage: 0% (Learning...)"
            textSize = 14f
            setTextColor(Color.parseColor("#7f8c8d"))
            setPadding(0, 0, 0, 8)
        }
        modelResultsContainer.addView(memoryText)
        
        modelStatsText = TextView(this).apply {
            text = "Predictions: 0 | Threats: 0"
            textSize = 14f
            setTextColor(Color.parseColor("#7f8c8d"))
        }
        modelResultsContainer.addView(modelStatsText)
        
        // Add server communication status
        serverStatusText = TextView(this).apply {
            text = "📡 Server Status: Initializing..."
            textSize = 14f
            setTextColor(Color.parseColor("#3498db"))
            setPadding(0, 8, 0, 0)
        }
        modelResultsContainer.addView(serverStatusText)
        
        mainLayout.addView(modelResultsContainer)
        
        // Add spacing
        val spacer3 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }
        mainLayout.addView(spacer3)
        
        // Statistics container
        statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }
        
        val statsTitle = TextView(this).apply {
            text = "📊 Live Network Flow Statistics"
            textSize = 18f
            setTextColor(Color.parseColor("#2c3e50"))
            setPadding(0, 0, 0, 16)
        }
        statsContainer.addView(statsTitle)
        
        // RecyclerView for flows
        flowsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }
        statsContainer.addView(flowsRecyclerView)
        
        mainLayout.addView(statsContainer)
        
        setContentView(mainLayout)
    }
    
    /**
     * Setup UI event listeners
     */
    private fun setupUI() {
        flowsAdapter = FlowsAdapter(flows)
        flowsRecyclerView.adapter = flowsAdapter
        
        startButton.setOnClickListener {
            startMonitoring()
        }
        
        stopButton.setOnClickListener {
            stopMonitoring()
        }
        
        exportButton.setOnClickListener {
            exportData()
        }
        
        // Add long press on export button for debug info
        exportButton.setOnLongClickListener {
            showDebugInfo()
            true
        }
    }
    
    /**
     * Start network monitoring
     */
    private fun startMonitoring() {
        try {
            Log.d(TAG, "Starting network monitoring...")
            
            if (!hasRequiredPermissions()) {
                requestPermissions()
                return
            }
            
            isMonitoring = true
            
            // Update UI
            statusText.text = "Status: Initializing..."
            statusText.setTextColor(Color.parseColor("#f39c12"))
            startButton.isEnabled = false
            stopButton.isEnabled = true
        
        try {
            val serviceIntent = Intent(this, NetworkMonitoringService::class.java).apply {
                action = NetworkMonitoringService.ACTION_START_MONITORING
            }
            
            // Use startForegroundService for Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Log.d(TAG, "Service start initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
            Toast.makeText(this, "Error starting monitoring service", Toast.LENGTH_SHORT).show()
        }
        
        // Start local monitoring for UI updates
        try {
            networkMonitor.startMonitoring { flowStats ->
                runOnUiThread {
                    try {
                        // Add flow to batch for PyTorch analysis
                        flowBatch.add(flowStats)
                        batchCount++
                        
                        // Update flow list for UI
                        flows.add(0, flowStats)
                        if (flows.size > 10) {
                            flows.removeAt(flows.size - 1)
                        }
                        flowsAdapter.notifyDataSetChanged()
                        
                        // Analyze with PyTorch only when we have 30 records
                        val analysisResult = if (batchCount >= BATCH_SIZE) {
                            try {
                                if (::flowAnalyzer.isInitialized) {
                                    // Analyze the latest flow from the batch (the model maintains internal memory)
                                    val result = flowAnalyzer.analyzeFlow(flowStats)
                                    
                                    // Send batch to server with analysis results
                                    sendBatchToServer(flowBatch.toList(), result)
                                    
                                    // Clear batch after analysis and sending
                                    flowBatch.clear()
                                    batchCount = 0
                                    
                                    Log.d(TAG, "🔬 PyTorch analysis completed for batch of $BATCH_SIZE records")
                                    result
                                } else {
                                    // Model not initialized, but still send batch to server
                                    val fallbackResult = createFallbackResult("Not initialized", "⚠️ AI model not available - using basic monitoring only")
                                    sendBatchToServer(flowBatch.toList(), fallbackResult)
                                    
                                    // Clear batch after sending
                                    flowBatch.clear()
                                    batchCount = 0
                                    
                                    fallbackResult
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error in PyTorch batch analysis", e)
                                
                                // Still try to send batch to server even if analysis fails
                                try {
                                    val errorResult = createFallbackResult("Error: ${e.message}", "❌ AI analysis failed - check logs")
                                    sendBatchToServer(flowBatch.toList(), errorResult)
                                } catch (sendError: Exception) {
                                    Log.e(TAG, "❌ Error sending batch after analysis failure", sendError)
                                }
                                
                                // Clear batch after sending
                                flowBatch.clear()
                                batchCount = 0
                                
                                createFallbackResult("Error: ${e.message}", "❌ AI analysis failed - check logs")
                            }
                        } else {
                            // Batch not complete yet, show progress
                            createProgressResult()
                        }
                        
                        // Update PyTorch model results UI
                        updateModelResultsUI(analysisResult)
                        
                        Log.d(TAG, "✅ UI updated - Batch progress: $batchCount/$BATCH_SIZE")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Critical error updating UI", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitor", e)
            Toast.makeText(this, "Error starting network monitoring", Toast.LENGTH_SHORT).show()
            
            // Reset UI state on error
            isMonitoring = false
            statusText.text = "Status: Error"
            statusText.setTextColor(Color.parseColor("#e74c3c"))
            startButton.isEnabled = true
            stopButton.isEnabled = false
            return
        }
        
        // Update UI to show monitoring is active
        statusText.text = "Status: Monitoring"
        statusText.setTextColor(Color.parseColor("#27ae60"))
        
        Toast.makeText(this, "Network monitoring started", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Monitoring started successfully")
        
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in startMonitoring", e)
            Toast.makeText(this, "Critical error starting monitoring", Toast.LENGTH_LONG).show()
            
            // Reset state
            isMonitoring = false
            statusText.text = "Status: Error"
            statusText.setTextColor(Color.parseColor("#e74c3c"))
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }
    
    /**
     * Stop network monitoring
     */
    private fun stopMonitoring() {
        isMonitoring = false
        
        // Update UI
        statusText.text = "Status: Stopped"
        statusText.setTextColor(Color.parseColor("#e74c3c"))
        startButton.isEnabled = true
        stopButton.isEnabled = false
        
        // Stop monitoring service
        val serviceIntent = Intent(this, NetworkMonitoringService::class.java).apply {
            action = NetworkMonitoringService.ACTION_STOP_MONITORING
        }
        startService(serviceIntent)
        
        // Stop local monitoring
        networkMonitor.stopMonitoring()
        
        // Reset PyTorch model UI and batch
        resetModelResultsUI()
        
        // Clear batch
        flowBatch.clear()
        batchCount = 0
        
        Toast.makeText(this, "Network monitoring stopped", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Monitoring stopped")
    }
    
    /**
     * Update PyTorch model results UI with latest analysis
     */
    private fun updateModelResultsUI(analysisResult: MalwareAnalysisResult) {
        try {
            // Update model status with better handling
            val statusText = when {
                analysisResult.modelStatus.contains("Not initialized") -> "Model Status: ⚠️ Not Initialized"
                analysisResult.modelStatus.contains("Error") -> "Model Status: ❌ Error"
                analysisResult.modelStatus == "Active" -> "Model Status: ✅ Active"
                analysisResult.modelStatus.contains("Collecting batch") -> "Model Status: 📊 ${analysisResult.modelStatus}"
                else -> "Model Status: ${analysisResult.modelStatus}"
            }
            
            modelStatusText.text = statusText
            modelStatusText.setTextColor(
                when {
                    analysisResult.modelStatus == "Active" -> Color.parseColor("#27ae60")
                    analysisResult.modelStatus.contains("Collecting batch") -> Color.parseColor("#3498db")
                    analysisResult.modelStatus.contains("Not initialized") -> Color.parseColor("#f39c12")
                    analysisResult.modelStatus.contains("Error") -> Color.parseColor("#e74c3c")
                    else -> Color.parseColor("#7f8c8d")
                }
            )
            
            // Update confidence
            val confidencePercent = (analysisResult.mlConfidence * 100).toInt()
            confidenceText.text = "Confidence: $confidencePercent%"
            confidenceText.setTextColor(
                when {
                    confidencePercent >= 80 -> Color.parseColor("#e74c3c")
                    confidencePercent >= 60 -> Color.parseColor("#f39c12")
                    confidencePercent >= 40 -> Color.parseColor("#f1c40f")
                    else -> Color.parseColor("#27ae60")
                }
            )
            
            // Update risk level
            val riskEmoji = when (analysisResult.riskLevel) {
                RiskLevel.CRITICAL -> "🔴"
                RiskLevel.HIGH -> "🟠"
                RiskLevel.MEDIUM -> "🟡"
                RiskLevel.LOW -> "🟢"
                RiskLevel.SAFE -> "✅"
            }
            
            riskLevelText.text = "Risk Level: $riskEmoji ${analysisResult.riskLevel}"
            riskLevelText.setTextColor(
                when (analysisResult.riskLevel) {
                    RiskLevel.CRITICAL -> Color.parseColor("#e74c3c")
                    RiskLevel.HIGH -> Color.parseColor("#e67e22")
                    RiskLevel.MEDIUM -> Color.parseColor("#f39c12")
                    RiskLevel.LOW -> Color.parseColor("#f1c40f")
                    RiskLevel.SAFE -> Color.parseColor("#27ae60")
                }
            )
            
            // Update memory usage
            val memoryPercent = (analysisResult.memoryUtilization * 100).toInt()
            memoryText.text = "Memory Usage: $memoryPercent% ${if (memoryPercent < 50) "(Learning...)" else "(Experienced)"}"
            memoryText.setTextColor(
                if (memoryPercent < 50) Color.parseColor("#3498db") 
                else Color.parseColor("#27ae60")
            )
            
            // Update general statistics (we'll need to track these)
            updateModelStatistics()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating model results UI", e)
        }
    }
    
    /**
     * Update model statistics display
     */
    private fun updateModelStatistics() {
        try {
            // This would need to be implemented with a way to track total predictions and threats
            // For now, we'll use placeholder values
            val totalPredictions = flows.size
            val threatFlows = flows.count { 
                // Simple heuristic for demonstration
                it.flowPacketsPerSecond > 10 || it.flowBytesPerSecond > 1000
            }
            
            modelStatsText.text = "Predictions: $totalPredictions | Threats: $threatFlows"
            modelStatsText.setTextColor(
                if (threatFlows > 0) Color.parseColor("#e74c3c") 
                else Color.parseColor("#27ae60")
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating model statistics", e)
        }
    }
    
    /**
     * Reset PyTorch model results UI to initial state
     */
    private fun resetModelResultsUI() {
        try {
            modelStatusText.text = "Model Status: Stopped"
            modelStatusText.setTextColor(Color.parseColor("#7f8c8d"))
            
            confidenceText.text = "Confidence: N/A"
            confidenceText.setTextColor(Color.parseColor("#7f8c8d"))
            
            riskLevelText.text = "Risk Level: ✅ SAFE"
            riskLevelText.setTextColor(Color.parseColor("#27ae60"))
            
            memoryText.text = "Memory Usage: 0% (Inactive)"
            memoryText.setTextColor(Color.parseColor("#7f8c8d"))
            
            modelStatsText.text = "Predictions: 0 | Threats: 0"
            modelStatsText.setTextColor(Color.parseColor("#7f8c8d"))
            
            serverStatusText.text = "📡 Server Status: Ready - Device ID: ${deviceId.takeLast(8)}"
            serverStatusText.setTextColor(Color.parseColor("#27ae60"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting model results UI", e)
        }
    }
    
    /**
     * Initialize PyTorch model results UI with safe defaults
     */
    private fun initializeModelResultsUI() {
        try {
            modelStatusText.text = "Model Status: ⚙️ Initializing..."
            modelStatusText.setTextColor(Color.parseColor("#3498db"))
            
            confidenceText.text = "Confidence: N/A"
            confidenceText.setTextColor(Color.parseColor("#7f8c8d"))
            
            riskLevelText.text = "Risk Level: ✅ SAFE"
            riskLevelText.setTextColor(Color.parseColor("#27ae60"))
            
            memoryText.text = "Memory Usage: 0% (Preparing...)"
            memoryText.setTextColor(Color.parseColor("#3498db"))
            
            modelStatsText.text = "Predictions: 0 | Threats: 0"
            modelStatsText.setTextColor(Color.parseColor("#7f8c8d"))
            
            Log.d(TAG, "✅ Model results UI initialized with safe defaults")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing model results UI", e)
        }
    }
    
    /**
     * Check if PyTorch model initialization was successful
     */
    private fun checkModelInitializationStatus() {
        try {
            if (::flowAnalyzer.isInitialized) {
                // Try a dummy analysis to check if model is working
                val dummyFlow = NetworkFlowStats().apply {
                    addForwardPacket(100, System.currentTimeMillis())
                    addBackwardPacket(50, System.currentTimeMillis() + 100)
                    label = "Test"
                }
                
                val testResult = try {
                    flowAnalyzer.analyzeFlow(dummyFlow)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Model test failed", e)
                    null
                }
                
                if (testResult != null) {
                    // Model is working
                    modelStatusText.text = "Model Status: ✅ Ready (waiting for data...)"
                    modelStatusText.setTextColor(Color.parseColor("#27ae60"))
                    memoryText.text = "Memory Usage: 0% (Ready to learn)"
                    Log.d(TAG, "✅ PyTorch model is ready and working")
                } else {
                    // Model failed test
                    modelStatusText.text = "Model Status: ⚠️ Failed to load"
                    modelStatusText.setTextColor(Color.parseColor("#e74c3c"))
                    memoryText.text = "Memory Usage: N/A (Model error)"
                    Log.w(TAG, "⚠️ PyTorch model failed initialization test")
                }
            } else {
                // FlowAnalyzer not initialized
                modelStatusText.text = "Model Status: ❌ Failed to initialize"
                modelStatusText.setTextColor(Color.parseColor("#e74c3c"))
                memoryText.text = "Memory Usage: N/A (Not initialized)"
                Log.w(TAG, "⚠️ FlowAnalyzer was not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking model initialization status", e)
            modelStatusText.text = "Model Status: ❌ Check failed"
            modelStatusText.setTextColor(Color.parseColor("#e74c3c"))
        }
    }
    
    /**
     * Create fallback analysis result for errors
     */
    private fun createFallbackResult(status: String, recommendation: String): MalwareAnalysisResult {
        return MalwareAnalysisResult().apply {
            modelStatus = status
            mlConfidence = 0.0
            mlRiskLevel = RiskLevel.SAFE
            memoryUtilization = 0.0
            riskLevel = RiskLevel.SAFE
            recommendations = listOf(recommendation)
        }
    }
    
    /**
     * Create progress result while collecting batch
     */
    private fun createProgressResult(): MalwareAnalysisResult {
        return MalwareAnalysisResult().apply {
            modelStatus = "Collecting batch ($batchCount/$BATCH_SIZE)"
            mlConfidence = 0.0
            mlRiskLevel = RiskLevel.SAFE
            memoryUtilization = 0.0
            riskLevel = RiskLevel.SAFE
            recommendations = listOf("📊 Collecting $BATCH_SIZE samples for AI analysis... ($batchCount/$BATCH_SIZE)")
        }
    }
    
    /**
     * Export collected data
     */
    private fun exportData() {
        try {
            Log.d(TAG, "Export button clicked")
            
            // Check if monitoring is active
            if (!isMonitoring) {
                Toast.makeText(this, "Start monitoring first to collect data", Toast.LENGTH_LONG).show()
                return
            }
            
            // Show current flow count
            Toast.makeText(this, "Exporting ${flows.size} flows from UI + service data...", Toast.LENGTH_SHORT).show()
            
            val serviceIntent = Intent(this, NetworkMonitoringService::class.java).apply {
                action = NetworkMonitoringService.ACTION_EXPORT_DATA
            }
            
            // Use startForegroundService for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Log.d(TAG, "Export service intent sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating export", e)
            Toast.makeText(this, "Error exporting data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Start updating network info periodically
     */
    private fun startNetworkInfoUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                networkInfoText.text = "Network: ${networkMonitor.getNetworkInfo()}"
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        }
        handler.post(updateRunnable)
    }
    
    /**
     * Show debug information about current data state
     */
    private fun showDebugInfo() {
        try {
            val currentFlows = networkMonitor.getCurrentFlows()
            val networkAvailable = networkMonitor.isNetworkAvailable()
            
            val debugInfo = StringBuilder().apply {
                append("🔍 DEBUG INFO:\n\n")
                append("📊 UI Flows: ${flows.size}\n")
                append("🌐 Network Monitor Flows: ${currentFlows.size}\n")
                append("📡 Network Available: $networkAvailable\n")
                append("⚡ Monitoring Active: $isMonitoring\n\n")
                
                if (currentFlows.isNotEmpty()) {
                    append("📋 Recent Flows:\n")
                    currentFlows.entries.take(3).forEach { (key, flow) ->
                        append("• $key: ${flow.totalFwdPackets}/${flow.totalBwdPackets} packets\n")
                    }
                } else {
                    append("❌ No flows detected\n")
                    append("💡 Try using apps that access internet\n")
                }
                
                append("\n📝 Check Logcat for detailed logs")
                append("\n🔄 Long press Export for this info")
            }
            
            // Create and show dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Debug Information")
                .setMessage(debugInfo.toString())
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setNeutralButton("Copy to Clipboard") { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo.toString())
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                .show()
                
            Log.d(TAG, "Debug info shown to user")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing debug info", e)
            Toast.makeText(this, "Error getting debug info", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    private fun hasRequiredPermissions(): Boolean {
        // If no permissions are required (Android 10+), return true
        if (REQUIRED_PERMISSIONS.isEmpty()) {
            return true
        }
        
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request required permissions
     */
    private fun requestPermissions() {
        // If no permissions are required (Android 10+), don't request anything
        if (REQUIRED_PERMISSIONS.isEmpty()) {
            Toast.makeText(this, "No permissions required for this Android version", Toast.LENGTH_SHORT).show()
            return
        }
        
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }
    
    /**
     * Initialize HTTP client for server communication
     */
    private fun initializeHttpClient() {
        try {
            httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
                
            gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()
                
            Log.d(TAG, "✅ HTTP client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing HTTP client", e)
        }
    }
    
    /**
     * Initialize or retrieve unique device ID
     */
    private fun initializeDeviceId() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            deviceId = prefs.getString(DEVICE_ID_KEY, null) ?: run {
                // Generate new unique device ID
                val newId = "AMARU_${System.currentTimeMillis()}_${(1000..9999).random()}"
                prefs.edit().putString(DEVICE_ID_KEY, newId).apply()
                Log.d(TAG, "📱 Generated new device ID: $newId")
                newId
            }
            
            Log.d(TAG, "✅ Device ID initialized: $deviceId")
            
            // Update server status UI
            runOnUiThread {
                serverStatusText.text = "📡 Server Status: Ready - Device ID: ${deviceId.takeLast(8)}"
                serverStatusText.setTextColor(Color.parseColor("#27ae60"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing device ID", e)
            deviceId = "AMARU_FALLBACK_${System.currentTimeMillis()}"
            
            // Update server status UI
            runOnUiThread {
                serverStatusText.text = "📡 Server Status: Error initializing device ID"
                serverStatusText.setTextColor(Color.parseColor("#e74c3c"))
            }
        }
    }
    
    /**
     * Send batch data to server
     */
    private fun sendBatchToServer(batchData: List<NetworkFlowStats>, analysisResult: MalwareAnalysisResult? = null) {
        thread {
            try {
                Log.d(TAG, "📤 Preparing to send batch of ${batchData.size} records + PyTorch analysis to server")
                
                // Update UI to show sending
                runOnUiThread {
                    serverStatusText.text = "📡 Server Status: Sending batch (${batchData.size} records + AI analysis)..."
                    serverStatusText.setTextColor(Color.parseColor("#f39c12"))
                }
                
                // Create payload
                val payload = BatchPayload(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    batchSize = batchData.size,
                    flows = batchData.map { flow -> flowToJsonObject(flow) },
                    pytorchAnalysis = analysisResult?.let { analysisResultToJsonObject(it) }
                )
                
                val jsonPayload = gson.toJson(payload)
                Log.d(TAG, "📋 Payload created: ${jsonPayload.length} characters (includes PyTorch analysis: ${analysisResult != null})")
                
                // Create request
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Amaru-Android/$deviceId")
                    .build()
                
                // Send request
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "✅ Batch sent successfully: ${response.code} - $responseBody")
                        
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "✅ Batch + AI analysis sent successfully", Toast.LENGTH_SHORT).show()
                            serverStatusText.text = "📡 Server Status: ✅ Last batch + AI analysis sent successfully"
                            serverStatusText.setTextColor(Color.parseColor("#27ae60"))
                        }
                    } else {
                        Log.w(TAG, "⚠️ Server responded with error: ${response.code} - ${response.message}")
                        
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "⚠️ Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                            serverStatusText.text = "📡 Server Status: ⚠️ Error ${response.code} - ${response.message}"
                            serverStatusText.setTextColor(Color.parseColor("#e74c3c"))
                        }
                    }
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "❌ Network error sending batch", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    serverStatusText.text = "📡 Server Status: ❌ Network error: ${e.message}"
                    serverStatusText.setTextColor(Color.parseColor("#e74c3c"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error sending batch", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Error sending batch: ${e.message}", Toast.LENGTH_SHORT).show()
                    serverStatusText.text = "📡 Server Status: ❌ Error: ${e.message}"
                    serverStatusText.setTextColor(Color.parseColor("#e74c3c"))
                }
            }
        }
    }
    
    /**
     * Convert NetworkFlowStats to JSON object
     */
    private fun flowToJsonObject(flow: NetworkFlowStats): Map<String, Any> {
        return mapOf(
            "flowStartTime" to flow.flowStartTime,
            "flowEndTime" to flow.flowEndTime,
            "totalFwdPackets" to flow.totalFwdPackets,
            "totalBwdPackets" to flow.totalBwdPackets,
            "totalLengthFwdPackets" to flow.totalLengthFwdPackets,
            "totalLengthBwdPackets" to flow.totalLengthBwdPackets,
            "flowBytesPerSecond" to flow.flowBytesPerSecond,
            "flowPacketsPerSecond" to flow.flowPacketsPerSecond,
            "flowDuration" to flow.flowDuration,
            "label" to flow.label,
            "flowId" to flow.flowId
        )
    }
    
    /**
     * Convert MalwareAnalysisResult to JSON object
     */
    private fun analysisResultToJsonObject(analysisResult: MalwareAnalysisResult): Map<String, Any> {
        return mapOf(
            "modelStatus" to analysisResult.modelStatus,
            "mlConfidence" to analysisResult.mlConfidence,
            "mlRiskLevel" to analysisResult.mlRiskLevel.toString(),
            "memoryUtilization" to analysisResult.memoryUtilization,
            "riskLevel" to analysisResult.riskLevel.toString(),
            "recommendations" to analysisResult.recommendations,
            "analysisTimestamp" to System.currentTimeMillis()
        )
    }
    
    /**
     * Check and request permissions
     */
    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
        } else {
            // All permissions are already granted or not required
            val message = if (REQUIRED_PERMISSIONS.isEmpty()) {
                "Ready to monitor! No permissions required for Android ${Build.VERSION.SDK_INT}"
            } else {
                "All permissions already granted"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // No permissions were requested
                Toast.makeText(this, "No permissions required", Toast.LENGTH_SHORT).show()
            } else if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied. App may not work properly.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isMonitoring) {
            stopMonitoring()
        }
        handler.removeCallbacksAndMessages(null)
    }
}

/**
 * RecyclerView adapter for displaying network flows
 */
class FlowsAdapter(private val flows: List<NetworkFlowStats>) : RecyclerView.Adapter<FlowsAdapter.FlowViewHolder>() {
    
    class FlowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flowInfo: TextView = itemView.findViewById(android.R.id.text1)
        val flowDetails: TextView = itemView.findViewById(android.R.id.text2)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_2,
            parent,
            false
        )
        return FlowViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FlowViewHolder, position: Int) {
        val flow = flows[position]
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        holder.flowInfo.text = "Flow ${position + 1} - ${flow.label} (${dateFormat.format(Date(flow.flowStartTime))})"
        holder.flowDetails.text = "Fwd: ${flow.totalFwdPackets}, Bwd: ${flow.totalBwdPackets}, " +
                "Rate: ${"%.2f".format(flow.flowBytesPerSecond)} B/s"
    }
    
    override fun getItemCount(): Int = flows.size
}

/**
 * Data class for batch payload sent to server
 */
data class BatchPayload(
    val deviceId: String,
    val timestamp: Long,
    val batchSize: Int,
    val flows: List<Map<String, Any>>,
    val pytorchAnalysis: Map<String, Any>? = null
)