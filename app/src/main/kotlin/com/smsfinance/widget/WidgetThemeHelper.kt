package com.smsfinance.widget

import android.graphics.Color
import com.smsfinance.R
import com.smsfinance.domain.model.WidgetTheme

/**
 * Maps WidgetTheme enum → drawable resource ID.
 * Using setBackgroundResource() instead of setBackgroundColor() avoids
 * the "Can't load widget" crash on some launchers (Samsung, MIUI, etc.)
 * that reject programmatic color backgrounds on RemoteViews root views.
 */
object WidgetThemeHelper {

    fun bgDrawable(theme: WidgetTheme): Int = when (theme) {
        WidgetTheme.SMART_DARK    -> R.drawable.widget_background
        WidgetTheme.TEAL_FRESH    -> R.drawable.widget_bg_teal
        WidgetTheme.BLUE_OCEAN    -> R.drawable.widget_bg_ocean
        WidgetTheme.PURPLE_NIGHT  -> R.drawable.widget_bg_purple
        WidgetTheme.SUNSET_ORANGE -> R.drawable.widget_bg_sunset
        WidgetTheme.ROSE_GOLD     -> R.drawable.widget_bg_rose
        WidgetTheme.DARK_MINIMAL  -> R.drawable.widget_bg_dark
        WidgetTheme.LIGHT_CLEAN   -> R.drawable.widget_bg_light
        WidgetTheme.GREEN_DARK    -> R.drawable.widget_bg_green
    }

    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    fun expenseColor(): Int = Color.parseColor("#FF5C5C")
}
