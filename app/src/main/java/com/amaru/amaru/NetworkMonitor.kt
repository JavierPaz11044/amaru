package com.amaru.amaru

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Network monitor class that captures network flow statistics
 * Based on Android's available network APIs and simulated packet-level data
 */
class NetworkMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val MONITORING_INTERVAL = 10000L // 10 seconds
        private const val FLOW_TIMEOUT = 30000L // 30 seconds timeout for flows
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val flows = ConcurrentHashMap<String, NetworkFlowStats>()
    private val random = Random()
    
    // Previous measurements for calculating deltas
    private var previousRxBytes = 0L
    private var previousTxBytes = 0L
    private var previousRxPackets = 0L
    private var previousTxPackets = 0L
    private var lastMeasurementTime = 0L
    
    private var isMonitoring = false
    private var onFlowStatsUpdated: ((NetworkFlowStats) -> Unit)? = null
    
    /**
     * Start monitoring network traffic
     */
    fun startMonitoring(onStatsUpdated: (NetworkFlowStats) -> Unit) {
        if (isMonitoring) return
        
        isMonitoring = true
        onFlowStatsUpdated = onStatsUpdated
        

        initializeBaselineMeasurements()
        

        scheduleNextMeasurement()
        
        Log.d(TAG, "Network monitoring started")
    }
    
    /**
     * Stop monitoring network traffic
     */
    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        onFlowStatsUpdated = null
        flows.clear()
        
        Log.d(TAG, "Network monitoring stopped")
    }
    
    /**
     * Initialize baseline measurements
     */
    private fun initializeBaselineMeasurements() {
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()
        previousRxPackets = TrafficStats.getTotalRxPackets()
        previousTxPackets = TrafficStats.getTotalTxPackets()
        lastMeasurementTime = System.currentTimeMillis()
    }
    
    /**
     * Schedule the next measurement
     */
    private fun scheduleNextMeasurement() {
        handler.postDelayed({
            if (isMonitoring) {
                measureNetworkActivity()
                scheduleNextMeasurement()
            }
        }, MONITORING_INTERVAL)
    }
    
    /**
     * Measure network activity and update flow statistics
     */
    private fun measureNetworkActivity() {
        try {
            val currentTime = System.currentTimeMillis()
            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()
            val currentRxPackets = TrafficStats.getTotalRxPackets()
            val currentTxPackets = TrafficStats.getTotalTxPackets()

            if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong() || 
                currentTxBytes == TrafficStats.UNSUPPORTED.toLong()) {
                Log.w(TAG, "TrafficStats not supported on this device")
                return
            }
            
            // Calculate deltas (ensure positive values)
            val deltaRxBytes = maxOf(0, currentRxBytes - previousRxBytes)
            val deltaTxBytes = maxOf(0, currentTxBytes - previousTxBytes)
            val deltaRxPackets = maxOf(0, currentRxPackets - previousRxPackets)
            val deltaTxPackets = maxOf(0, currentTxPackets - previousTxPackets)
            val deltaTime = currentTime - lastMeasurementTime
            
            // Generate flow statistics based on network activity or synthetic data for testing
            if (deltaRxBytes > 0 || deltaTxBytes > 0 || deltaRxPackets > 0 || deltaTxPackets > 0) {
                Log.d(TAG, "Real network activity detected")
                generateFlowStatistics(deltaRxBytes, deltaTxBytes, deltaRxPackets, deltaTxPackets, currentTime, deltaTime)
            } else {
                Log.d(TAG, "No real network activity detected - generating synthetic data for testing")
                // Generate synthetic data for testing in emulator
                generateSyntheticFlowStatistics(currentTime, deltaTime)
            }
            
            // Update previous values
            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes
            previousRxPackets = currentRxPackets
            previousTxPackets = currentTxPackets
            lastMeasurementTime = currentTime
            
            // Clean up old flows
            cleanupOldFlows(currentTime)
            
            Log.d(TAG, "Network activity measured - RX: ${deltaRxBytes}B, TX: ${deltaTxBytes}B, RX Packets: $deltaRxPackets, TX Packets: $deltaTxPackets")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error measuring network activity", e)
        }
    }
    
    /**
     * Generate flow statistics based on network activity
     * This simulates packet-level analysis since Android doesn't provide direct access without root
     */
    private fun generateFlowStatistics(
        rxBytes: Long,
        txBytes: Long,
        rxPackets: Long,
        txPackets: Long,
        timestamp: Long,
        deltaTime: Long
    ) {
        try {
            // Generate a flow key based on network state and time
            val flowKey = "flow_${timestamp / MONITORING_INTERVAL}"
            
            // Get or create flow statistics
            val flowStats = flows.getOrPut(flowKey) {
                NetworkFlowStats().apply {
                    flowStartTime = timestamp
                    label = detectNetworkType()
                }
            }
            
            // Simulate packet distribution based on total bytes and packets
            simulatePacketDistribution(flowStats, rxBytes, txBytes, rxPackets, txPackets, timestamp)
            
            // Update flow end time
            flowStats.flowEndTime = timestamp
            
            Log.d(TAG, "Generated flow statistics for real network activity - Key: $flowKey")
            
            // Notify listeners
            onFlowStatsUpdated?.invoke(flowStats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating flow statistics", e)
        }
    }
    
    /**
     * Generate synthetic flow statistics for testing (useful in emulators)
     */
    private fun generateSyntheticFlowStatistics(timestamp: Long, deltaTime: Long) {
        try {
            // Generate a flow key based on timestamp
            val flowKey = "synthetic_flow_${timestamp / MONITORING_INTERVAL}"
            
            // Create synthetic flow statistics
            val flowStats = flows.getOrPut(flowKey) {
                NetworkFlowStats().apply {
                    flowStartTime = timestamp - random.nextInt(5000) // Vary start time slightly
                    label = "${detectNetworkType()}_Synthetic"
                }
            }
            
            // Generate synthetic packet data
            val syntheticTxPackets = random.nextInt(20) + 5  // 5-25 packets
            val syntheticRxPackets = random.nextInt(30) + 10 // 10-40 packets
            val syntheticTxBytes = syntheticTxPackets * (random.nextInt(800) + 64) // Variable packet sizes
            val syntheticRxBytes = syntheticRxPackets * (random.nextInt(1200) + 64)
            
            // Simulate packet distribution with synthetic data
            simulatePacketDistribution(
                flowStats, 
                syntheticRxBytes.toLong(), 
                syntheticTxBytes.toLong(), 
                syntheticRxPackets.toLong(), 
                syntheticTxPackets.toLong(), 
                timestamp
            )
            
            // Update flow end time
            flowStats.flowEndTime = timestamp
            
            Log.d(TAG, "Generated SYNTHETIC flow statistics - Key: $flowKey, TX: $syntheticTxPackets packets, RX: $syntheticRxPackets packets")
            
            // Notify listeners
            onFlowStatsUpdated?.invoke(flowStats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synthetic flow statistics", e)
        }
    }
    
    /**
     * Simulate packet distribution to generate realistic flow statistics
     */
    private fun simulatePacketDistribution(
        flowStats: NetworkFlowStats,
        rxBytes: Long,
        txBytes: Long,
        rxPackets: Long,
        txPackets: Long,
        timestamp: Long
    ) {
        try {
            // Limit packet simulation to avoid performance issues
            val maxPacketsToSimulate = 100
            
            // Simulate forward packets (outgoing)
            val avgTxPacketSize = if (txPackets > 0) (txBytes / txPackets).toInt() else 0
            val txPacketsToSimulate = minOf(txPackets, maxPacketsToSimulate.toLong())
            
            for (i in 0 until txPacketsToSimulate) {
                val packetSize = generateRealisticPacketSize(avgTxPacketSize)
                val packetTimestamp = timestamp + random.nextInt(MONITORING_INTERVAL.toInt()).toLong()
                flowStats.addForwardPacket(packetSize, packetTimestamp)
            }
            
            // Simulate backward packets (incoming)
            val avgRxPacketSize = if (rxPackets > 0) (rxBytes / rxPackets).toInt() else 0
            val rxPacketsToSimulate = minOf(rxPackets, maxPacketsToSimulate.toLong())
            
            for (i in 0 until rxPacketsToSimulate) {
                val packetSize = generateRealisticPacketSize(avgRxPacketSize)
                val packetTimestamp = timestamp + random.nextInt(MONITORING_INTERVAL.toInt()).toLong()
                flowStats.addBackwardPacket(packetSize, packetTimestamp)
            }
            
            Log.d(TAG, "Simulated $txPacketsToSimulate TX and $rxPacketsToSimulate RX packets")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating packet distribution", e)
        }
    }
    
    /**
     * Generate realistic packet sizes based on average with some variation
     */
    private fun generateRealisticPacketSize(avgSize: Int): Int {
        try {
            if (avgSize <= 0) return random.nextInt(1340) + 60 // Typical packet size range 60-1400
            
            // Add some variation (±20%) but ensure positive values
            val variation = maxOf(1, (avgSize * 0.2).toInt())
            val randomVariation = random.nextInt(variation * 2) - variation
            return maxOf(60, minOf(1500, avgSize + randomVariation)) // Keep within reasonable bounds
            
        } catch (e: Exception) {
            Log.w(TAG, "Error generating packet size, using default", e)
            return 512 // Default packet size
        }
    }
    
    /**
     * Detect network type for labeling
     */
    private fun detectNetworkType(): String {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            return when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting network type", e)
            return "Unknown"
        }
    }
    
    /**
     * Clean up old flows that have timed out
     */
    private fun cleanupOldFlows(currentTime: Long) {
        val iterator = flows.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.flowEndTime > FLOW_TIMEOUT) {
                iterator.remove()
                Log.d(TAG, "Cleaned up old flow: ${entry.key}")
            }
        }
    }
    
    /**
     * Get current flows
     */
    fun getCurrentFlows(): Map<String, NetworkFlowStats> {
        return flows.toMap()
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get network information
     */
    fun getNetworkInfo(): String {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            val type = when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
            
            val speed = when {
                networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> "Connected"
                else -> "Disconnected"
            }
            
            "$type - $speed"
        } catch (e: Exception) {
            "Unknown - Error"
        }
    }
} 