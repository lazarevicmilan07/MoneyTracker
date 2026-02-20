package com.moneytracker.simplebudget.data.repository

import com.moneytracker.simplebudget.data.local.dao.AccountDao
import com.moneytracker.simplebudget.data.mapper.toDomain
import com.moneytracker.simplebudget.data.mapper.toEntity
import com.moneytracker.simplebudget.domain.model.Account
import com.moneytracker.simplebudget.domain.model.AccountWithBalance
import com.moneytracker.simplebudget.domain.model.DefaultAccounts
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

    fun getAllAccountsWithBalances(): Flow<List<AccountWithBalance>> =
        accountDao.getAllAccountsWithBalances().map { entities ->
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
            accountDao.insertAccounts(DefaultAccounts.map { it.toEntity() })
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

    suspend fun deleteAllAccounts() =
        accountDao.deleteAllAccounts()

    suspend fun getAllAccountsSync(): List<Account> =
        accountDao.getAllAccountsSync().map { it.toDomain() }
}
