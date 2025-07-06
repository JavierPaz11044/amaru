#!/usr/bin/env python3
"""
Example test server for Amaru Android app
Save this file outside the Android project and run it to test server integration

Usage:
1. pip install flask
2. python test_server_example.py
3. Update Android app SERVER_URL to point to your computer's IP
4. Start monitoring in the app to see data being received

The server will save received data to logs/ directory for analysis.
"""

from flask import Flask, request, jsonify
import json
from datetime import datetime
import os

app = Flask(__name__)

# Create logs directory if it doesn't exist
os.makedirs('logs', exist_ok=True)

@app.route('/', methods=['POST'])
def receive_batch():
    try:
        # Get JSON data from request
        data = request.get_json()
        
        # Log the received data
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        print(f"\n[{timestamp}] Received batch from device: {data.get('deviceId', 'Unknown')}")
        print(f"Batch size: {data.get('batchSize', 0)} records")
        print(f"Timestamp: {data.get('timestamp', 'Unknown')}")
        
        # Save to file for analysis
        log_filename = f"logs/batch_{data.get('deviceId', 'unknown')}_{data.get('timestamp', 'unknown')}.json"
        with open(log_filename, 'w') as f:
            json.dump(data, f, indent=2)
        
        print(f"Data saved to: {log_filename}")
        
        # Print first few flows for debugging
        flows = data.get('flows', [])
        if flows:
            print(f"First flow sample:")
            for key, value in flows[0].items():
                print(f"  {key}: {value}")
        
        # Print PyTorch analysis results
        pytorch_analysis = data.get('pytorchAnalysis')
        if pytorch_analysis:
            print(f"PyTorch Analysis Results:")
            print(f"  Model Status: {pytorch_analysis.get('modelStatus', 'Unknown')}")
            print(f"  ML Confidence: {pytorch_analysis.get('mlConfidence', 0.0)}")
            print(f"  Risk Level: {pytorch_analysis.get('riskLevel', 'Unknown')}")
            print(f"  Memory Utilization: {pytorch_analysis.get('memoryUtilization', 0.0)}")
            recommendations = pytorch_analysis.get('recommendations', [])
            if recommendations:
                print(f"  Recommendations: {', '.join(recommendations)}")
        else:
            print("No PyTorch analysis data received")
        
        # Return success response
        response = {
            'status': 'success',
            'message': 'Batch received successfully',
            'deviceId': data.get('deviceId'),
            'recordsReceived': len(flows),
            'timestamp': timestamp
        }
        
        return jsonify(response), 200
        
    except Exception as e:
        print(f"Error processing batch: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat()
    })

if __name__ == '__main__':
    print("🚀 Starting Amaru test server...")
    print("📡 Server will receive data on: http://localhost:5000")
    print("💡 Update your Android app to use: http://YOUR_IP:5000")
    print("🔍 Data will be saved to logs/ directory")
    print("=" * 50)
    
    app.run(host='0.0.0.0', port=5000, debug=True) 