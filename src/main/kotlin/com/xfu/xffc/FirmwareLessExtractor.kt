package com.xfu.xffc

import java.io.File

class FirmwareLessExtractor(process: Process) : BaseExtractor(process) {

    override val toExtract: List<String> by lazy { firmwareLessFilter() }
    override val updaterLines: MutableList<String> by lazy { firmwareLessUpdaterScript() }

    init {
        File("tmp/META-INF/com/android/").mkdirs()
    }

    private fun firmwareLessFilter(): List<String> {
        return zipContent.filterNot {
            it.startsWith("firmware-update/")
        }
    }

    private fun firmwareLessUpdaterScript(): MutableList<String> {
        val updaterScriptLines: MutableList<String> = mutableListOf()
        File("tmp/META-INF/com/google/android/updater-script").useLines { lines ->
            lines.forEach {
                if (it.contains("getprop") || it.contains("Target") ||
                    it.contains("boot.img") || it.contains("system") || it.contains("vendor")
                ) {
                    updaterScriptLines.add(it)
                }
            }
        }
        return updaterScriptLines
    }
}
