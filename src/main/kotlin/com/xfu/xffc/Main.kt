package com.xfu.xffc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.xfu.xffc.extractors.FirmwareExtractor
import com.xfu.xffc.extractors.FirmwareLessExtractor
import com.xfu.xffc.extractors.NonArbExtractor
import com.xfu.xffc.extractors.Process
import com.xfu.xffc.extractors.VendorExtractor

class ArgParse : CliktCommand(
    name = "xffc",
    help = """
    Xiaomi Flashable Firmware Creator (Kotlin version)
    A tool that generates flashable firmware-update packages
    from official (or non official) MIUI ROMS.
    It supports creating untouched firmware, non-arb firmware,
    firmware + vendor flashable zip, and firmware-less ROMs.
    """
) {
    private val process: Process by mutuallyExclusiveOptions<Process>(
        option("-F", "--firmware", help = "Create normal Firmware zip", metavar = "ROM")
            .convert { Process.Firmware(it) },
        option("-N", "--nonarb", help = "Create non-ARB Firmware zip", metavar = "ROM")
            .convert { Process.NonArb(it) },
        option("-L", "--firmwareless", help = "Create Firmware-less zip", metavar = "ROM")
            .convert { Process.FirmwareLess(it) },
        option("-V", "--vendor", help = "Create Firmware+Vendor zip", metavar = "ROM")
            .convert { Process.Vendor(it) }
    ).single().required()

    override fun run() {
        when (process) {
            is Process.Firmware -> {
                val firmware = FirmwareExtractor(process)
                firmware.extract()
            }
            is Process.NonArb -> {
                val nonArb = NonArbExtractor(process)
                nonArb.extract()
            }
            is Process.FirmwareLess -> {
                val firmwareLess = FirmwareLessExtractor(process)
                firmwareLess.extract()
            }
            is Process.Vendor -> {
                val vendor = VendorExtractor(process)
                vendor.extract()
            }
        }
    }
}

fun main(args: Array<String>) = ArgParse().main(args)
