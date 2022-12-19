package com.yavin.yavinandroidsdk.logger.repository

import android.content.Context
import androidx.work.ListenableWorker
import java.io.File

interface IYavinLoggerUploaderRepository {
    suspend fun uploadFile(context: Context, file: File): ListenableWorker.Result
}