package com.vnsas.vnappcall.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientContactDao {

    @Upsert
    suspend fun upsert(contact: ClientContact): Long

    @Delete
    suspend fun delete(contact: ClientContact)

    @Query("SELECT * FROM client_contacts ORDER BY name ASC")
    fun observeAll(): Flow<List<ClientContact>>

    @Query("SELECT * FROM client_contacts WHERE phone = :phone LIMIT 1")
    suspend fun findByPhoneExact(phone: String): ClientContact?

    @Query("SELECT * FROM client_contacts ORDER BY name ASC")
    suspend fun getAll(): List<ClientContact>

    @Query("SELECT * FROM client_contacts WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<ClientContact>
}
