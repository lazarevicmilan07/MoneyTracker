package com.moneytracker.simplebudget.data.local

import androidx.room.TypeConverter
import com.moneytracker.simplebudget.domain.model.AccountType
import com.moneytracker.simplebudget.domain.model.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
}
