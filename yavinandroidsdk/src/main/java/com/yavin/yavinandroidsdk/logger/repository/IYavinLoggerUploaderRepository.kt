package com.yavin.yavinandroidsdk.logger.repository

import androidx.work.ListenableWorker
import java.io.File

interface IYavinLoggerUploaderRepository {
    fun uploadFile(file: File): ListenableWorker.Result
}