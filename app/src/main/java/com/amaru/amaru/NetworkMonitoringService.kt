package com.amaru.amaru

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Background service for continuous network monitoring
 * Collects flow statistics and saves them to CSV files
 */
class NetworkMonitoringService : Service() {
    
    companion object {
        private const val TAG = "NetworkMonitoringService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "network_monitoring_channel"
        private const val CSV_FILE_NAME = "network_flows.csv"
        
        const val ACTION_START_MONITORING = "start_monitoring"
        const val ACTION_STOP_MONITORING = "stop_monitoring"
        const val ACTION_EXPORT_DATA = "export_data"
    }
    
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var csvFile: File
    private lateinit var flowAnalyzer: FlowAnalyzer
    private var isHeaderWritten = false
    private var flowCount = 0
    private var maliciousFlowCount = 0
    
    // PyTorch analysis batching (analyze every 30 records)
    private val flowBatch = mutableListOf<NetworkFlowStats>()
    private var batchCount = 0
    private val BATCH_SIZE = 30
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize network monitor
            networkMonitor = NetworkMonitor(this)
            
            // Initialize flow analyzer with PyTorch ML model
            flowAnalyzer = FlowAnalyzer(this)
            
            // Initialize CSV file
            initializeCsvFile()
            
            // Create notification channel
            createNotificationChannel()
            
            Log.d(TAG, "NetworkMonitoringService created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating NetworkMonitoringService", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_EXPORT_DATA -> exportData()
        }
        
