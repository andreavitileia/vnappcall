package com.vnsas.vnappcall.data

data class CallLogEntry(
    val name: String?,
    val number: String?,
    val type: Int,
    val date: Long,
    val durationSec: Int
) {
    val readableType: String
        get() = when (type) {
            1 -> "Entrante"
            2 -> "Uscente"
            3 -> "Persa"
            4 -> "Segreteria"
            5 -> "Rifiutata"
            6 -> "Bloccata"
            7 -> "Risposta alt."
            else -> "Sconosciuta"
        }
}
