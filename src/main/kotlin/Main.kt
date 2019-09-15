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
import kotlin.system.exitProcess

fun initDirs() {
    // Initial cleanup and housekeeping
    File("tmp").deleteRecursively()
    File("out").deleteRecursively()
    File("out/META-INF/com/google/android/").mkdirs()
    File("tmp/META-INF/com/google/android/").mkdirs()
}

fun getDateAndHostName(): Pair<String, String> {
    // Sets today data and hostname
    val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val today: String = LocalDateTime.now().format(timeFormat)
    val host: String = InetAddress.getLocalHost().hostName
    return Pair(today, host)
}

fun cleanup() {
    // final cleanup
    File("tmp").deleteRecursively()
    File("out").deleteRecursively()
}

fun checkFirmware(filename: String): MutableList<String> {
    val zipContent: MutableList<String> = mutableListOf()
    try {
        // open a zip file for reading
        ZipFile(filename).use { zipFile ->
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
    if (!isValidROM) {
        println("This zip isn't a valid ROM!")
        exitProcess(2)
    } else {
        return zipContent
    }
}

fun prepareOut(zipContent: MutableList<String>) {
    for (item: String in zipContent) {
        if (item.endsWith('/')) {
            File("tmp/$item").mkdir()
        }
    }
}

fun getFirmwareType(zipContent: MutableList<String>): String =
    if ("firmware-update" in zipContent.toString()) "qcom" else "mtk"

fun extractFile(zipName: String, toExtract: List<String>) {
    File("tmp/firmware-update/").mkdirs()
    val zipFile: Path = Paths.get(zipName)
    FileSystems.newFileSystem(zipFile, null).use { fileSystem: FileSystem ->
        toExtract.forEach { file ->
            val outputFile: Path = Paths.get("tmp/$file")
            val fileToExtract: Path = fileSystem.getPath(file)
            Files.copy(fileToExtract, outputFile)
        }
    }
}

fun extractFirmware(filename: String, zipContent: MutableList<String>) {
    println("Unzipping MIUI..")
    val workFiles: List<String> = zipContent.filter {
        it.startsWith("firmware-update/") ||
                it.startsWith("META-INF/com/google/android")
    }
    val toExtract = workFiles.filterNot {
        it.contains("dtbo") ||
                it.contains("splash") ||
                it.contains("vbmeta")
    }
    extractFile(filename, toExtract)
    Files.move(Paths.get("tmp/firmware-update/"), Paths.get("out/firmware-update/"))
    Files.move(
        Paths.get("tmp/META-INF/com/google/android/update-binary"),
        Paths.get("out/META-INF/com/google/android/update-binary"), StandardCopyOption.REPLACE_EXISTING
    )
}

// TODO mtk_firmware_extract
// TODO rom_extract
// TODO vendor_extract

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

fun generateUpdaterScript(today: String, host: String): String {
    println("Generating updater-script..")
    var updaterLines: MutableList<String> = mutableListOf()
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
    updaterLines = fixupUpdaterScript(updaterLines)
    File("out/META-INF/com/google/android/updater-script").also {
        it.writeText("show_progress(0.200000, 10);\n\n")
        it.appendText(
            "# Generated by Xiaomi Flashable Firmware Creator\n# $today - $host\n\n"
        )
        it.appendText("ui_print(\"Flashing Normal firmware...\");\n")
        updaterLines.forEach { line -> it.appendText("$line\n") }
        it.appendText("\nshow_progress(0.100000, 2);\nset_progress(1.000000);\n")
    }
    return getCodename(updaterLines.toString())
}

// TODO mtk_firmware_updater
// TODO nonarb_updater
// TODO firmwareless_updater
// TODO vendor_updater
// TODO mtk_vendor_updater

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

fun renameOutput(process: Process, codename: String) {
    val filename = process.fileName
    when (process.type) {
        "Firmware" -> Files.move(
            Paths.get("out.zip"),
            Paths.get("fw_${codename}_$filename"),
            StandardCopyOption.REPLACE_EXISTING
        )
        "FirmwareLess" -> Files.move(
            Paths.get("out.zip"),
            Paths.get("fw-less_${codename}_$filename"),
            StandardCopyOption.REPLACE_EXISTING
        )
        "NonArb" -> Files.move(
            Paths.get("out.zip"),
            Paths.get("fw-non-arb_${codename}_$filename"),
            StandardCopyOption.REPLACE_EXISTING
        )
        "Vendor" -> Files.move(
            Paths.get("out.zip"),
            Paths.get("fw-vendor_${codename}_$filename"),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

// TODO main

fun controller(process: Process) {
    println("Generating ${process.type} ZIP from ${process.fileName}")
    initDirs()
    val (today: String, host: String) = getDateAndHostName()
    val zipContent: MutableList<String> = checkFirmware(process.fileName)
    val fwType: String = getFirmwareType(zipContent)
    println("$fwType ROM detected")
    prepareOut(zipContent)
    extractFirmware(process.fileName, zipContent)
    val codename = generateUpdaterScript(today, host)
    makeZip()
    renameOutput(process, codename)
    println("All Done!")
    cleanup()
}

fun main(args: Array<String>) = ArgParse().main(args)
