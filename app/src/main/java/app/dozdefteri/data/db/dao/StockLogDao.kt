package app.dozdefteri.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.dozdefteri.data.db.entity.StockLogEntity

@Dao
interface StockLogDao {
    @Insert
    suspend fun insert(log: StockLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<StockLogEntity>)

    @Query("SELECT * FROM stock_logs WHERE medicationId = :medicationId ORDER BY createdAt, id")
    suspend fun getByMedication(medicationId: Long): List<StockLogEntity>

    @Query("SELECT * FROM stock_logs ORDER BY id")
    suspend fun getAll(): List<StockLogEntity>

    @Query("DELETE FROM stock_logs")
    suspend fun deleteAll()
}
