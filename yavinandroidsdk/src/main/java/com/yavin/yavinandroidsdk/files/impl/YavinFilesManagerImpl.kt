package com.yavin.yavinandroidsdk.files.impl

import android.content.Context
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import java.io.File

class YavinFilesManagerImpl : YavinFilesManager {

    companion object {
        private const val ROOT_FOLDER_NAME = "yavin_files"
    }

    override fun init() {

    }

    private fun getRootFolder(context: Context): File {
        val root = File(context.applicationContext.filesDir, ROOT_FOLDER_NAME)
        root.mkdirs()
        return root
    }

    override fun getDirectory(context: Context, directoryName: String): File {
        val root = getRootFolder(context)

        val directory = File(root, directoryName)
        directory.mkdirs()

        return directory
    }

    override fun getFileFromDirectory(context: Context, directoryName: String, fileName: String): File {
        val root = getRootFolder(context)

        val directory = File(root, directoryName)
        directory.mkdirs()

        return File(directory, fileName)
    }

    override fun getFile(context: Context, fileName: String): File {
        val root = getRootFolder(context)
        return File(root, fileName)
    }

    override fun getFilesFromDirectory(context: Context, directoryName: String, orderByDate: Boolean): List<File>? {
        val root = getRootFolder(context)
        val directory = File(root, directoryName)
        if (directory.isDirectory) {
            val files = directory.listFiles { f -> f.isFile }?.toList()

            return if (orderByDate) {
                files?.sortedByDescending {
                    it.lastModified()
                }
            } else {
                files
            }
        }

        return null
    }

    override fun deleteFileFromDirectory(context: Context, directoryName: String, fileName: String): Boolean {
        val root = getRootFolder(context)
        val file = File(root, directoryName + File.separator + fileName)

        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    override fun deleteDirectory(context: Context, directoryName: String): Boolean {
        val root = getRootFolder(context)
        val directory = File(root, directoryName)

        return if (directory.exists()) {
            directory.deleteRecursively()
        } else {
            false
        }
    }

    override fun deleteFile(context: Context, fileName: String): Boolean {
        val root = getRootFolder(context)
        val file = File(root, fileName)

        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}