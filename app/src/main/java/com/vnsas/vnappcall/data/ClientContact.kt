package com.vnsas.vnappcall.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client_contacts")
data class ClientContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val machines: String = ""  // Format: "serial1|model1,serial2|model2"
)
