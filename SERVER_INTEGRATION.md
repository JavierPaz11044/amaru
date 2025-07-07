# Server Integration - Amaru Network Monitor

## Overview

The Amaru app now automatically sends batches of 30 network flow records **plus PyTorch AI analysis results** to a remote server via HTTP POST requests. Each device generates a unique ID that is consistently used for all requests.

## Configuration

### Server URL
The server URL is configured in `MainActivity.kt`:
```kotlin
private const val SERVER_URL = "https://myServer.status.com"
```

To change the server URL, modify this constant and rebuild the app.

### Device ID
Each device generates a unique identifier in the format: `AMARU_<timestamp>_<random>`
- Example: `AMARU_1703123456789_1234`
- The ID is persisted in SharedPreferences and remains consistent across app sessions
- The last 8 characters are displayed in the UI for identification

## Data Format

### Request Structure
The request contains both network flow data and PyTorch AI analysis results:

#### Network Flow Fields
Each flow record contains **all 31 PyTorch model features** plus identification fields:

**Flow Identification:**
- `flowStartTime`, `flowEndTime`: Flow timestamp boundaries
- `flowDuration`: Duration in milliseconds (calculated)
- `flowId`: Unique identifier (generated from timestamp and hash)
- `label`: Flow classification label

**PyTorch Model Features (31 features in exact order):**
1. `totalFwdPackets` - Total Forward Packets
2. `totalBwdPackets` - Total Backward Packets  
3. `totalLengthFwdPackets` - Total Length of Fwd Packets
4. `totalLengthBwdPackets` - Total Length of Bwd Packets
5. `fwdPacketLengthMax` - Fwd Packet Length Max
6. `fwdPacketLengthMean` - Fwd Packet Length Mean
7. `fwdPacketLengthStd` - Fwd Packet Length Std
8. `bwdPacketLengthMax` - Bwd Packet Length Max
9. `bwdPacketLengthMin` - Bwd Packet Length Min
10. `bwdPacketLengthMean` - Bwd Packet Length Mean
11. `bwdPacketLengthStd` - Bwd Packet Length Std
12. `flowBytesPerSecond` - Flow Bytes/s
13. `flowPacketsPerSecond` - Flow Packets/s
14. `flowIATMean` - Flow IAT Mean
15. `flowIATStd` - Flow IAT Std
16. `flowIATMax` - Flow IAT Max
17. `flowIATMin` - Flow IAT Min
18. `fwdIATMean` - Fwd IAT Mean
19. `fwdIATStd` - Fwd IAT Std
20. `fwdIATMax` - Fwd IAT Max
21. `fwdIATMin` - Fwd IAT Min
22. `bwdIATMean` - Bwd IAT Mean
23. `bwdIATStd` - Bwd IAT Std
24. `bwdIATMax` - Bwd IAT Max
25. `bwdIATMin` - Bwd IAT Min
26. `minPacketLength` - Min Packet Length
27. `maxPacketLength` - Max Packet Length
28. `packetLengthMean` - Packet Length Mean
29. `packetLengthStd` - Packet Length Std
30. `fwdPacketsPerSecond` - Fwd Packets/s
31. `bwdPacketsPerSecond` - Bwd Packets/s

**Note:** These 31 features match exactly the input expected by the PyTorch malware detection model.
```json
{
  "deviceId": "AMARU_1703123456789_1234",
  "timestamp": 1703123456789,
  "batchSize": 30,
  "flows": [
    {
      "flowStartTime": 1703123456789,
      "flowEndTime": 1703123456890,
      "flowDuration": 101,
      "flowId": "1703123456789_1234567890",
      "totalFwdPackets": 5,
      "totalBwdPackets": 3,
      "totalLengthFwdPackets": 1500,
      "totalLengthBwdPackets": 800,
      "fwdPacketLengthMax": 1500,
      "fwdPacketLengthMean": 300.0,
      "fwdPacketLengthStd": 125.5,
      "bwdPacketLengthMax": 400,
      "bwdPacketLengthMin": 64,
      "bwdPacketLengthMean": 266.67,
      "bwdPacketLengthStd": 98.3,
      "flowBytesPerSecond": 23.45,
      "flowPacketsPerSecond": 8.2,
      "flowIATMean": 25.5,
      "flowIATStd": 15.2,
      "flowIATMax": 45,
      "flowIATMin": 10,
      "fwdIATMean": 30.0,
      "fwdIATStd": 12.8,
      "fwdIATMax": 50,
      "fwdIATMin": 15,
      "bwdIATMean": 20.0,
      "bwdIATStd": 8.5,
      "bwdIATMax": 35,
      "bwdIATMin": 5,
      "minPacketLength": 40,
      "maxPacketLength": 1500,
      "packetLengthMean": 287.5,
      "packetLengthStd": 112.4,
      "fwdPacketsPerSecond": 4.95,
      "bwdPacketsPerSecond": 2.97,
      "label": "TCP_Flow"
    }
    // ... 29 more flows
  ],
  "pytorchAnalysis": {
    "modelStatus": "Active",
    "mlConfidence": 0.85,
    "mlRiskLevel": "HIGH",
    "memoryUtilization": 0.65,
    "riskLevel": "HIGH",
    "recommendations": [
      "🔴 High risk detected in network traffic",
      "🔍 Review suspicious connections"
    ],
    "analysisTimestamp": 1703123456789
  }
}
```

