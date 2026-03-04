package com.smsfinance.domain.model

data class Budget(
    val id: Long = 0,
    val userId: Long = 1L,
    val category: String,
    val icon: String = "💰",
    val color: String = "#00C853",
    val amount: Double,
    val keywords: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class BudgetProgress(
    val budget: Budget,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Double,
    val isOverBudget: Boolean
)

/** Preset budget categories with icons and matching keywords */
val BUDGET_PRESETS = listOf(
    Triple("Food & Dining", "🍽️", "restaurant,hotel,food,chakula,mkaa,meza"),
    Triple("Transport", "🚗", "fuel,petrol,fare,nauli,uber,bolt,daladala,boda"),
    Triple("Bills & Utilities", "💡", "electricity,umeme,water,maji,luku,internet,wifi"),
    Triple("Shopping", "🛍️", "shop,duka,store,market,soko,supermarket"),
    Triple("Health", "🏥", "hospital,clinic,dawa,medicine,pharmacy,daktari"),
    Triple("Education", "📚", "school,shule,fees,ada,university,chuo"),
    Triple("Entertainment", "🎬", "cinema,event,burudani,starehe"),
    Triple("Savings", "🏦", "savings,akiba,save"),
    Triple("Other", "💰", "")
)
