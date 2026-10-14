package io.github.mangi.eta.data.model

import kotlinx.serialization.Serializable

const val MIN_INTERFACE_SCALE = 0.8f
const val MAX_INTERFACE_SCALE = 1.1f
const val DEFAULT_INTERFACE_SCALE = 1f

/** 背景图暗化下限（不压暗）。 */
const val MIN_BACKGROUND_DIM = 0f

/** 背景图暗化上限，保留一点可见度，避免完全看不到图。 */
const val MAX_BACKGROUND_DIM = 0.8f

/** 默认暗化程度：在深色文字主题与浅色文字主题下都能保证前景可读。 */
const val DEFAULT_BACKGROUND_DIM = 0.35f

@Serializable
data class AppearanceSettings(
    val themeMode: AppearanceThemeMode = AppearanceThemeMode.SYSTEM,
    val monetEnabled: Boolean = false,
    val paletteStyle: AppearancePaletteStyle = AppearancePaletteStyle.TONAL_SPOT,
    val accentColor: AppearanceAccentColor = AppearanceAccentColor.SYSTEM,
    val pureBlackEnabled: Boolean = false,
    val blurEnabled: Boolean = true,
    val topBarBlurStyle: AppearanceTopBarBlurStyle = AppearanceTopBarBlurStyle.GAUSSIAN,
    val swipeDismissEnabled: Boolean = true,
    val predictiveBackEnabled: Boolean = true,
    val interfaceScale: Float = DEFAULT_INTERFACE_SCALE,
    /** 自定义背景图的相册 URI；为空表示使用主题纯色背景。 */
    val backgroundImageUri: String = "",
    /** 背景图暗化程度，越大越压暗，用于保证前景内容对比度。 */
    val backgroundDim: Float = DEFAULT_BACKGROUND_DIM,
) {
    fun normalized(): AppearanceSettings = copy(
        interfaceScale = normalizeInterfaceScale(interfaceScale),
        backgroundDim = normalizeBackgroundDim(backgroundDim),
    )

    /** 是否启用了自定义背景图。 */
    val hasBackgroundImage: Boolean
        get() = backgroundImageUri.isNotBlank()
}

@Serializable
enum class AppearanceThemeMode(val persistedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPersistedValue(value: String?): AppearanceThemeMode =
            entries.firstOrNull { it.persistedValue == value } ?: SYSTEM
    }
}

@Serializable
enum class AppearancePaletteStyle(val persistedValue: String) {
    TONAL_SPOT("tonal_spot"),
    NEUTRAL("neutral"),
    VIBRANT("vibrant"),
    EXPRESSIVE("expressive"),
    RAINBOW("rainbow"),
    FRUIT_SALAD("fruit_salad"),
    MONOCHROME("monochrome"),
    FIDELITY("fidelity"),
    CONTENT("content");

    companion object {
        fun fromPersistedValue(value: String?): AppearancePaletteStyle =
            entries.firstOrNull { it.persistedValue == value } ?: TONAL_SPOT
    }
}

@Serializable
enum class AppearanceAccentColor(val persistedValue: String) {
    SYSTEM("system"),
    BLUE("blue"),
    PURPLE("purple"),
    PINK("pink"),
    RED("red"),
    ORANGE("orange"),
    YELLOW("yellow"),
    GREEN("green"),
    TEAL("teal");

    companion object {
        fun fromPersistedValue(value: String?): AppearanceAccentColor =
            entries.firstOrNull { it.persistedValue == value } ?: SYSTEM
    }
}

@Serializable
enum class AppearanceTopBarBlurStyle(val persistedValue: String) {
    GAUSSIAN("gaussian"),
    PROGRESSIVE("progressive");

    companion object {
        fun fromPersistedValue(value: String?): AppearanceTopBarBlurStyle =
            entries.firstOrNull { it.persistedValue == value } ?: GAUSSIAN
    }
}

fun normalizeInterfaceScale(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_INTERFACE_SCALE, MAX_INTERFACE_SCALE)
    } else {
        DEFAULT_INTERFACE_SCALE
    }

fun normalizeBackgroundDim(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_BACKGROUND_DIM, MAX_BACKGROUND_DIM)
    } else {
        DEFAULT_BACKGROUND_DIM
    }
