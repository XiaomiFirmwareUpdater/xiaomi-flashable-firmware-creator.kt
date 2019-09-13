import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option

sealed class Processes {
    data class Firmware(val filename: String) : Processes()
    data class FirmwareLess(val filename: String) : Processes()
    data class NonArb(val filename: String) : Processes()
    data class Vendor(val filename: String) : Processes()
}

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
    private val process: Processes? by mutuallyExclusiveOptions<Processes>(
        option("-F", "--firmware", help = "Create normal Firmware zip", metavar = "rom")
            .convert { Processes.Firmware(it) },
        option("-N", "--nonarb", help = "Create non-ARB Firmware zip", metavar = "rom")
            .convert { Processes.NonArb(it) },
        option("-L", "--firmwareless", help = "Create Firmware-less zip", metavar = "rom")
            .convert { Processes.FirmwareLess(it) },
        option("-V", "--vendor", help = "Create Firmware+Vendor zip", metavar = "rom")
            .convert { Processes.Vendor(it) }
    ).single().required()

    override fun run() = controller(process.toString())
}
