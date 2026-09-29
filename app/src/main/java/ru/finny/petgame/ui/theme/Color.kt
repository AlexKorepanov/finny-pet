package ru.finny.petgame.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра игры. У каждого цвета есть светлый и тёмный вариант; какой отдавать,
 * решает [isDark]. Он читает состояние Compose, поэтому при переключении темы
 * экраны перекрашиваются сами.
 */
object FinnyColors {
    var isDark: () -> Boolean = { false }

    private fun pick(light: Long, dark: Long): Color = Color(if (isDark()) dark else light)

    val Background get() = pick(0xFFFDF6EC, 0xFF1E1A16)
    val Surface get() = pick(0xFFFFFCF6, 0xFF2A251F)
    val SurfaceVariant get() = pick(0xFFF6EADC, 0xFF342E27)
    val CardBorder get() = pick(0xFFE8D9C2, 0xFF3D362E)

    /** Нижняя «объёмная» кромка карточки: в тёмной теме темнее самой карточки, а не светлее. */
    val CardEdge get() = pick(0xFFE8D9C2, 0xFF14110E)
    val OutlineSoft get() = pick(0xFFD8C7AE, 0xFF5C5246)
    val TextPrimary get() = pick(0xFF43331F, 0xFFF3E9DA)
    val TextSecondary get() = pick(0xFF7A6A55, 0xFFBFB2A0)

    val Primary get() = pick(0xFF3B6FE0, 0xFF4C7FE8)
    val PrimaryEdge get() = pick(0xFF2C53AE, 0xFF2C53AE)
    val OnPrimary = Color(0xFFFFFFFF)

    val Success get() = pick(0xFF1F7A33, 0xFF2E8B45)
    val SuccessEdge get() = pick(0xFF14571F, 0xFF14571F)
    val Optional get() = pick(0xFFB45309, 0xFFC4630F)
    val OptionalEdge get() = pick(0xFF8A3F07, 0xFF8A3F07)
    val Tasks get() = pick(0xFF6D3FBF, 0xFF8A5CD6)
    val TasksEdge get() = pick(0xFF4E2A8F, 0xFF5A36A0)
    val Teal get() = pick(0xFF0E7A6E, 0xFF14897B)
    val TealEdge get() = pick(0xFF0A5A51, 0xFF0A5A51)
    val Adult get() = pick(0xFF5B6670, 0xFF6F7B86)
    val AdultEdge get() = pick(0xFF3F474F, 0xFF3F474F)

    val ProgressYellow = Color(0xFFF6C445)
    val ProgressHighlight = Color(0xFFFFE08A)
    val ProgressYellowEdge = Color(0xFFC9971F)

    /** Урок на тропинке, который ещё закрыт. */
    val LockedFace get() = pick(0xFFE5DACB, 0xFF4A4238)
    val LockedEdge get() = pick(0xFFC9BBA6, 0xFF3A332B)

    /** Ответ «не совсем»: мягкий красный, всегда вместе с иконкой и текстом. */
    val Wrong get() = pick(0xFFC0392B, 0xFFD04A3C)
    val WrongEdge get() = pick(0xFF8E2A20, 0xFF8E2A20)
    val WrongContainer get() = pick(0xFFFDE4E1, 0xFF4A2522)
    val OnProgressText = Color(0xFF4A3A14)

    /** Цвета полосы на стартовом экране (как на макете). Заставка — картинка, тема её не меняет. */
    val SplashInk = Color(0xFF5A3A1A)
    val SplashBarTrack = Color(0xFFFFF8EC)
    val SplashBarFill = Color(0xFFE39A3C)

    /** Плашки и нижнее меню поверх полянки на главном экране. */
    val HudSurface get() = pick(0xFFFFF8EC, 0xFF2A251F)
    val HudInk get() = pick(0xFF5A3A1A, 0xFFF3E9DA)
    val HudBorder get() = pick(0xFF5A3A1A, 0xFF14110E)

    /** Фиолетовый текст на светло-фиолетовой плашке. */
    val TasksText get() = pick(0xFF4E2A8F, 0xFFCDB8F5)

    val SoftBlue get() = pick(0xFFE8F0FF, 0xFF25324A)
    val SoftGreen get() = pick(0xFFE3F5E7, 0xFF1F3A27)
    val SoftOrange get() = pick(0xFFFCEBD3, 0xFF45321C)
    val SoftPurple get() = pick(0xFFF0E8FF, 0xFF342A4A)

    val BadgePositiveContainer get() = pick(0xFFDFF3E1, 0xFF1F3A27)
    val BadgePositiveContent get() = pick(0xFF14571F, 0xFF9FD8A9)
    val BadgeAttentionContainer get() = pick(0xFFFCEBD3, 0xFF45321C)
    val BadgeAttentionContent get() = pick(0xFF7A4A08, 0xFFF2C07A)
    val BadgeNeutralContainer get() = pick(0xFFF6EADC, 0xFF342E27)
    val BadgeNeutralContent get() = pick(0xFF6B5B44, 0xFFCDBFAB)
}
