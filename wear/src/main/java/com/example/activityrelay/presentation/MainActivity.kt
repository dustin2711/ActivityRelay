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
import androidx.wear.compose.material.Button
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
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            var straightAcceleration by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
            var maximumStraightAcceleration by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }

            var spinAcceleration by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
            var minimumSpinAcceleration by remember { mutableStateOf(Triple(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)) }
            var maximumSpinAcceleration by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }

            var spinVelocity by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }
            var minSpinVelocity by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }
            var maxSpinVelocity by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }

            var previousRotation by remember { mutableStateOf(Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)) }
            var previousRotationVectorTimestamp = 0L

            // Function to reset all values to their defaults
            val resetValues = {
                straightAcceleration = Triple(0f, 0f, 0f)
                maximumStraightAcceleration = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                spinAcceleration = Triple(0f, 0f, 0f)
                minimumSpinAcceleration = Triple(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
                maximumSpinAcceleration = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                previousRotation = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
                spinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
                minSpinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
                maxSpinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                previousRotation =
                    previousRotationVectorTimestamp=
            }

            // Use DisposableEffect to handle sensor registration and un-registration
            DisposableEffect(Unit) {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event?.let {
                            when (it.sensor.type) {
                                Sensor.TYPE_ACCELEROMETER ->
                                {
                                    straightAcceleration = Triple(it.values[0], it.values[1], it.values[2])
                                    maximumStraightAcceleration = VectorUtils.max(
                                        VectorUtils.abs(straightAcceleration),
                                        maximumStraightAcceleration
                                    )
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
                                    val deltaTime = (event.timestamp - previousRotationVectorTimestamp) * 1e-9f

                                    // Process rotation vector for each axis
                                    val rotation = Triple(
                                        processRotationAxis(previousRotation.first, it.values[0]),
                                        processRotationAxis(previousRotation.second, it.values[1]),
                                        processRotationAxis(previousRotation.third, it.values[2])
                                    )

                                    // Calculate spin velocity for each axis
                                    spinVelocity = Triple(
                                        (previousRotation.first - rotation.first) / deltaTime,
                                        (previousRotation.second - rotation.second) / deltaTime,
                                        (previousRotation.third - rotation.third) / deltaTime
                                    )
                                    // Update the max and min spin velocities
                                    minSpinVelocity = VectorUtils.min(minSpinVelocity, spinVelocity)
                                    maxSpinVelocity = VectorUtils.max(maxSpinVelocity, spinVelocity)

                                    // Update the current rotation values

                                    // Update the timestamp
                                    previousRotationVectorTimestamp = event.timestamp
                                    previousRotation = rotation
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
                previousRotation,
                spinVelocity,
                minSpinVelocity,
                maxSpinVelocity,
                onButtonClick = resetValues // Pass the reset function to the button
            )
        }
    }
}

fun processRotationAxis(previousValue: Float, currentValue: Float): Float {
    var newValue = currentValue
    var oldValue = previousValue

    // Handle wraparound between 1 and -1
    if (oldValue > 0.5f && newValue < -0.5f) {
        newValue += 2f
    } else if (oldValue < -0.5f && newValue > 0.5f) {
        oldValue += 2f
    }

    // Normalize the value back to the range [-1, 1]
    if (newValue > 1f) {
        newValue -= 2f
    }

    return newValue
}

@Composable
fun ShowSensorData(
    straightAcceleration: Triple<Float, Float, Float>,
    maximumStraightAcceleration: Triple<Float, Float, Float>,
    spinAcceleration: Triple<Float, Float, Float>,
    minimumSpinAcceleration: Triple<Float, Float, Float>,
    maximumSpinAcceleration: Triple<Float, Float, Float>,
    rotation: Triple<Float, Float, Float>,
    spinVelocity: Triple<Float, Float, Float>,
    minSpinVelocity: Triple<Float, Float, Float>,
    maxSpinVelocity: Triple<Float, Float, Float>,
    onButtonClick: () -> Unit // Add this parameter for button click handling
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = ("\n\n\n"
                    + "Linear accel: %.2f | %.2f | %.2f".format(straightAcceleration.first, straightAcceleration.second, straightAcceleration.third) + "\n"
                    + "Spin accel: %.2f | %.2f | %.2f".format(spinAcceleration.first, spinAcceleration.second, spinAcceleration.third) + "\n"
                    + "Rotation: %.2f | %.2f | %.2f".format(rotation.first, rotation.second, rotation.third) + "\n\n"
                    + "Spin velo: %.2f".format(spinVelocity.third) + "\n"
                    + "Spin velo (clockwise): %.2f".format(maxSpinVelocity.third) + "\n"
                    + "Spin velo (counter \"): %.2f".format(minSpinVelocity.third) + "\n"
                    )
        )

        Button(
            onClick = { onButtonClick() }, // Trigger the passed lambda function
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Reset Values")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    ActivityRelayTheme {
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
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                Triple(0f, 0f, 0f),
                onButtonClick = {} // Placeholder for preview
            )
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}
