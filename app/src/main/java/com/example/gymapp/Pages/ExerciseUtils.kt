package com.example.gymapp.Pages

import kotlin.math.*

fun calculateAngle(
    x1: Float, y1: Float, // First point
    x2: Float, y2: Float, // Middle point (vertex)
    x3: Float, y3: Float  // End point
): Double {
    val baX = x1 - x2
    val baY = y1 - y2
    val bcX = x3 - x2
    val bcY = y3 - y2
    val dotProduct = baX * bcX + baY * bcY
    val magBA = sqrt(baX * baX + baY * baY)
    val magBC = sqrt(bcX * bcX + bcY * bcY)
    val cosAngle = dotProduct / (magBA * magBC)
    val clampedCosAngle = cosAngle.coerceIn(-1.0f, 1.0f)
    return Math.toDegrees(acos(clampedCosAngle.toDouble()))
}

