package com.amaru.amaru

import android.content.Context
import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced flow analyzer for detecting potential malware patterns
 * Based on CICAndMal2017 dataset characteristics
 * Enhanced with PyTorch ML model for real-time detection
 */
class FlowAnalyzer(private val context: Context) {
    
    // PyTorch ML model for enhanced detection
    private val pytorchDetector = PyTorchMalwareDetector(context)
    private var isMLModelReady = false
    
    init {
        // Initialize ML model asynchronously
        Thread {
            isMLModelReady = pytorchDetector.initializeModel()
            if (isMLModelReady) {
                Log.i(TAG, "🧠 PyTorch ML model initialized successfully")
            } else {
                Log.w(TAG, "⚠️ ML model initialization failed, using traditional patterns only")
            }
        }.start()
    }
    
    companion object {
        private const val TAG = "FlowAnalyzer"
        
        // Thresholds based on CICAndMal2017 research
        private const val SUSPICIOUS_PACKET_RATE_THRESHOLD = 100.0 // packets/second
        private const val SUSPICIOUS_BYTE_RATE_THRESHOLD = 10000.0 // bytes/second
        private const val SUSPICIOUS_IAT_VARIANCE_THRESHOLD = 0.8
        private const val SUSPICIOUS_PACKET_SIZE_VARIANCE_THRESHOLD = 0.7
        
        // Malware patterns from CICAndMal2017 dataset
        private val ADWARE_PATTERNS = mapOf(
            "high_forward_rate" to 50.0,
            "regular_intervals" to 0.3,
            "small_packets" to 200.0
        )
        
        private val RANSOMWARE_PATTERNS = mapOf(
            "burst_activity" to 0.9,
            "large_packets" to 800.0,
            "symmetric_flow" to 0.6
        )
        
        private val SCAREWARE_PATTERNS = mapOf(
            "periodic_beacons" to 0.4,
            "dns_queries" to 100.0,
            "short_flows" to 5000.0
        )
        
        private val SMS_MALWARE_PATTERNS = mapOf(
            "small_frequent_packets" to 50.0,
            "outbound_heavy" to 0.8,
            "short_intervals" to 100.0
        )
    }
    
    /**
     * Analyze flow for potential malware characteristics
     * Combines PyTorch ML prediction with traditional pattern detection
     */
    fun analyzeFlow(flowStats: NetworkFlowStats): MalwareAnalysisResult {
        val result = MalwareAnalysisResult()
        
        // Calculate flow characteristics
        val flowDuration = flowStats.flowEndTime - flowStats.flowStartTime
        val totalPackets = flowStats.totalFwdPackets + flowStats.totalBwdPackets
        val totalBytes = flowStats.totalLengthFwdPackets + flowStats.totalLengthBwdPackets
        
        // PyTorch ML-based prediction (if model is ready)
        var mlPrediction: MalwarePredictionResult? = null
        if (isMLModelReady) {
            mlPrediction = pytorchDetector.predict(flowStats)
            result.mlConfidence = mlPrediction.confidence
            result.mlRiskLevel = mlPrediction.riskLevel
            result.memoryUtilization = mlPrediction.memoryUtilization
            result.modelStatus = mlPrediction.modelStatus
        }
        
        // Traditional pattern-based detection
        result.suspiciousActivity = detectSuspiciousActivity(flowStats)
        result.adwareScore = detectAdwarePattern(flowStats)
        result.ransomwareScore = detectRansomwarePattern(flowStats)
        result.scarewareScore = detectScarewarePattern(flowStats)
        result.smsmalwareScore = detectSMSMalwarePattern(flowStats)
        
        // Combined risk assessment (ML + traditional patterns)
        result.riskLevel = calculateCombinedRiskLevel(result, mlPrediction)
        
        // Generate enhanced recommendations
        result.recommendations = generateEnhancedRecommendations(result, mlPrediction)
        
        Log.d(TAG, "Flow analysis - Risk: ${result.riskLevel}, ML: ${mlPrediction?.confidence?.let { "${(it * 100).toInt()}%" } ?: "N/A"}, Memory: ${(result.memoryUtilization * 100).toInt()}%")
        
        return result
    }
    
