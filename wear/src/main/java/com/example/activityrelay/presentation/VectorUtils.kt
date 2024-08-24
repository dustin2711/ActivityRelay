package com.example.activityrelay.presentation

object VectorUtils
{
    fun min(
        a: Triple<Float, Float, Float>,
        b: Triple<Float, Float, Float>)
            : Triple<Float, Float, Float> {
        return Triple(
            minOf(a.first, b.first),
            minOf(a.second, b.second),
            minOf(a.third, b.third)
        )
    }

    fun max(
        a: Triple<Float, Float, Float>,
        b: Triple<Float, Float, Float>)
            : Triple<Float, Float, Float> {
        return Triple(
            maxOf(a.first, b.first),
            maxOf(a.second, b.second),
            maxOf(a.third, b.third)
        )
    }

    fun abs(
        a: Triple<Float, Float, Float>)
            : Triple<Float, Float, Float> {
        return Triple(
            kotlin.math.abs(a.first),
            kotlin.math.abs(a.second),
            kotlin.math.abs(a.third))
    }
}