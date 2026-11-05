package za.ac.imali.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import za.ac.imali.data.FinanceRepository
import za.ac.imali.data.SessionManager

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)
        val token = session.token ?: return Result.success()
        return try {
            FinanceRepository(applicationContext).syncPending(token)
            Log.i(TAG, "Background sync completed")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background sync retry: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