    /**
     * Calculate combined risk level using both ML and traditional patterns
     */
    private fun calculateCombinedRiskLevel(result: MalwareAnalysisResult, mlPrediction: MalwarePredictionResult?): RiskLevel {
        val traditionalRiskLevel = calculateTraditionalRiskLevel(result)
        
        // If ML model is not available, use traditional assessment
        if (mlPrediction == null || !isMLModelReady) {
            return traditionalRiskLevel
        }
        
        val mlRiskLevel = mlPrediction.riskLevel
        val mlConfidence = mlPrediction.confidence
        
        // If ML model has high confidence, give it more weight
        return if (mlConfidence > 0.8) {
            // High ML confidence - use ML prediction but consider traditional patterns
            when {
                mlRiskLevel == RiskLevel.CRITICAL || traditionalRiskLevel == RiskLevel.CRITICAL -> RiskLevel.CRITICAL
                mlRiskLevel == RiskLevel.HIGH || traditionalRiskLevel == RiskLevel.HIGH -> RiskLevel.HIGH
                mlRiskLevel == RiskLevel.MEDIUM || traditionalRiskLevel == RiskLevel.MEDIUM -> RiskLevel.MEDIUM
                mlRiskLevel == RiskLevel.LOW || traditionalRiskLevel == RiskLevel.LOW -> RiskLevel.LOW
                else -> RiskLevel.SAFE
            }
        } else {
            // Lower ML confidence - balance both approaches
            val traditionalWeight = 0.4
            val mlWeight = 0.6
            
            val traditionalScore = when (traditionalRiskLevel) {
                RiskLevel.CRITICAL -> 4.0
                RiskLevel.HIGH -> 3.0
                RiskLevel.MEDIUM -> 2.0
                RiskLevel.LOW -> 1.0
                RiskLevel.SAFE -> 0.0
            }
            
            val mlScore = when (mlRiskLevel) {
                RiskLevel.CRITICAL -> 4.0
                RiskLevel.HIGH -> 3.0
                RiskLevel.MEDIUM -> 2.0
                RiskLevel.LOW -> 1.0
                RiskLevel.SAFE -> 0.0
            }
            
            val combinedScore = traditionalScore * traditionalWeight + mlScore * mlWeight
            
            when {
                combinedScore >= 3.5 -> RiskLevel.CRITICAL
                combinedScore >= 2.5 -> RiskLevel.HIGH
                combinedScore >= 1.5 -> RiskLevel.MEDIUM
                combinedScore >= 0.5 -> RiskLevel.LOW
                else -> RiskLevel.SAFE
            }
        }
    }
    
