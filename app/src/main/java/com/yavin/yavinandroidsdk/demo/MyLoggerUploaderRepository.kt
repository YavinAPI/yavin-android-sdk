package com.yavin.yavinandroidsdk.demo

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import com.yavin.yavinandroidsdk.logger.repository.IYavinLoggerUploaderRepository
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyLoggerUploaderRepository @Inject constructor(
    private val connectivityProvider: YavinConnectivityProvider
) : IYavinLoggerUploaderRepository {

    override suspend fun uploadFile(context: Context, file: File): ListenableWorker.Result {
        Log.d("MyLoggerUploaderRepository", "uploadFile called: ${file.name}")
        // Do your upload here
        return ListenableWorker.Result.success()
    }
}