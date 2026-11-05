package za.ac.imali.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.ac.imali.data.local.ImaliDatabase
import za.ac.imali.data.local.TransactionEntity
import za.ac.imali.sync.SyncScheduler

class FinanceRepository(context: Context) {
    private val dao = ImaliDatabase.get(context).transactionDao()
    private val api = ApiClient.api
    private val appContext = context.applicationContext

    fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDto() } }

    suspend fun addTransactionLocal(
        amount: Double,
        type: String,
        category: String,
        note: String,
        date: String,
        photoUrl: String = "",
    ) {
        val id = dao.insert(
            TransactionEntity(
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
                photoUrl = photoUrl,
                syncStatus = TransactionEntity.SYNC_PENDING,
            ),
        )
        Log.i(TAG, "Saved local transaction id=$id (pending sync)")
        SyncScheduler.enqueue(appContext)
    }

    suspend fun syncPending(token: String) {
        val pending = dao.getBySyncStatus(TransactionEntity.SYNC_PENDING)
        if (pending.isEmpty()) return
        val bearer = "Bearer $token"
        for (item in pending) {
            try {
                val saved = api.addTransaction(
                    bearer,
                    TransactionRequest(item.amount, item.type, item.category, item.note, item.date),
                )
                dao.markSynced(item.localId, saved.id ?: "", TransactionEntity.SYNC_SYNCED)
                Log.i(TAG, "Synced transaction localId=${item.localId}")
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed for localId=${item.localId}: ${e.message}")
                throw e
            }
        }
    }

    suspend fun refreshFromServer(token: String) {
        val bearer = "Bearer $token"
        val pending = dao.getBySyncStatus(TransactionEntity.SYNC_PENDING)
        val remote = api.getTransactions(bearer)
        dao.clearAll()
        dao.insertAll(
            remote.map { tx ->
                TransactionEntity(
                    serverId = tx.id,
                    amount = tx.amount,
                    type = tx.type,
                    category = tx.category,
                    note = tx.note,
                    date = tx.date,
                    photoUrl = "",
                    syncStatus = TransactionEntity.SYNC_SYNCED,
                )
            },
        )
        pending.forEach { dao.insert(it) }
    }

    suspend fun deleteTransaction(token: String, serverId: String?) {
        if (!serverId.isNullOrBlank()) {
            api.deleteTransaction("Bearer $token", serverId)
            dao.deleteByServerId(serverId)
        }
    }

    private fun TransactionEntity.toDto() = Transaction(
        id = serverId ?: "local-$localId",
        amount = amount,
        type = type,
        category = category,
        note = note,
        date = date,
    )

    companion object {
        private const val TAG = "FinanceRepository"
    }
}
