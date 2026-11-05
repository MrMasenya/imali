package za.ac.imali.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val amount: Double,
    val type: String,
    val category: String,
    val note: String = "",
    val date: String,
    val photoUrl: String = "",
    val syncStatus: String = SYNC_PENDING,
) {
    companion object {
        const val SYNC_PENDING = "pending"
        const val SYNC_SYNCED = "synced"
    }
}
