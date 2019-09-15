package com.xfu.xffc

import java.io.File

class FirmwareExtractor(process: Process) : BaseExtractor(process) {

    override val toExtract: List<String> = firmwareFilter()
    override var updaterLines: MutableList<String> = firmwareUpdaterScript()

    fun firmwareFilter(): List<String> {
        return zipContent.filter {
            it.startsWith("firmware-update/") ||
                    it.startsWith("META-INF/com/google/android")
        }.filterNot {
            it.contains("dtbo") ||
                    it.contains("splash") ||
                    it.contains("vbmeta")
        }
    }

    fun firmwareUpdaterScript(): MutableList<String> {
        val updaterLines: MutableList<String> = mutableListOf()
        File("tmp/META-INF/com/google/android/updater-script").useLines { lines ->
            lines.forEach {
                if (it.contains("getprop") ||
                    it.contains("Target") ||
                    it.contains("firmware-update")
                ) {
                    updaterLines.add(it)
                }
            }
        }
        return updaterLines
    }
}