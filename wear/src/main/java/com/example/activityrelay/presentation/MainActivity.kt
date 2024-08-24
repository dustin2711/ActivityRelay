/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.activityrelay.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.activityrelay.presentation.theme.ActivityRelayTheme

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var rotationVector: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) // TYPE_ROTATION_VECTOR, TYPE_GEOMAGNETIC_ROTATION_VECTOR


        setContent {
            var straightAcceleration by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
            var maximumStraightAcceleration by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }

            var spinAcceleration by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
            var minimumSpinAcceleration by remember { mutableStateOf(Triple(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)) }
            var maximumSpinAcceleration by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }

            var rotation by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }
            var spinVelocity by remember { mutableStateOf(0f) }
            var maxSpinVelocity by remember { mutableStateOf(0f) }

            var previousRotationVectorTimestamp = 0L

            // Use DisposableEffect to handle sensor registration and un-registration
            DisposableEffect(Unit)
            {
                val listener = object : SensorEventListener
                {
                    override fun onSensorChanged(event: SensorEvent?)
                    {
                        event?.let {
                            when (it.sensor.type)
                            {
                                Sensor.TYPE_ACCELEROMETER ->
                                {
                                    straightAcceleration = Triple(it.values[0], it.values[1], it.values[2])
                                    maximumStraightAcceleration = VectorUtils.max(
                                        VectorUtils.abs(straightAcceleration),
                                        maximumStraightAcceleration)
                                }
                                Sensor.TYPE_GYROSCOPE ->
                                {
                                    spinAcceleration = Triple(it.values[0], it.values[1], it.values[2])
                                    minimumSpinAcceleration = VectorUtils.min(spinAcceleration, minimumSpinAcceleration)
                                    maximumSpinAcceleration = VectorUtils.max(spinAcceleration, maximumSpinAcceleration)
                                }
                                Sensor.TYPE_ROTATION_VECTOR ->
                                {
                                    // Convert nano seconds to seconds
                                    val deltaTime = ( event.timestamp - previousRotationVectorTimestamp) * 1e-9f

                                    // values[2] has the compass rotation
                                    spinVelocity = (rotation.third - it.values[2]) / deltaTime
                                    rotation = Triple(it.values[0], it.values[1], it.values[2])

                                    maxSpinVelocity = maxOf(maxSpinVelocity, spinVelocity)

                                    previousRotationVectorTimestamp = event.timestamp
                                }
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_NORMAL)

                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }

            ShowSensorData(
                straightAcceleration,
                maximumStraightAcceleration,
                spinAcceleration,
                minimumSpinAcceleration,
                maximumSpinAcceleration,
                rotation,
                spinVelocity,
                maxSpinVelocity,
            )
        }
    }
}

// Composable functions outside of class are for UI logic
@Composable
fun ShowSensorData(
    straightAcceleration: Triple<Float, Float, Float>,
    maximumStraightAcceleration: Triple<Float, Float, Float>,
    spinAcceleration: Triple<Float, Float, Float>,
    minimumSpinAcceleration: Triple<Float, Float, Float>,
    maximumSpinAcceleration: Triple<Float, Float, Float>,
    rotation: Triple<Float, Float, Float>,
    spinVelocity: Float,
    maxSpinVelocity: Float,
)
{
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = ("\n\n\n"
                + "Rotation: %.2f | %.2f | %.2f".format(rotation.first, rotation.second, rotation.third) + "\n\n"
                + "Linear: %.2f | %.2f | %.2f".format(straightAcceleration.first, straightAcceleration.second, straightAcceleration.third) + "\n\n"
                + "Spin accel: %.2f | %.2f | %.2f".format(spinAcceleration.first, spinAcceleration.second, spinAcceleration.third) + "\n"
                + "Max spin accel: %.2f".format(maximumSpinAcceleration.third) + "\n"
                + "Spin velo: %.2f".format(spinVelocity) + "\n"
                + "Spin velo (max): %.2f".format(maxSpinVelocity) + "\n"
                ))
}

@Composable
fun WearApp(greetingName: String)
{
    ActivityRelayTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            ShowSensorData(
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                0f,
                0f,
            )
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview()
{
    WearApp("Preview Android")
}