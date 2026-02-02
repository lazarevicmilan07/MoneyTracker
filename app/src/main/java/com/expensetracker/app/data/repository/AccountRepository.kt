package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.AccountDao
import com.expensetracker.app.data.mapper.toDomain
import com.expensetracker.app.data.mapper.toEntity
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.DefaultAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {

    fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getAccountById(id: Long): Account? =
        accountDao.getAccountById(id)?.toDomain()

    suspend fun getDefaultAccount(): Account? =
        accountDao.getDefaultAccount()?.toDomain()

    suspend fun getAccountCount(): Int =
        accountDao.getAccountCount()

    suspend fun insertAccount(account: Account): Long =
        accountDao.insertAccount(account.toEntity())

    suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.toEntity())

    suspend fun deleteAccount(account: Account) =
        accountDao.deleteAccount(account.toEntity())

    suspend fun deleteAccountById(id: Long) =
        accountDao.deleteAccountById(id)

    suspend fun initializeDefaultAccount() {
        if (!accountDao.hasAccounts()) {
            accountDao.insertAccount(DefaultAccount.toEntity())
        }
    }

    suspend fun setDefaultAccount(accountId: Long) {
        accountDao.clearAllDefaults()
        val account = accountDao.getAccountById(accountId)
        account?.let { accountDao.updateAccount(it.copy(isDefault = true)) }
    }

    suspend fun clearDefaultAccount(accountId: Long) {
        val account = accountDao.getAccountById(accountId)
        account?.let { accountDao.updateAccount(it.copy(isDefault = false)) }
    }

    fun getAccountBalance(accountId: Long): Flow<Double> =
        accountDao.getAccountBalance(accountId)

    fun getTotalBalance(): Flow<Double?> =
        accountDao.getTotalBalance()

    suspend fun insertAccounts(accounts: List<Account>) =
        accountDao.insertAccounts(accounts.map { it.toEntity() })
}
