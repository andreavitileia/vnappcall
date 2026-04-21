package com.vnsas.vnappcall.util

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(
    val name: String,
    val phone: String
)

object ContactsReader {

    suspend fun loadAll(context: Context): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<PhoneContact>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    try {
                        val name = it.getString(0) ?: continue
                        val phone = it.getString(1) ?: continue
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            contacts += PhoneContact(name.trim(), phone.trim())
                        }
                    } catch (_: Throwable) {
                        // Skip malformed contact entry
                    }
                }
            }
        } catch (_: Throwable) {
            // Permission not granted or other error
        }
        contacts.distinctBy { it.name + it.phone }
    }
}
