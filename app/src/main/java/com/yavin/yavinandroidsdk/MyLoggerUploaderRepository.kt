package com.yavin.yavinandroidsdk

import android.content.Context
import androidx.work.ListenableWorker
import com.yavin.yavinandroidsdk.logger.repository.IYavinLoggerUploaderRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyLoggerUploaderRepository @Inject constructor() : IYavinLoggerUploaderRepository {

    override suspend fun uploadFile(context: Context, file: File): ListenableWorker.Result {
        // Do your upload here
        return ListenableWorker.Result.success()
    }
}