package com.xfu.xffc

import java.io.File

class NonArbExtractor (process: Process) : BaseExtractor(process) {

    override val toExtract: List<String> by lazy { nonArbFilter() }
    override val updaterLines: MutableList<String> by lazy { nonArbUpdaterScript() }

    init {
        File("tmp/firmware-update/").mkdirs()
    }

    private fun nonArbFilter(): List<String> {
        return zipContent.filter {
            it.startsWith("firmware-update/dspso.bin") ||
                    it.startsWith("firmware-update/BTFM.bin") ||
                    it.startsWith("firmware-update/NON-HLOS.bin") ||
                    it.startsWith("META-INF/com/google/android")
        }
    }

    private fun nonArbUpdaterScript(): MutableList<String> {
        val updaterScriptLines: MutableList<String> = mutableListOf()
        File("tmp/META-INF/com/google/android/updater-script").useLines { lines ->
            lines.forEach {
                if (it.contains("getprop") || it.contains("Target") ||
                    it.contains("modem") || it.contains("bluetooth") || it.contains("dsp")
                ) {
                    updaterScriptLines.add(it)
                }
            }
        }
        return updaterScriptLines
    }
}
