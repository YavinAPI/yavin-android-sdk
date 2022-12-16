package com.yavin.yavinandroidsdk.files.utils

import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object YavinFilesUtils {

    fun compressFile(inputFile: File, outputFile: File) {
        val os = outputFile.outputStream()
        val gos = GZIPOutputStream(os)
        gos.write(inputFile.readBytes())
        gos.close()
        os.close()
    }

    fun uncompressFile(inputFile: File, outputFile: File) {
        val bytes = GZIPInputStream(inputFile.inputStream()).use { it.readBytes() }
        outputFile.writeBytes(bytes)
    }
}