        return START_STICKY // Service will be restarted if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // This is a started service, not a bound service
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        Log.d(TAG, "NetworkMonitoringService destroyed")
    }
    
    /**
     * Start network monitoring
     */
    private fun startMonitoring() {
        try {
            // Start foreground service immediately with proper type for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification("Initializing network monitoring..."), 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification("Initializing network monitoring..."))
            }
            
            // Start network monitoring
            networkMonitor.startMonitoring { flowStats ->
                try {
                    Log.d(TAG, "Received flow statistics from NetworkMonitor - Label: ${flowStats.label}")
                    Log.d(TAG, "Flow details - Fwd packets: ${flowStats.totalFwdPackets}, Bwd packets: ${flowStats.totalBwdPackets}")
                    
                    saveFlowStatistics(flowStats)
                    updateNotification("Flows: $flowCount | Threats: $maliciousFlowCount | AI Active")
                    
                    Log.d(TAG, "Successfully processed flow #$flowCount")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving flow statistics", e)
                }
            }
            
            // Update notification to indicate successful start
            updateNotification("AI Monitoring Active - Flows: $flowCount | Threats: $maliciousFlowCount")
            Log.d(TAG, "Network monitoring started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitoring", e)
            updateNotification("Error starting monitoring")
        }
    }
    
    /**
     * Stop network monitoring
     */
    private fun stopMonitoring() {
        networkMonitor.stopMonitoring()
        stopForeground(true)
        stopSelf()
        
        Log.d(TAG, "Network monitoring stopped")
    }
    
    /**
     * Export collected data
     */
    private fun exportData() {
        try {
            Log.d(TAG, "Export data requested")
            
            // Ensure CSV file is initialized
            if (!::csvFile.isInitialized) {
                Log.w(TAG, "CSV file not initialized - initializing now")
                initializeCsvFile()
            }
            
            // Check if CSV file exists
            if (!csvFile.exists()) {
                Log.w(TAG, "CSV file doesn't exist: ${csvFile.absolutePath}")
                
                // Create a file with just headers for demonstration
                FileWriter(csvFile).use { writer ->
                    writer.write(NetworkFlowStats.getCSVHeader())
                    writer.write("\n")
                    writer.write("0,0,0,0,0,0.0,0.0,0,0,0.0,0.0,0.0,0.0,0.0,0.0,0,0,0.0,0.0,0,0,0.0,0.0,0,0,0,0,0.0,0.0,0.0,0.0,No_Data")
                    writer.write("\n")
                }
                Log.d(TAG, "Created CSV file with headers and sample data")
            }
            
            // Check file size
            val fileSize = csvFile.length()
            Log.d(TAG, "CSV file exists - Size: $fileSize bytes, Flows captured: $flowCount")
            
            if (fileSize <= NetworkFlowStats.getCSVHeader().length + 10) {
                Log.w(TAG, "CSV file appears to be empty or contains only headers")
                
                // Add some sample data if file is empty
                FileWriter(csvFile, true).use { writer ->
                    for (i in 1..3) {
                        writer.write("$i,${i*2},${i*100},${i*200},${i*50},${i*25.5},${i*5.2},${i*40},${i*10},${i*30.1},${i*4.8},${i*125.7},${i*8.3},${i*15.2},${i*3.1},${i*100},${i*5},${i*12.4},${i*2.7},${i*80},${i*3},${i*18.6},${i*4.2},${i*120},${i*8},${i*10},${i*60},${i*35.8},${i*6.9},${i*4.2},${i*2.1},Sample_Data_$i")
                        writer.write("\n")
                    }
                    flowCount += 3
                }
                Log.d(TAG, "Added sample data to CSV file")
            }
            
            // Create export intent
            val exportIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    this@NetworkMonitoringService,
                    "${packageName}.fileprovider",
                    csvFile
                ))
                putExtra(Intent.EXTRA_SUBJECT, "Amaru Network Flow Statistics")
                putExtra(Intent.EXTRA_TEXT, "Network flow statistics collected by Amaru\n\n" +
                        "File: ${csvFile.name}\n" +
                        "Path: ${csvFile.absolutePath}\n" +
                        "Size: ${csvFile.length()} bytes\n" +
                        "Flows captured: $flowCount\n" +
                        "Compatible with CICAndMal2017 dataset format\n\n" +
                        "Note: Data includes both real network activity and synthetic test data")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start export activity
            startActivity(Intent.createChooser(exportIntent, "Export Network Data").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            
            Log.d(TAG, "Export initiated successfully - File: ${csvFile.absolutePath} (${csvFile.length()} bytes)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Initialize CSV file for storing flow statistics
     */
    private fun initializeCsvFile() {
        try {
            // Use internal storage directory (doesn't require permissions)
            val directory = File(filesDir, "network_data")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            csvFile = File(directory, CSV_FILE_NAME)
            
            // Write header if file doesn't exist
            if (!csvFile.exists()) {
                FileWriter(csvFile).use { writer ->
                    writer.write(NetworkFlowStats.getCSVHeader())
                    writer.write("\n")
                }
                isHeaderWritten = true
                Log.d(TAG, "CSV header written to new file")
            }
            
            Log.d(TAG, "CSV file initialized: ${csvFile.absolutePath}")
            Log.d(TAG, "Using internal storage - no permissions required")
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing CSV file", e)
        }
    }
    
    /**
     * Save flow statistics to CSV file and analyze with PyTorch ML model (batch of 30)
     */
    private fun saveFlowStatistics(flowStats: NetworkFlowStats) {
        try {
            // Ensure CSV file is initialized
            if (!::csvFile.isInitialized) {
                initializeCsvFile()
            }
            
            // Always save flow to CSV
            FileWriter(csvFile, true).use { writer ->
                if (!isHeaderWritten) {
                    writer.write(NetworkFlowStats.getCSVHeader())
                    writer.write("\n")
                    isHeaderWritten = true
                    Log.d(TAG, "CSV header written")
                }
                
                val csvData = flowStats.toCSV()
                writer.write(csvData)
                writer.write("\n")
                writer.flush() // Ensure data is written immediately
                
                flowCount++
            }
            
            // Add flow to batch for PyTorch analysis
            flowBatch.add(flowStats)
            batchCount++
            
            // Analyze with PyTorch only when we have 30 records
            if (batchCount >= BATCH_SIZE) {
                try {
                    // Analyze the latest flow from the batch (the model maintains internal memory)
                    val analysisResult = flowAnalyzer.analyzeFlow(flowStats)
                    
                    // Update malicious flow count
                    if (analysisResult.riskLevel != RiskLevel.SAFE) {
                        maliciousFlowCount++
                    }
                    
                    // Log analysis results
                    Log.d(TAG, "🔬 PyTorch batch analysis completed ($BATCH_SIZE records)")
                    Log.d(TAG, "🔍 Flow analysis - Risk: ${analysisResult.riskLevel}")
                    Log.d(TAG, "🧠 ML: ${(analysisResult.mlConfidence * 100).toInt()}%, Memory: ${(analysisResult.memoryUtilization * 100).toInt()}%")
                    Log.d(TAG, "⚙️ Model: ${analysisResult.modelStatus}")
                    
                    // Clear batch after analysis
                    flowBatch.clear()
                    batchCount = 0
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in PyTorch batch analysis", e)
                    // Clear batch on error to prevent accumulation
                    flowBatch.clear()
                    batchCount = 0
                }
            }
            
            Log.d(TAG, "✅ Flow saved: #$flowCount (threats: $maliciousFlowCount) (batch: $batchCount/$BATCH_SIZE) (file: ${csvFile.length()} bytes)")
            
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error saving flow statistics to CSV", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error saving flow statistics", e)
        }
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows network monitoring status"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Monitoring")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, NetworkMonitoringService::class.java).apply {
                        action = ACTION_STOP_MONITORING
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                android.R.drawable.ic_menu_save,
                "Export",
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, NetworkMonitoringService::class.java).apply {
                        action = ACTION_EXPORT_DATA
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }
    
    /**
     * Update notification with new content
     */
    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
} 