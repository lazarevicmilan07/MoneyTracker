package com.expensetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CategoryIcon(
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIconForName(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "directions_car", "car", "transportation" -> Icons.Default.DirectionsCar
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "movie", "entertainment" -> Icons.Default.Movie
        "receipt_long", "receipt", "bills" -> Icons.Default.Receipt
        "medical_services", "health" -> Icons.Default.MedicalServices
        "school", "education" -> Icons.Default.School
        "payments", "salary", "money" -> Icons.Default.Payments
        "trending_up", "investment" -> Icons.Default.TrendingUp
        "home" -> Icons.Default.Home
        "flight", "travel" -> Icons.Default.Flight
        "fitness_center", "fitness" -> Icons.Default.FitnessCenter
        "pets", "pet" -> Icons.Default.Pets
        "child_care", "child" -> Icons.Default.ChildCare
        "card_giftcard", "gift" -> Icons.Default.CardGiftcard
        "local_cafe", "cafe", "coffee" -> Icons.Default.LocalCafe
        "wifi", "internet" -> Icons.Default.Wifi
        "phone", "mobile" -> Icons.Default.Phone
        "subscriptions", "subscription" -> Icons.Default.Subscriptions
        "savings" -> Icons.Default.Savings
        "account_balance", "bank" -> Icons.Default.AccountBalance
        "work", "business" -> Icons.Default.Work
        "category" -> Icons.Default.Category
        "local_grocery_store", "grocery" -> Icons.Default.LocalGroceryStore
        "fastfood", "fast_food" -> Icons.Default.Fastfood
        "delivery_dining", "delivery" -> Icons.Default.DeliveryDining
        "local_gas_station", "gas", "fuel" -> Icons.Default.LocalGasStation
        "train", "transit" -> Icons.Default.Train
        "local_parking", "parking" -> Icons.Default.LocalParking
        "build", "maintenance", "repair" -> Icons.Default.Build
        "local_taxi", "taxi", "ride" -> Icons.Default.LocalTaxi
        "checkroom", "clothing" -> Icons.Default.Checkroom
        "devices", "electronics" -> Icons.Default.Devices
        "spa", "personal_care", "beauty" -> Icons.Default.Spa
        "music_note", "music" -> Icons.Default.MusicNote
        "sports_esports", "games", "gaming" -> Icons.Default.SportsEsports
        "live_tv", "streaming" -> Icons.Default.LiveTv
        "sports_soccer", "sports" -> Icons.Default.SportsSoccer
        "bolt", "electricity", "power" -> Icons.Default.Bolt
        "water_drop", "water" -> Icons.Default.WaterDrop
        "apartment", "rent", "housing" -> Icons.Default.Apartment
        "local_hospital", "doctor", "hospital" -> Icons.Default.LocalHospital
        "local_pharmacy", "pharmacy" -> Icons.Default.LocalPharmacy
        "shield", "insurance" -> Icons.Default.Shield
        "menu_book", "books", "book" -> Icons.Default.MenuBook
        "laptop", "computer" -> Icons.Default.Laptop
        "attach_money", "bonus" -> Icons.Default.AttachMoney
        "show_chart", "stocks" -> Icons.Default.ShowChart
        "currency_exchange", "crypto", "exchange" -> Icons.Default.CurrencyExchange
        "volunteer_activism", "charity", "donate" -> Icons.Default.VolunteerActivism
        "star", "favorite" -> Icons.Default.Star
        "cake", "celebration" -> Icons.Default.Cake
        "brush", "art" -> Icons.Default.Brush
        "yard", "garden" -> Icons.Default.Yard
        else -> Icons.Default.MoreHoriz
    }
}

val AvailableIcons = listOf(
    "restaurant",
    "directions_car",
    "shopping_bag",
    "movie",
    "receipt_long",
    "medical_services",
    "school",
    "payments",
    "trending_up",
    "home",
    "flight",
    "fitness_center",
    "pets",
    "child_care",
    "card_giftcard",
    "local_cafe",
    "wifi",
    "phone",
    "subscriptions",
    "savings",
    "account_balance",
    "work",
    "category",
    "local_grocery_store",
    "fastfood",
    "delivery_dining",
    "local_gas_station",
    "train",
    "local_parking",
    "build",
    "local_taxi",
    "checkroom",
    "devices",
    "spa",
    "music_note",
    "sports_esports",
    "live_tv",
    "sports_soccer",
    "bolt",
    "water_drop",
    "apartment",
    "local_hospital",
    "local_pharmacy",
    "shield",
    "menu_book",
    "laptop",
    "attach_money",
    "show_chart",
    "currency_exchange",
    "volunteer_activism",
    "star",
    "cake",
    "brush",
    "yard",
    "more_horiz"
)

val AvailableColors = listOf(
    Color(0xFFFF6B6B),
    Color(0xFF4ECDC4),
    Color(0xFFFFE66D),
    Color(0xFF95E1D3),
    Color(0xFFA8E6CF),
    Color(0xFFDDA0DD),
    Color(0xFF87CEEB),
    Color(0xFF98D8C8),
    Color(0xFFB4A7D6),
    Color(0xFFBDBDBD),
    Color(0xFFFF9F43),
    Color(0xFFEE5A24),
    Color(0xFF7158E2),
    Color(0xFF3AE374),
    Color(0xFF17C0EB),
    Color(0xFFE056A0),
    Color(0xFF5F27CD),
    Color(0xFF01A3A4),
    Color(0xFFF8A5C2),
    Color(0xFF778BEB),
    Color(0xFFF3A683),
    Color(0xFF63CDDA),
    Color(0xFFCF6A87),
    Color(0xFF574B90),
    Color(0xFFE77F67),
    Color(0xFF786FA6),
    Color(0xFF2BCB6E),
    Color(0xFFFF6348),
    Color(0xFF70A1FF),
    Color(0xFFFFC312)
)
