/**
 * Adapted from: https://github.com/dzolnai/ExoVisualizer
 *
 * MIT License
 *
 * Copyright (c) 2019 Dániel Zolnai
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.livekit.android.example.voiceassistant.ui.noise.FFTAudioProcessor
import io.livekit.android.util.LKLog
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow

// Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
private val FREQUENCY_BAND_LIMITS = arrayOf(
    20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
    800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
    12500, 16000, 20000
)

private val BANDS = FREQUENCY_BAND_LIMITS.size
private val SIZE = FFTAudioProcessor.SAMPLE_SIZE / 2
private val maxConst = 25_000 // Reference max value for accum magnitude

@Composable
fun BarVisualizer(fft: FloatArray, modifier: Modifier = Modifier) {

    Surface(modifier = modifier) {
        Box(modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF9E82F0),
                        Color(0xFF42A5F5)
                    )
                )
                onDrawWithContent {

                    LKLog.e{"drawing"}

                    if (fft.isEmpty()) {
                        return@onDrawWithContent
                    }

                    var currentFftPosition = 0
                    var currentFrequencyBandLimitIndex = 0
                    var currentAverage = 0f

                    // We average out the values over 3 occurences (plus the current one), so big jumps are smoothed out
                    val smoothingFactor = 3
                    val previousValues = FloatArray(BANDS * smoothingFactor)
                    // Iterate over the entire FFT result array
                    while (currentFftPosition < SIZE) {
                        var accum = 0f

                        // We divide the bands by frequency.
                        // Check until which index we need to stop for the current band
                        val nextLimitAtPosition =
                            floor(FREQUENCY_BAND_LIMITS[currentFrequencyBandLimitIndex] / 20_000.toFloat() * SIZE).toInt()

                        synchronized(fft) {
                            // Here we iterate within this single band
                            for (j in 0 until (nextLimitAtPosition - currentFftPosition) step 2) {
                                // Convert real and imaginary part to get energy
                                val raw = (fft[currentFftPosition + j]
                                    .toDouble()
                                    .pow(2.0) +
                                        fft[currentFftPosition + j + 1]
                                            .toDouble()
                                            .pow(2.0)).toFloat()

                                // Hamming window (by frequency band instead of frequency, otherwise it would prefer 10kHz, which is too high)
                                // The window mutes down the very high and the very low frequencies, usually not hearable by the human ear
                                val m = BANDS / 2
                                val windowed = raw * (0.54f - 0.46f * cos(2 * Math.PI * currentFrequencyBandLimitIndex / (m + 1))).toFloat()
                                accum += windowed
                            }
                        }

                        // A window might be empty which would result in a 0 division
                        if (nextLimitAtPosition - currentFftPosition != 0) {
                            accum /= (nextLimitAtPosition - currentFftPosition)
                        } else {
                            accum = 0.0f
                        }
                        currentFftPosition = nextLimitAtPosition

                        // Here we do the smoothing
                        // If you increase the smoothing factor, the high shoots will be toned down, but the
                        // 'movement' in general will decrease too
                        var smoothedAccum = accum
                        for (i in 0 until smoothingFactor) {
                            smoothedAccum += previousValues[i * BANDS + currentFrequencyBandLimitIndex]
                            if (i != smoothingFactor - 1) {
                                previousValues[i * BANDS + currentFrequencyBandLimitIndex] =
                                    previousValues[(i + 1) * BANDS + currentFrequencyBandLimitIndex]
                            } else {
                                previousValues[i * BANDS + currentFrequencyBandLimitIndex] = accum
                            }
                        }
                        smoothedAccum /= (smoothingFactor + 1) // +1 because it also includes the current value

                        // We display the average amplitude with a vertical line
                        currentAverage += smoothedAccum / BANDS


                        val leftX = size.width * (currentFrequencyBandLimitIndex / BANDS.toFloat())
                        val rightX = leftX + size.width / BANDS.toFloat()

                        val barHeight =
                            (size.height * (smoothedAccum / maxConst.toDouble())
                                .coerceAtMost(1.0)
                                .toFloat())
                        val top = size.height - barHeight

                        drawRect(
                            brush = brush,
                            topLeft = Offset(leftX, top),
                            size = Size(rightX - leftX, barHeight),
                        )


                        currentFrequencyBandLimitIndex++
                    }

                    LKLog.e{"drawing end"}
                }
            })
    }
}