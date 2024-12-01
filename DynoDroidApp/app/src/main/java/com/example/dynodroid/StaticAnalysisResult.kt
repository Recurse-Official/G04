package com.example.dynodroid

data class StaticAnalysisResult(
    val malwarePrediction: String,
    val permissionCount: Int,
    val activityCount: Int,
    val serviceCount: Int,
    val receiverCount: Int,
    val providerCount: Int,
    val featureCount: Int,
    val rawFeatures: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StaticAnalysisResult

        if (malwarePrediction != other.malwarePrediction) return false
        if (permissionCount != other.permissionCount) return false
        if (activityCount != other.activityCount) return false
        if (serviceCount != other.serviceCount) return false
        if (receiverCount != other.receiverCount) return false
        if (providerCount != other.providerCount) return false
        if (featureCount != other.featureCount) return false
        return rawFeatures.contentEquals(other.rawFeatures)
    }

    override fun hashCode(): Int {
        var result = malwarePrediction.hashCode()
        result = 31 * result + permissionCount
        result = 31 * result + activityCount
        result = 31 * result + serviceCount
        result = 31 * result + receiverCount
        result = 31 * result + providerCount
        result = 31 * result + featureCount
        result = 31 * result + rawFeatures.contentHashCode()
        return result
    }
}