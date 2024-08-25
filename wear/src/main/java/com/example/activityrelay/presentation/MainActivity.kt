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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Button
import com.example.activityrelay.R
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
            var previousRotationVectorTimestamp by remember { mutableStateOf(0L) }


            // Function to reset all values to their defaults
            val resetValues = {
                straightAcceleration = Triple(0f, 0f, 0f)
                maximumStraightAcceleration = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                spinAcceleration = Triple(0f, 0f, 0f)
                minimumSpinAcceleration = Triple(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
                maximumSpinAcceleration = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                spinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
                minSpinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
                maxSpinVelocity = Triple(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
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
                                    val deltaTime =
                                        (event.timestamp - previousRotationVectorTimestamp) * 1e-9f

                                    // Convert to radian
                                    val rotation = Triple(
                                        convertToRadian(it.values[0]),
                                        convertToRadian(it.values[1]),
                                        convertToRadian(it.values[2]),
                                    )

                                    // Calculate spin velocity for each axis
                                    spinVelocity = Triple(
                                        (getAngleDifference(previousRotation.first, rotation.first)) / deltaTime,
                                        (getAngleDifference(previousRotation.second, rotation.second)) / deltaTime,
                                        (getAngleDifference(previousRotation.third, rotation.third)) / deltaTime
                                    )
                                    // Update the max and min spin velocities
                                    minSpinVelocity =
                                        VectorUtils.min(minSpinVelocity, spinVelocity)
                                    maxSpinVelocity =
                                        VectorUtils.max(maxSpinVelocity, spinVelocity)

                                    // Update the timestamp
                                    previousRotationVectorTimestamp = event.timestamp
                                    previousRotation = rotation
                                }
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
                sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
                sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)

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

const val TAU: Float = 2 * kotlin.math.PI.toFloat()
const val PI: Float = kotlin.math.PI.toFloat()

val textFont = FontFamily(
    Font(R.font.cascadia_mono)
)

fun convertToRadian(value: Float): Float
{
    return PI + PI * value
}

fun getAngleDifference(radianStart: Float, radianEnd: Float): Float {
    val difference = radianEnd - radianStart
    val normalizedDifference = (difference + PI) % TAU - PI
    return if (normalizedDifference < -PI) {
        normalizedDifference + TAU
    } else {
        normalizedDifference
    }
}

fun formatWithSign(value: Float): String {
    return if (value >= 0) {
        "+%.2f".format(value)
    } else {
        "%.2f".format(value)
    }
}

fun formatWithoutSign(value: Float): String {
    return if (value >= 0) {
        "%.2f".format(value)
    } else {
        "%.2f".format(value)
    }
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
            fontFamily = textFont,
            text = (
                "\n\n\n\n" +
//                "Linear: %s|%s|%s".format(
//                    formatWithSign(straightAcceleration.first),
//                    formatWithSign(straightAcceleration.second),
//                    formatWithSign(straightAcceleration.third)
//                ) + "\n" +
//                "Angular: %s|%s|%s".format(
//                    formatWithSign(spinAcceleration.first),
//                    formatWithSign(spinAcceleration.second),
//                    formatWithSign(spinAcceleration.third)
//                ) + "\n" +
//                "Rotation: %s|%s|%s".format(
//                    formatWithSign(rotation.first),
//                    formatWithSign(rotation.second),
//                    formatWithSign(rotation.third)
//                ) + "\n" +
                "Spin: %s".format(formatWithSign(spinVelocity.third / TAU)) + " / s\n" +
                "Max Spin ↺: %s".format(formatWithoutSign(maxSpinVelocity.third / TAU)) + " / s\n" +
                "Max Spin ↻: %s".format(formatWithoutSign(-minSpinVelocity.third / TAU)) + " / s\n"
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
fun WearApp()
{
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
fun DefaultPreview()
{
    WearApp()
}
