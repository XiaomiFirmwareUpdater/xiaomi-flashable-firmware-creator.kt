package com.xfu.xffc.extractors

import java.io.File

class FirmwareExtractor(process: Process) : BaseExtractor(process) {

    override val toExtract: List<String> by lazy { firmwareFilter() }
    override val updaterLines: MutableList<String> by lazy { firmwareUpdaterScript() }

    private fun firmwareFilter(): List<String> {
        return if (romType == "qcom") {
            File("tmp/firmware-update/").mkdirs()
            zipContent.filter {
                it.startsWith("firmware-update/") || it.startsWith("META-INF/com/google/android")
            }.filterNot {
                it.contains("dtbo") || it.contains("splash") ||
                        it.contains("vbmeta")
            }
        } else {
            zipContent.filterNot {
                it.contains("system") || it.contains("vendor") ||
                        it.contains("boot.img") || it.contains("file_contexts") || it.endsWith('/')
            }
        }
    }

    private fun firmwareUpdaterScript(): MutableList<String> {
        val updaterScriptLines: MutableList<String> = mutableListOf()
        File("tmp/META-INF/com/google/android/updater-script").useLines { lines ->
            if (romType == "qcom") {
                lines.forEach {
                    if (it.contains("getprop") || it.contains("Target") ||
                        it.contains("firmware-update") && !it.contains("dtbo.img") &&
                        !it.contains("vbmeta.img") && !it.contains("splash")
                    ) {
                        updaterScriptLines.add(it)
                    }
                }
            } else {
                lines.forEach {
                    if (!it.contains("system") || !it.contains("vendor") ||
                        !it.contains("boot.img")
                    ) {
                        updaterScriptLines.add(it)
                    }
                }
            }
        }
        return updaterScriptLines
    }
}
