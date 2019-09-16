package com.xfu.xffc.extractors

sealed class Process(val fileName: String) {
    val type: String = javaClass.simpleName

    class Firmware(fileName: String) : Process(fileName)
    class FirmwareLess(fileName: String) : Process(fileName)
    class NonArb(fileName: String) : Process(fileName)
    class Vendor(fileName: String) : Process(fileName)
}
