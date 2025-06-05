package alex.kaplenkov.safetyalert.domain.detector

interface DetectorWithHeatmap {
    fun getLastHeatmap(): Array<Array<FloatArray>>?
    fun getLastHeatmapFlattened(): FloatArray?
}