# Amaru Network Monitor

Una aplicación Android para monitoreo de tráfico de red en tiempo real que calcula las características del dataset CICAndMal2017 para detección de malware.

## 📋 Descripción

Esta aplicación implementa un sistema de monitoreo de red que captura y analiza el tráfico de red cada 10 segundos, calculando todas las características utilizadas en el dataset CICAndMal2017 de la Universidad de New Brunswick:

### 🔍 Características Calculadas

- **Total Fwd Packets**: Número total de paquetes enviados hacia adelante
- **Total Backward Packets**: Número total de paquetes enviados hacia atrás
- **Total Length of Fwd/Bwd Packets**: Suma total de bytes en paquetes
- **Fwd/Bwd Packet Length Max/Min/Mean/Std**: Estadísticas de longitud de paquetes
- **Flow Bytes/s**: Bytes por segundo del flujo
- **Flow Packets/s**: Paquetes por segundo del flujo
- **Flow IAT Mean/Std/Max/Min**: Estadísticas del tiempo entre arribos
- **Fwd/Bwd IAT Mean/Std/Max/Min**: Estadísticas del tiempo entre arribos por dirección
- **Min/Max Packet Length**: Longitudes mínima y máxima de paquetes
- **Packet Length Mean/Std**: Estadísticas generales de longitud
- **Fwd/Bwd Packets/s**: Paquetes por segundo por dirección
- **Label**: Etiqueta de clasificación

## 🏗️ Arquitectura

### Componentes Principales

1. **NetworkFlowStats**: Clase de datos que representa las estadísticas de flujo de red
2. **NetworkMonitor**: Monitor de tráfico de red con intervalos de 10 segundos
3. **NetworkMonitoringService**: Servicio en segundo plano para monitoreo continuo
4. **MainActivity**: Interfaz de usuario principal con diseño moderno
5. **FlowAnalyzer**: Analizador avanzado para detección de patrones de malware

### Funcionalidades

- ✅ Monitoreo en tiempo real cada 10 segundos
- ✅ Cálculo de todas las características del dataset CICAndMal2017
- ✅ Detección de patrones de malware (Adware, Ransomware, Scareware, SMS Malware)
- ✅ Exportación de datos en formato CSV
- ✅ Interfaz de usuario moderna y responsiva
- ✅ Servicio en segundo plano con notificaciones
- ✅ Análisis de riesgo en tiempo real

## 📱 Uso de la Aplicación

### 1. Instalación
```bash
# Clonar el repositorio
git clone [url_del_repositorio]
cd amaru

# Abrir en Android Studio
# Compilar y ejecutar en dispositivo Android
```

### 2. Permisos Requeridos
La aplicación requiere los siguientes permisos:
- `INTERNET`: Para acceso a internet
- `ACCESS_NETWORK_STATE`: Para información del estado de red
- `ACCESS_WIFI_STATE`: Para información de WiFi
- `WRITE_EXTERNAL_STORAGE`: Para guardar archivos CSV
- `READ_EXTERNAL_STORAGE`: Para leer archivos

### 3. Interfaz de Usuario

#### Pantalla Principal
- **Estado del Monitoreo**: Muestra si el monitoreo está activo
- **Información de Red**: Tipo de conexión (WiFi, Cellular, etc.)
- **Botones de Control**:
  - "Start Monitoring": Inicia el monitoreo
  - "Stop Monitoring": Detiene el monitoreo
  - "Export Data": Exporta datos recopilados
- **Estadísticas en Vivo**: Lista de flujos capturados recientemente

#### Notificaciones
- Notificación persistente durante el monitoreo
- Acciones rápidas: "Stop" y "Export"
- Contador de flujos capturados

### 4. Exportación de Datos

Los datos se exportan en formato CSV compatible con el dataset CICAndMal2017:

```csv
Total Fwd Packets,Total Backward Packets,Total Length of Fwd Packets,...,Label
10,15,2048,3072,512,204.8,45.2,1024,64,341.3,67.8,204.8,15.0,...,WiFi
```

## 🔧 Implementación Técnica

### Limitaciones de Android

Android no permite captura directa de paquetes sin permisos root, por lo que la implementación utiliza:

1. **TrafficStats API**: Para estadísticas de red del sistema
2. **Simulación de Paquetes**: Genera distribuciones realistas basadas en estadísticas
3. **Análisis de Patrones**: Detecta comportamientos sospechosos

### Algoritmos de Detección

#### 1. Adware
- Alta tasa de paquetes hacia adelante
- Intervalos regulares (solicitudes periódicas de anuncios)
- Paquetes pequeños (contenido de anuncios)

#### 2. Ransomware
- Actividad en ráfagas (cifrado rápido de archivos)
- Paquetes grandes (chunks de archivos cifrados)
- Flujo simétrico (comunicación C&C)

#### 3. Scareware
- Beacons periódicos (alertas falsas)
- Consultas tipo DNS (escaneo falso)
- Flujos cortos (interacciones rápidas)

#### 4. SMS Malware
- Paquetes pequeños y frecuentes (envío de SMS)
- Tráfico saliente pesado
- Intervalos cortos (envío rápido de SMS)

## 📊 Salida de Datos

### Formato CSV
```csv
Total Fwd Packets,Total Backward Packets,...,Label
10,15,2048,3072,512,204.8,45.2,...,WiFi
```

### Análisis de Riesgo
```kotlin
data class MalwareAnalysisResult(
    var suspiciousActivity: Boolean = false,
    var adwareScore: Double = 0.0,
    var ransomwareScore: Double = 0.0,
    var scarewareScore: Double = 0.0,
    var smsmalwareScore: Double = 0.0,
    var riskLevel: RiskLevel = RiskLevel.SAFE,
    var recommendations: List<String> = emptyList()
)
```

## 🎯 Casos de Uso

### 1. Investigación de Seguridad
- Recopilación de datos de tráfico de red
- Análisis de comportamientos de aplicaciones
- Detección de anomalías en tiempo real

### 2. Educación en Ciberseguridad
- Demostración de técnicas de monitoreo
- Análisis de patrones de malware
- Comprensión de métricas de red

### 3. Análisis de Rendimiento
- Monitoreo de uso de red
- Identificación de aplicaciones que consumen ancho de banda
- Optimización de rendimiento

## 🔗 Referencias

- [Dataset CICAndMal2017](https://www.unb.ca/cic/datasets/andmal2017.html)
- Paper: "Toward Developing a Systematic Approach to Generate Benchmark Android Malware Datasets and Classification"
- Universidad de New Brunswick - Canadian Institute for Cybersecurity

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## 📜 Licencia

Este proyecto está bajo la licencia MIT. Ver `LICENSE` para más detalles.

## ⚠️ Advertencias

- Esta aplicación es para propósitos educativos e investigación
- No debe usarse para actividades maliciosas
- Respeta las políticas de privacidad y términos de uso
- Algunos patrones de detección pueden generar falsos positivos

## 📧 Contacto

Si tienes preguntas o sugerencias, no dudes en contactar al desarrollador.

---

**Desarrollado con ❤️ para la comunidad de ciberseguridad** 