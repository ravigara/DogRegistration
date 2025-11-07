package com.example.dogregistration.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Uses gyroscope to check if the device is stable while capturing.
 */
class StabilizationMonitor(context: Context, val onStable: (Boolean) -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun start() { sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME) }
    fun stop() { sm.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        val stable = event?.values?.all { kotlin.math.abs(it) < 0.15f } ?: false
        onStable(stable)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}