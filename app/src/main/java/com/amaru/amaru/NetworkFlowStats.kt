package com.amaru.amaru

import kotlin.math.sqrt

/**
 * Data class representing network flow statistics
 * Based on CICAndMal2017 dataset features
 */
data class NetworkFlowStats(
    // Basic packet counts
    var totalFwdPackets: Int = 0,
    var totalBwdPackets: Int = 0,
    
    // Total lengths
    var totalLengthFwdPackets: Long = 0,
    var totalLengthBwdPackets: Long = 0,
    
    // Packet lengths collections for statistical calculations
    val fwdPacketLengths: MutableList<Int> = mutableListOf(),
    val bwdPacketLengths: MutableList<Int> = mutableListOf(),
    
    // Inter-arrival times (IAT) in milliseconds
    val fwdIATs: MutableList<Long> = mutableListOf(),
    val bwdIATs: MutableList<Long> = mutableListOf(),
    val flowIATs: MutableList<Long> = mutableListOf(),
    
    // Timestamps
    var flowStartTime: Long = 0,
    var flowEndTime: Long = 0,
    var lastFwdPacketTime: Long = 0,
    var lastBwdPacketTime: Long = 0,
    
    // Label for classification
    var label: String = "Unknown"
) {
    
    // Forward packet length statistics
    val fwdPacketLengthMax: Int
        get() = if (fwdPacketLengths.isNotEmpty()) fwdPacketLengths.maxOrNull() ?: 0 else 0
    
    val fwdPacketLengthMean: Double
        get() = if (fwdPacketLengths.isNotEmpty()) fwdPacketLengths.average() else 0.0
    
    val fwdPacketLengthStd: Double
        get() = calculateStandardDeviation(fwdPacketLengths.map { it.toDouble() })
    
    // Backward packet length statistics
    val bwdPacketLengthMax: Int
        get() = if (bwdPacketLengths.isNotEmpty()) bwdPacketLengths.maxOrNull() ?: 0 else 0
    
    val bwdPacketLengthMin: Int
        get() = if (bwdPacketLengths.isNotEmpty()) bwdPacketLengths.minOrNull() ?: 0 else 0
    
    val bwdPacketLengthMean: Double
        get() = if (bwdPacketLengths.isNotEmpty()) bwdPacketLengths.average() else 0.0
    
    val bwdPacketLengthStd: Double
        get() = calculateStandardDeviation(bwdPacketLengths.map { it.toDouble() })
    
    // Flow rate statistics
    val flowBytesPerSecond: Double
        get() {
            val duration = (flowEndTime - flowStartTime) / 1000.0 // Convert to seconds
            return if (duration > 0) (totalLengthFwdPackets + totalLengthBwdPackets) / duration else 0.0
        }
    
    val flowPacketsPerSecond: Double
        get() {
            val duration = (flowEndTime - flowStartTime) / 1000.0 // Convert to seconds
            return if (duration > 0) (totalFwdPackets + totalBwdPackets) / duration else 0.0
        }
    
    // Flow IAT statistics
    val flowIATMean: Double
        get() = if (flowIATs.isNotEmpty()) flowIATs.average() else 0.0
    
    val flowIATStd: Double
        get() = calculateStandardDeviation(flowIATs.map { it.toDouble() })
    
    val flowIATMax: Long
        get() = if (flowIATs.isNotEmpty()) flowIATs.maxOrNull() ?: 0 else 0
    
    val flowIATMin: Long
        get() = if (flowIATs.isNotEmpty()) flowIATs.minOrNull() ?: 0 else 0
    
    // Forward IAT statistics
    val fwdIATMean: Double
        get() = if (fwdIATs.isNotEmpty()) fwdIATs.average() else 0.0
    
    val fwdIATStd: Double
        get() = calculateStandardDeviation(fwdIATs.map { it.toDouble() })
    
    val fwdIATMax: Long
        get() = if (fwdIATs.isNotEmpty()) fwdIATs.maxOrNull() ?: 0 else 0
    
    val fwdIATMin: Long
        get() = if (fwdIATs.isNotEmpty()) fwdIATs.minOrNull() ?: 0 else 0
    
    // Backward IAT statistics
    val bwdIATMean: Double
        get() = if (bwdIATs.isNotEmpty()) bwdIATs.average() else 0.0
    
    val bwdIATStd: Double
        get() = calculateStandardDeviation(bwdIATs.map { it.toDouble() })
    
    val bwdIATMax: Long
        get() = if (bwdIATs.isNotEmpty()) bwdIATs.maxOrNull() ?: 0 else 0
    
    val bwdIATMin: Long
        get() = if (bwdIATs.isNotEmpty()) bwdIATs.minOrNull() ?: 0 else 0
    
    // General packet length statistics
    val minPacketLength: Int
        get() {
            val allLengths = fwdPacketLengths + bwdPacketLengths
            return if (allLengths.isNotEmpty()) allLengths.minOrNull() ?: 0 else 0
        }
    
    val maxPacketLength: Int
        get() {
            val allLengths = fwdPacketLengths + bwdPacketLengths
            return if (allLengths.isNotEmpty()) allLengths.maxOrNull() ?: 0 else 0
        }
    
    val packetLengthMean: Double
        get() {
            val allLengths = fwdPacketLengths + bwdPacketLengths
            return if (allLengths.isNotEmpty()) allLengths.average() else 0.0
        }
    
    val packetLengthStd: Double
        get() {
            val allLengths = fwdPacketLengths + bwdPacketLengths
            return calculateStandardDeviation(allLengths.map { it.toDouble() })
        }
    
    // Packets per second by direction
    val fwdPacketsPerSecond: Double
        get() {
            val duration = (flowEndTime - flowStartTime) / 1000.0 // Convert to seconds
            return if (duration > 0) totalFwdPackets / duration else 0.0
        }
    
    val bwdPacketsPerSecond: Double
        get() {
            val duration = (flowEndTime - flowStartTime) / 1000.0 // Convert to seconds
            return if (duration > 0) totalBwdPackets / duration else 0.0
        }
    
    /**
     * Calculate standard deviation of a list of values
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    /**
     * Add a forward packet to the statistics
     */
    fun addForwardPacket(length: Int, timestamp: Long) {
        totalFwdPackets++
        totalLengthFwdPackets += length
        fwdPacketLengths.add(length)
        
        if (flowStartTime == 0L) {
            flowStartTime = timestamp
        }
        
        // Calculate IAT
        if (lastFwdPacketTime != 0L) {
            val iat = timestamp - lastFwdPacketTime
            fwdIATs.add(iat)
            flowIATs.add(iat)
        }
        
        lastFwdPacketTime = timestamp
        flowEndTime = timestamp
    }
    
    /**
     * Add a backward packet to the statistics
     */
    fun addBackwardPacket(length: Int, timestamp: Long) {
        totalBwdPackets++
        totalLengthBwdPackets += length
        bwdPacketLengths.add(length)
        
        if (flowStartTime == 0L) {
            flowStartTime = timestamp
        }
        
        // Calculate IAT
        if (lastBwdPacketTime != 0L) {
            val iat = timestamp - lastBwdPacketTime
            bwdIATs.add(iat)
            flowIATs.add(iat)
        }
        
        lastBwdPacketTime = timestamp
        flowEndTime = timestamp
    }
    
    /**
     * Convert to CSV format matching CICAndMal2017 dataset
     */
    fun toCSV(): String {
        return listOf(
            totalFwdPackets,
            totalBwdPackets,
            totalLengthFwdPackets,
            totalLengthBwdPackets,
            fwdPacketLengthMax,
            fwdPacketLengthMean,
            fwdPacketLengthStd,
            bwdPacketLengthMax,
            bwdPacketLengthMin,
            bwdPacketLengthMean,
            bwdPacketLengthStd,
            flowBytesPerSecond,
            flowPacketsPerSecond,
            flowIATMean,
            flowIATStd,
            flowIATMax,
            flowIATMin,
            fwdIATMean,
            fwdIATStd,
            fwdIATMax,
            fwdIATMin,
            bwdIATMean,
            bwdIATStd,
            bwdIATMax,
            bwdIATMin,
            minPacketLength,
            maxPacketLength,
            packetLengthMean,
            packetLengthStd,
            fwdPacketsPerSecond,
            bwdPacketsPerSecond,
            label
        ).joinToString(",")
    }
    
    /**
     * Convert to feature array for PyTorch ML model
     * Returns 31 features (excluding label) as FloatArray
     */
    fun toFeatureArray(): FloatArray {
        return floatArrayOf(
            totalFwdPackets.toFloat(),
            totalBwdPackets.toFloat(),
            totalLengthFwdPackets.toFloat(),
            totalLengthBwdPackets.toFloat(),
            fwdPacketLengthMax.toFloat(),
            fwdPacketLengthMean.toFloat(),
            fwdPacketLengthStd.toFloat(),
            bwdPacketLengthMax.toFloat(),
            bwdPacketLengthMin.toFloat(),
            bwdPacketLengthMean.toFloat(),
            bwdPacketLengthStd.toFloat(),
            flowBytesPerSecond.toFloat(),
            flowPacketsPerSecond.toFloat(),
            flowIATMean.toFloat(),
            flowIATStd.toFloat(),
            flowIATMax.toFloat(),
            flowIATMin.toFloat(),
            fwdIATMean.toFloat(),
            fwdIATStd.toFloat(),
            fwdIATMax.toFloat(),
            fwdIATMin.toFloat(),
            bwdIATMean.toFloat(),
            bwdIATStd.toFloat(),
            bwdIATMax.toFloat(),
            bwdIATMin.toFloat(),
            minPacketLength.toFloat(),
            maxPacketLength.toFloat(),
            packetLengthMean.toFloat(),
            packetLengthStd.toFloat(),
            fwdPacketsPerSecond.toFloat(),
            bwdPacketsPerSecond.toFloat()
        )
    }
    
    /**
     * Get CSV header
     */
    companion object {
        fun getCSVHeader(): String {
            return listOf(
                "Total Fwd Packets",
                "Total Backward Packets",
                "Total Length of Fwd Packets",
                "Total Length of Bwd Packets",
                "Fwd Packet Length Max",
                "Fwd Packet Length Mean",
                "Fwd Packet Length Std",
                "Bwd Packet Length Max",
                "Bwd Packet Length Min",
                "Bwd Packet Length Mean",
                "Bwd Packet Length Std",
                "Flow Bytes/s",
                "Flow Packets/s",
                "Flow IAT Mean",
                "Flow IAT Std",
                "Flow IAT Max",
                "Flow IAT Min",
                "Fwd IAT Mean",
                "Fwd IAT Std",
                "Fwd IAT Max",
                "Fwd IAT Min",
                "Bwd IAT Mean",
                "Bwd IAT Std",
                "Bwd IAT Max",
                "Bwd IAT Min",
                "Min Packet Length",
                "Max Packet Length",
                "Packet Length Mean",
                "Packet Length Std",
                "Fwd Packets/s",
                "Bwd Packets/s",
                "Label"
            ).joinToString(",")
        }
    }
}
