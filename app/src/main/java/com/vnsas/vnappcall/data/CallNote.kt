package com.vnsas.vnappcall.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_notes")
data class CallNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactName: String = "",
    val phone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val billable: Boolean = false,
    val resolved: Boolean = false,
    val serial: String = "",
    val durationSec: Int = 0
)
