package com.yavin.yavinandroidsdk

import android.content.Context
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
        // Do your upload here
        return ListenableWorker.Result.success()
    }
}