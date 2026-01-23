package com.expensetracker.app.data.local

import androidx.room.TypeConverter
import com.expensetracker.app.data.local.entity.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
