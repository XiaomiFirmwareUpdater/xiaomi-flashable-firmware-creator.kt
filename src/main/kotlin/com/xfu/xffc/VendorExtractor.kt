package com.xfu.xffc

import java.io.File

class VendorExtractor(process: Process) : BaseExtractor(process) {

    override val toExtract: List<String> by lazy { vendorFilter() }
    override val updaterLines: MutableList<String> by lazy { vendorUpdaterScript() }

    init {
        File("tmp/firmware-update/").mkdirs()
        File("tmp/META-INF/com/android/").mkdirs()
    }

    private fun vendorFilter(): List<String> {
        return zipContent.filterNot {
            it.startsWith("system") || it.startsWith("boot.img") || it.endsWith('/')
        }
    }

    private fun vendorUpdaterScript(): MutableList<String> {
        val updaterScriptLines: MutableList<String> = mutableListOf()
        File("tmp/META-INF/com/google/android/updater-script").useLines { lines ->
            if (romType == "qcom") {
                lines.forEach {
                    if (it.contains("getprop") || it.contains("Target") ||
                        it.contains("firmware-update") || it.contains("vendor")
                    ) {
                        updaterScriptLines.add(it)
                    }
                }
            } else {
                lines.forEach {
                    if (!it.contains("system") || !it.contains("boot.img")
                    ) {
                        updaterScriptLines.add(it)
                    }
                }
            }
        }
        return updaterScriptLines
    }
}