    /**
     * Calculate traditional risk level (original method)
     */
    private fun calculateTraditionalRiskLevel(result: MalwareAnalysisResult): RiskLevel {
        val maxScore = maxOf(
            result.adwareScore,
            result.ransomwareScore,
            result.scarewareScore,
            result.smsmalwareScore
        )
        
        return when {
            maxScore >= 0.8 -> RiskLevel.CRITICAL
            maxScore >= 0.6 -> RiskLevel.HIGH
            maxScore >= 0.4 -> RiskLevel.MEDIUM
            maxScore >= 0.2 || result.suspiciousActivity -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
    }
    
    /**
     * Detect general suspicious activity patterns
     */
    private fun detectSuspiciousActivity(flowStats: NetworkFlowStats): Boolean {
        // High packet rate
        if (flowStats.flowPacketsPerSecond > SUSPICIOUS_PACKET_RATE_THRESHOLD) {
            return true
        }
        
        // High byte rate
        if (flowStats.flowBytesPerSecond > SUSPICIOUS_BYTE_RATE_THRESHOLD) {
            return true
        }
        
        // Unusual IAT variance
        if (flowStats.flowIATStd > 0 && flowStats.flowIATMean > 0) {
            val iatVariance = flowStats.flowIATStd / flowStats.flowIATMean
            if (iatVariance > SUSPICIOUS_IAT_VARIANCE_THRESHOLD) {
                return true
            }
        }
        
        // Unusual packet size variance
        if (flowStats.packetLengthStd > 0 && flowStats.packetLengthMean > 0) {
            val sizeVariance = flowStats.packetLengthStd / flowStats.packetLengthMean
            if (sizeVariance > SUSPICIOUS_PACKET_SIZE_VARIANCE_THRESHOLD) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Detect adware patterns
     */
    private fun detectAdwarePattern(flowStats: NetworkFlowStats): Double {
        var score = 0.0
        
        // High forward packet rate (ads requests)
        if (flowStats.fwdPacketsPerSecond > ADWARE_PATTERNS["high_forward_rate"]!!) {
            score += 0.3
        }
        
        // Regular intervals (periodic ad requests)
        if (flowStats.fwdIATStd > 0 && flowStats.fwdIATMean > 0) {
            val regularity = 1.0 - (flowStats.fwdIATStd / flowStats.fwdIATMean)
            if (regularity > ADWARE_PATTERNS["regular_intervals"]!!) {
                score += 0.4
            }
        }
        
        // Small packets (typical ad content)
        if (flowStats.fwdPacketLengthMean < ADWARE_PATTERNS["small_packets"]!!) {
            score += 0.3
        }
        
        return minOf(score, 1.0)
    }
    
    /**
     * Detect ransomware patterns
     */
    private fun detectRansomwarePattern(flowStats: NetworkFlowStats): Double {
        var score = 0.0
        
        // Burst activity (rapid file encryption)
        if (flowStats.flowIATStd > 0 && flowStats.flowIATMean > 0) {
            val burstiness = flowStats.flowIATStd / flowStats.flowIATMean
            if (burstiness > RANSOMWARE_PATTERNS["burst_activity"]!!) {
                score += 0.4
            }
        }
        
        // Large packets (encrypted file chunks)
        if (flowStats.fwdPacketLengthMean > RANSOMWARE_PATTERNS["large_packets"]!!) {
            score += 0.3
        }
        
        // Symmetric flow (C&C communication)
        val totalPackets = flowStats.totalFwdPackets + flowStats.totalBwdPackets
        if (totalPackets > 0) {
            val symmetry = 1.0 - abs(flowStats.totalFwdPackets - flowStats.totalBwdPackets).toDouble() / totalPackets
            if (symmetry > RANSOMWARE_PATTERNS["symmetric_flow"]!!) {
                score += 0.3
            }
        }
        
        return minOf(score, 1.0)
    }
    
    /**
     * Detect scareware patterns
     */
    private fun detectScarewarePattern(flowStats: NetworkFlowStats): Double {
        var score = 0.0
        
        // Periodic beacons (fake alerts)
        if (flowStats.fwdIATStd > 0 && flowStats.fwdIATMean > 0) {
            val periodicity = 1.0 - (flowStats.fwdIATStd / flowStats.fwdIATMean)
            if (periodicity > SCAREWARE_PATTERNS["periodic_beacons"]!!) {
                score += 0.4
            }
        }
        
        // High DNS-like queries (fake scanning)
        if (flowStats.totalFwdPackets.toDouble() > SCAREWARE_PATTERNS["dns_queries"]!!) {
            score += 0.3
        }
        
        // Short flows (quick interactions)
        val flowDuration = flowStats.flowEndTime - flowStats.flowStartTime
        if (flowDuration < SCAREWARE_PATTERNS["short_flows"]!!.toLong()) {
            score += 0.3
        }
        
        return minOf(score, 1.0)
    }
    
    /**
     * Detect SMS malware patterns
     */
    private fun detectSMSMalwarePattern(flowStats: NetworkFlowStats): Double {
        var score = 0.0
        
        // Small frequent packets (SMS sending)
        if (flowStats.fwdPacketLengthMean < SMS_MALWARE_PATTERNS["small_frequent_packets"]!! &&
            flowStats.fwdPacketsPerSecond > 1.0) {
            score += 0.4
        }
        
        // Outbound heavy traffic (SMS sending)
        val totalPackets = flowStats.totalFwdPackets + flowStats.totalBwdPackets
        if (totalPackets > 0) {
            val outboundRatio = flowStats.totalFwdPackets.toDouble() / totalPackets
            if (outboundRatio > SMS_MALWARE_PATTERNS["outbound_heavy"]!!) {
                score += 0.3
            }
        }
        
        // Short intervals (rapid SMS sending)
        if (flowStats.fwdIATMean < SMS_MALWARE_PATTERNS["short_intervals"]!!) {
            score += 0.3
        }
        
        return minOf(score, 1.0)
    }
    
    /**
     * Generate enhanced security recommendations using ML and traditional analysis
     */
    private fun generateEnhancedRecommendations(result: MalwareAnalysisResult, mlPrediction: MalwarePredictionResult?): List<String> {
        val recommendations = mutableListOf<String>()
        
        // ML-based recommendations
        if (mlPrediction != null && mlPrediction.isMalicious) {
            val confidence = (mlPrediction.confidence * 100).toInt()
            recommendations.add("🧠 PyTorch AI detected potential malware (${confidence}% confidence)")
            
            when (mlPrediction.riskLevel) {
                RiskLevel.CRITICAL -> {
                    recommendations.add("⚠️ CRITICAL: Immediate action required - isolate device and run full security scan")
                }
                RiskLevel.HIGH -> {
                    recommendations.add("⚠️ HIGH RISK: Run security scan and monitor device behavior closely")
                }
                RiskLevel.MEDIUM -> {
                    recommendations.add("⚠️ MEDIUM RISK: Monitor network activity and consider security scan")
                }
                RiskLevel.LOW -> {
                    recommendations.add("⚠️ LOW RISK: Continue monitoring but no immediate action needed")
                }
                else -> {}
            }
        }
        
        // Traditional pattern-based recommendations
        if (result.adwareScore > 0.5) {
            recommendations.add("📱 Potential adware detected. Check for unwanted ads or pop-ups.")
        }
        
        if (result.ransomwareScore > 0.5) {
            recommendations.add("🔒 Potential ransomware activity. Backup important files immediately.")
        }
        
        if (result.scarewareScore > 0.5) {
            recommendations.add("⚠️ Potential scareware detected. Avoid clicking on suspicious alerts.")
        }
        
        if (result.smsmalwareScore > 0.5) {
            recommendations.add("📲 Potential SMS malware. Check for unauthorized SMS messages.")
        }
        
        if (result.suspiciousActivity) {
            recommendations.add("🔍 Suspicious network activity detected. Monitor app behavior.")
        }
        
        // Memory utilization info
        if (result.memoryUtilization > 0.5) {
            recommendations.add("📊 AI memory model is learning (${(result.memoryUtilization * 100).toInt()}% capacity)")
        }
        
        // Model status info
        if (mlPrediction != null && mlPrediction.modelStatus != "Active") {
            recommendations.add("⚙️ ML Model Status: ${mlPrediction.modelStatus}")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ Network activity appears normal.")
        }
        
        return recommendations
    }
    
    /**
     * Batch analyze multiple flows
     */
    fun batchAnalyzeFlows(flows: List<NetworkFlowStats>): BatchAnalysisResult {
        val results = flows.map { analyzeFlow(it) }
        
        return BatchAnalysisResult(
            totalFlows = flows.size,
            suspiciousFlows = results.count { it.suspiciousActivity },
            criticalFlows = results.count { it.riskLevel == RiskLevel.CRITICAL },
            highRiskFlows = results.count { it.riskLevel == RiskLevel.HIGH },
            mediumRiskFlows = results.count { it.riskLevel == RiskLevel.MEDIUM },
            lowRiskFlows = results.count { it.riskLevel == RiskLevel.LOW },
            safeFlows = results.count { it.riskLevel == RiskLevel.SAFE },
            avgAdwareScore = results.map { it.adwareScore }.average(),
            avgRansomwareScore = results.map { it.ransomwareScore }.average(),
            avgScarewareScore = results.map { it.scarewareScore }.average(),
            avgSMSMalwareScore = results.map { it.smsmalwareScore }.average()
        )
    }
}

/**
 * Result of malware analysis for a single flow
 */
data class MalwareAnalysisResult(
    var suspiciousActivity: Boolean = false,
    var adwareScore: Double = 0.0,
    var ransomwareScore: Double = 0.0,
    var scarewareScore: Double = 0.0,
    var smsmalwareScore: Double = 0.0,
    var riskLevel: RiskLevel = RiskLevel.SAFE,
    var recommendations: List<String> = emptyList(),
    // PyTorch ML model results
    var mlConfidence: Double = 0.0,
    var mlRiskLevel: RiskLevel = RiskLevel.SAFE,
    var memoryUtilization: Double = 0.0,
    var modelStatus: String = "Not initialized"
)

/**
 * Result of batch analysis for multiple flows
 */
data class BatchAnalysisResult(
    val totalFlows: Int,
    val suspiciousFlows: Int,
    val criticalFlows: Int,
    val highRiskFlows: Int,
    val mediumRiskFlows: Int,
    val lowRiskFlows: Int,
    val safeFlows: Int,
    val avgAdwareScore: Double,
    val avgRansomwareScore: Double,
    val avgScarewareScore: Double,
    val avgSMSMalwareScore: Double
)

/**
 * Risk level enumeration
 */
enum class RiskLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
} 