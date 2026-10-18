package com.instantledger.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val PrimaryLight = Color(0xFF6750A4)
val SecondaryLight = Color(0xFF03DAC6)
val BackgroundLight = Color(0xFFF5F5F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val ErrorLight = Color(0xFFB3261E)

val PrimaryDark = Color(0xFFCFBCFF)
val SecondaryDark = Color(0xFF66FFF9)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF121212)
val SurfaceVariantDark = Color(0xFF2F2836)
val ErrorDark = Color(0xFFF2B8B5)

// Transaction colors (Material Red/Green 700 & 300)
private val DebitRedLight = Color(0xFFD32F2F)   // Red 700
private val CreditGreenLight = Color(0xFF388E3C) // Green 700
private val DebitRedDark = Color(0xFFE57373)    // Red 300
private val CreditGreenDark = Color(0xFF81C784) // Green 300

val LocalDarkTheme = staticCompositionLocalOf { false }

object TransactionColors {
    @Composable
    fun debit(): Color = if (LocalDarkTheme.current) DebitRedDark else DebitRedLight

    @Composable
    fun credit(): Color = if (LocalDarkTheme.current) CreditGreenLight else CreditGreenLight.copy() // same in light; override if needed
}
