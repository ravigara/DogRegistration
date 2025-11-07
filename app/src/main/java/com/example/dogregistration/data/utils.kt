package com.example.dogregistration.data
import kotlin.math.sqrt
fun euclideanDistance(emb1: FloatArray, emb2: FloatArray): Float {
    if (emb1.size != emb2.size) return Float.MAX_VALUE

    var sum = 0.0f
    for (i in emb1.indices) {
        val diff = emb1[i] - emb2[i]
        sum += diff * diff
    }
    // We use sqrt to get the true, human-readable distance.
    return sqrt(sum)
}