package com.netlens.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects a device shake using [Sensor.TYPE_ACCELEROMETER].
 * Call [register] / [unregister] from Activity.onResume / onPause.
 *
 * @param context          Any context (activity/application).
 * @param threshold        G-force above Earth gravity that counts as a shake. Default: 12f.
 * @param slopMs           Minimum milliseconds between two shake events. Default: 500ms.
 * @param onShake          Callback invoked on the main thread when a shake is detected.
 */
class ShakeDetector(
    context: Context,
    private val threshold: Float = 12f,
    private val slopMs: Long     = 500L,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeMs = 0L

    /** Start listening. Call from onResume. */
    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /** Stop listening. Call from onPause. */
    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val (x, y, z) = event.values
        val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (gForce > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeMs > slopMs) {
                lastShakeMs = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
