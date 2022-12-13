package com.yavin.yavinandroidsdk.files

import android.content.Context
import java.io.File

interface YavinFilesManager {

    fun init()

    fun getFileFromDirectory(context: Context, directoryName: String, fileName: String): File
    fun getFile(context: Context, fileName: String): File
    fun getFilesFromDirectory(context: Context, directoryName: String): List<File>?

    fun deleteFileFromDirectory(context: Context, directoryName: String, fileName: String): Boolean
    fun deleteDirectory(context: Context, directoryName: String): Boolean
    fun deleteFile(context: Context, fileName: String): Boolean

    fun getMostRecentFromDirectory(context: Context, directoryName: String): File?
}