### PyTorch Analysis Fields
The `pytorchAnalysis` object contains the following fields:

- **`modelStatus`**: Status of the PyTorch model ("Active", "Not initialized", "Error", etc.)
- **`mlConfidence`**: Confidence level of the ML prediction (0.0 to 1.0)
- **`mlRiskLevel`**: Risk level determined by ML model ("SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL")
- **`memoryUtilization`**: Memory utilization of the model (0.0 to 1.0)
- **`riskLevel`**: Final risk assessment ("SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL")
- **`recommendations`**: Array of recommendation strings
- **`analysisTimestamp`**: Timestamp when the analysis was performed

### Analysis Availability
- **Normal Case**: When PyTorch model is active, full analysis results are included
- **Model Not Initialized**: Fallback analysis with `modelStatus: "Not initialized"`
- **Analysis Error**: Error analysis with `modelStatus: "Error: [error message]"`
- **Batch Progress**: During batch collection, progress analysis may be sent

### Response Format
The server should respond with:
```json
{
  "status": "success",
  "message": "Batch received successfully",
  "deviceId": "AMARU_1703123456789_1234",
  "recordsReceived": 30,
  "timestamp": "2023-12-20 15:30:45"
}
```

## Testing

### Local Test Server
A Python test server is included (`test_server.py`):

```bash
# Install Flask
pip install flask

# Run the test server
python test_server.py
```

The server will:
- Listen on `http://0.0.0.0:5000`
- Log all received batches
- Save data to `logs/` directory
- Print detailed information about each batch

### Update App for Testing
To test with the local server:
1. Find your computer's IP address on the local network
2. Update `SERVER_URL` in `MainActivity.kt` to: `http://YOUR_IP:5000`
3. Rebuild and install the app

## UI Indicators

The app shows server communication status in the PyTorch AI Model Results section:

- **📡 Server Status: Ready - Device ID: ...1234** - Ready to send data
- **📡 Server Status: Sending batch (30 records + AI analysis)...** - Currently sending
- **📡 Server Status: ✅ Last batch + AI analysis sent successfully** - Successful transmission
- **📡 Server Status: ⚠️ Error 500 - Internal Server Error** - Server error
- **📡 Server Status: ❌ Network error: Connection timeout** - Network issues

## Behavior

### Batch Collection
- The app collects network flow data continuously during monitoring
- Every 30 records, a batch is automatically sent to the server **with PyTorch analysis results**
- The batch includes both flow data and AI analysis results
- If the PyTorch model is not available, fallback analysis results are sent
- After sending, the batch is cleared and collection starts again

### Error Handling
- Network errors are logged and displayed in the UI
- Server errors (4xx/5xx) are handled gracefully
- Failed batches are not retried (data is lost)
- The app continues collecting new batches even after errors
- **PyTorch analysis is always included**, even if the model fails (with error status)

### Threading
- Server communication happens on a background thread
- UI updates are posted to the main thread
- The monitoring process is not blocked by server communication

## Security Considerations

### HTTPS Support
- The app supports both HTTP and HTTPS
- For production, use HTTPS URLs
- The `android:usesCleartextTraffic="true"` setting allows HTTP for testing

### Data Privacy
- All network flow data **and PyTorch analysis results** are sent to the configured server
- The device ID is persistent and can be used to track devices
- No personal information is included in the payload
- Network flow data may contain sensitive information about app usage
- PyTorch analysis results include risk assessments and recommendations

## Troubleshooting

### Common Issues

1. **Network Permission Denied**
   - Ensure `INTERNET` permission is granted in AndroidManifest.xml

2. **Server Not Reachable**
   - Check if the server URL is correct and accessible
   - Verify network connectivity on the device
   - For local testing, ensure the device and server are on the same network

3. **Data Not Received**
   - Check server logs for errors
   - Verify JSON parsing on the server side
   - Ensure the server accepts POST requests with JSON content

4. **UI Shows Network Error**
   - Check device internet connection
   - Verify server is running and accessible
   - Check firewall settings on server

### Debug Information
- Long press the "Export Data" button to see debug information
- Check Android logs (Logcat) for detailed error messages
- Use the test server to verify data format and transmission

## Server Implementation Tips

### Required Endpoints
```
POST / - Receive batch data
GET /health - Health check (optional)
```

### Error Responses
Return appropriate HTTP status codes:
- 200: Success
- 400: Bad request (invalid JSON)
- 500: Internal server error

### Data Processing
- Validate the JSON structure before processing
- Store the deviceId for tracking
- Handle potential duplicate batches
- Consider implementing rate limiting per device
- **Process PyTorch analysis results** for threat detection and monitoring
- Store both flow data and AI analysis results for comprehensive analysis

### Example Server (Python/Flask)
See `test_server.py` for a complete example implementation. 