package com.smsfinance.repository

import com.smsfinance.data.dao.InvestmentDao
import com.smsfinance.data.entity.InvestmentEntity
import com.smsfinance.domain.model.Investment
import com.smsfinance.domain.model.InvestmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestmentRepository @Inject constructor(private val dao: InvestmentDao) {

    fun getAll(userId: Long): Flow<List<Investment>> =
        dao.getAll(userId).map { it.map { e -> e.toDomain() } }

    fun getTotalValue(userId: Long): Flow<Double> = dao.getTotalValue(userId)
    fun getTotalInvested(userId: Long): Flow<Double> = dao.getTotalInvested(userId)

    suspend fun insert(i: Investment): Long = dao.insert(i.toEntity())
    suspend fun update(i: Investment) = dao.update(i.toEntity())
    suspend fun delete(i: Investment) = dao.delete(i.toEntity())

    private fun InvestmentEntity.toDomain() = Investment(
        id, userId, name, icon, color,
        InvestmentType.valueOf(type),
        initialAmount, currentValue, targetAmount, interestRate,
        startDate, maturityDate, institution, notes, createdAt
    )

    private fun Investment.toEntity() = InvestmentEntity(
        id, userId, name, icon, color, type.name,
        initialAmount, currentValue, targetAmount, interestRate,
        startDate, maturityDate, institution, notes, createdAt
    )
}
