package com.xfu.xffc

import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.toList


open class BaseExtractor(process: Process) {
    val fileName: String = process.fileName
    val type: String = process.type
    val today: String = getDate()
    val host: String = InetAddress.getLocalHost().hostName
    val zipContent: MutableList<String> = mutableListOf()
    val codename: String = ""
    val romType: String = ""
    open val toExtract: List<String> = listOf()
    open var updaterLines: MutableList<String> = mutableListOf()

    fun initDirs() {
        // Initial cleanup and housekeeping
        println("Generating $type ZIP from $fileName")
        File("tmp").deleteRecursively()
        File("out").deleteRecursively()
        File("out/META-INF/com/google/android/").mkdirs()
        File("tmp/META-INF/com/google/android/").mkdirs()
    }

    fun getDate(): String {
        // Get today's date
        val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.now().format(timeFormat)
    }

    fun checkFirmware(): MutableList<String> {
        try {
            // open a zip file for reading
            ZipFile(fileName).use { zipFile ->
                // get an enumeration of the ZIP file entries
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry: ZipEntry = entries.nextElement()
                    // get the name of the entry
                    zipContent.add(entry.name)
                }
            }
        } catch (ioe: IOException) {
            println("$ioe")
        }
        val updaterBinaries: List<String> =
            listOf("META-INF/com/google/android/update-binary", "META-INF/com/google/android/updater-script")
        val isValidROM: Boolean = zipContent.any { it == updaterBinaries[0] || it == updaterBinaries[1] }
        return if (!isValidROM) {
            println("This zip isn't a valid ROM!")
            mutableListOf()
        } else {
            zipContent
        }
    }

    fun getFirmwareType(): String = if ("firmware-update" in zipContent.toString()) "qcom" else "mtk"

    fun extractFile(toExtract: List<String>) {
        File("tmp/firmware-update/").mkdirs()
        val zipFile: Path = Paths.get(fileName)
        FileSystems.newFileSystem(zipFile, null).use { fileSystem: FileSystem ->
            toExtract.forEach { file ->
                val outputFile: Path = Paths.get("tmp/$file")
                val fileToExtract: Path = fileSystem.getPath(file)
                Files.copy(fileToExtract, outputFile)
            }
        }
    }

    fun extractFirmware() {
        println("Unzipping MIUI..")
        extractFile(toExtract)
        Files.move(Paths.get("tmp/firmware-update/"), Paths.get("out/firmware-update/"))
        Files.move(
            Paths.get("tmp/META-INF/com/google/android/update-binary"),
            Paths.get("out/META-INF/com/google/android/update-binary"), StandardCopyOption.REPLACE_EXISTING
        )
    }

    fun fixupUpdaterScript(lines: List<String>): MutableList<String> {
        val updaterScript: MutableList<String> = mutableListOf()
        lines.forEach {
            when {
                it.contains("/firmware/image/sec.dat") ->
                    updaterScript.add(
                        it.replace(
                            "/firmware/image/sec.dat",
                            "/dev/block/bootdevice/by-name/sec"
                        )
                    )
                it.contains("/firmware/image/splash.img") ->
                    updaterScript.add(
                        it.replace(
                            "/firmware/image/splash.img",
                            "/dev/block/bootdevice/by-name/splash"
                        )
                    )
                else -> updaterScript.add(it)
            }
        }
        return updaterScript
    }

    fun getCodename(lines: String): String {
        var codename: String
        val regex = Regex("/([a-z]*):")
        codename = regex.findAll(lines).map { it.groupValues[1] }.joinToString()
        if (codename.isEmpty()) {
            val regexAlt = Regex("get_device_compatible\\(\"([a-z]*)")
            codename = regexAlt.findAll(lines).map { it.groupValues[1] }.joinToString()
        }
        return codename
    }

    fun generateUpdaterScript(): String {
        println("Generating updater-script..")
        updaterLines = fixupUpdaterScript(updaterLines)
        File("out/META-INF/com/google/android/updater-script").also {
            it.writeText("show_progress(0.200000, 10);\n\n")
            it.appendText(
                "# Generated by Xiaomi Flashable Firmware Creator\n# $today - $host\n\n"
            )
            it.appendText("ui_print(\"Flashing $fileName zip...\");\n")
            updaterLines.forEach { line -> it.appendText("$line\n") }
            it.appendText("\nshow_progress(0.100000, 2);\nset_progress(1.000000);\n")
        }
        return getCodename(updaterLines.toString())
    }

    fun makeZip() {
        println("Compressing...")
        val env: HashMap<String, String> = hashMapOf("create" to "true")
        // locate file system by using the syntax
        // defined in java.net.
        val zipLocation: Path = FileSystems.getDefault().getPath("out.zip").toAbsolutePath()
        val uri: URI = URI.create("jar:file:$zipLocation")
        // files paths corrections
        val files: List<String> = Files.walk(Paths.get("out/")).map { x -> x.toString() }.toList()
        val toCompress: MutableList<String> = mutableListOf()
        for (path: String in files) {
            if (path == "out") {
                continue
            }
            toCompress.add(path.split("out/")[1])
        }
        // create the zip file and add files
        FileSystems.newFileSystem(uri, env).use { zipFs ->
            for (item: String in toCompress) {
                val toAdd: Path = Paths.get("out/$item")
                val pathInZipfile = zipFs.getPath(item)
                if (Files.isDirectory(toAdd)) {
                    // create directory in zip file
                    Files.createDirectories(pathInZipfile)
                    continue
                }
                // copy a file into the zip file
                Files.copy(
                    toAdd, pathInZipfile, StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    fun renameOutput(codename: String) {
        when (type) {
            "Firmware" -> Files.move(
                Paths.get("out.zip"),
                Paths.get("fw_${codename}_$fileName"),
                StandardCopyOption.REPLACE_EXISTING
            )
            "FirmwareLess" -> Files.move(
                Paths.get("out.zip"),
                Paths.get("fw-less_${codename}_$fileName"),
                StandardCopyOption.REPLACE_EXISTING
            )
            "NonArb" -> Files.move(
                Paths.get("out.zip"),
                Paths.get("fw-non-arb_${codename}_$fileName"),
                StandardCopyOption.REPLACE_EXISTING
            )
            "Vendor" -> Files.move(
                Paths.get("out.zip"),
                Paths.get("fw-vendor_${codename}_$fileName"),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    fun cleanUp() {
        // final cleanup
        File("tmp").deleteRecursively()
        File("out").deleteRecursively()
    }
}