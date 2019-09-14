import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option

sealed class Process(val fileName: String) {
    val type: String = javaClass.simpleName

    class Firmware(fileName: String) : Process(fileName)
    class FirmwareLess(fileName: String) : Process(fileName)
    class NonArb(fileName: String) : Process(fileName)
    class Vendor(fileName: String) : Process(fileName)
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
    private val process: Process by mutuallyExclusiveOptions<Process>(
        option("-F", "--firmware", help = "Create normal Firmware zip", metavar = "rom")
            .convert { Process.Firmware(it) },
        option("-N", "--nonarb", help = "Create non-ARB Firmware zip", metavar = "rom")
            .convert { Process.NonArb(it) },
        option("-L", "--firmwareless", help = "Create Firmware-less zip", metavar = "rom")
            .convert { Process.FirmwareLess(it) },
        option("-V", "--vendor", help = "Create Firmware+Vendor zip", metavar = "rom")
            .convert { Process.Vendor(it) }
    ).single().required()

    override fun run() = controller(process)
}
