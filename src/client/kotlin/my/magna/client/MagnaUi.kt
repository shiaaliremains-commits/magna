package my.magna.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

class UiRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun hit(mx: Double, my: Double): Boolean = mx >= x && mx < x + w && my >= y && my < y + h
    fun grow(p: Int): UiRect = UiRect(x - p, y - p, w + 2 * p, h + 2 * p)
}

object MagnaUi {
    val PANEL = 0xF2101322.toInt()
    val BORDER = 0xFF2A3050.toInt()
    val CARD = 0xFF1A1E34.toInt()
    val CARD_HOVER = 0xFF252A4A.toInt()
    val TEXT = 0xFFE8EAF6.toInt()
    val MUTED = 0xFF8A91B0.toInt()
    val ACCENT = 0xFF8B5CF6.toInt()
    val ACCENT2 = 0xFF22D3EE.toInt()
    val GOOD = 0xFF34D399.toInt()
    val BAD = 0xFFEF4444.toInt()
    val WARN = 0xFFFBBF24.toInt()
    val OFF = 0xFF4B5068.toInt()
    val WHITE = 0xFFFFFFFF.toInt()

    fun click() {
        runCatching {
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
        }
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(s: Int): Int = ((((a shr s) and 0xFF) * (1 - k)) + (((b shr s) and 0xFF) * k)).toInt()
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun rounded(g: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, c: Int) {
        if (w <= 2 || h <= 2) { g.fill(x, y, x + w, y + h, c); return }
        g.fill(x + 1, y, x + w - 1, y + h, c)
        g.fill(x, y + 1, x + w, y + h - 1, c)
    }

    fun card(g: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int) {
        rounded(g, x, y, w, h, border)
        rounded(g, x + 1, y + 1, w - 2, h - 2, fill)
    }

    fun gradient(g: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, c1: Int, c2: Int) {
        var i = 0
        while (i < w) {
            val seg = minOf(3, w - i)
            g.fill(x + i, y, x + i + seg, y + h, mix(c1, c2, i.toFloat() / w))
            i += seg
        }
    }

    fun text(g: GuiGraphicsExtractor, font: Font, c: Component, x: Int, y: Int, color: Int) {
        g.centeredText(font, c, x + font.width(c) / 2, y, color)
    }

    fun textRight(g: GuiGraphicsExtractor, font: Font, c: Component, xRight: Int, y: Int, color: Int) {
        g.centeredText(font, c, xRight - font.width(c) / 2, y, color)
    }

    fun textCenter(g: GuiGraphicsExtractor, font: Font, c: Component, cx: Int, y: Int, color: Int) {
        g.centeredText(font, c, cx, y, color)
    }

    fun fit(font: Font, s: String, maxW: Int): String {
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    fun segIndex(r: UiRect, n: Int, mx: Double): Int =
        (((mx - r.x) * n) / r.w).toInt().coerceIn(0, n - 1)

    fun segmented(
        g: GuiGraphicsExtractor, font: Font, r: UiRect, labels: List<String>,
        selected: Int, mx: Int, my: Int, accent: Int
    ) {
        card(g, r.x, r.y, r.w, r.h, CARD, BORDER)
        val n = labels.size
        for (i in 0 until n) {
            val x0 = r.x + r.w * i / n
            val x1 = r.x + r.w * (i + 1) / n
            val hov = mx >= x0 && mx < x1 && my >= r.y && my < r.y + r.h
            if (i == selected) rounded(g, x0 + 2, r.y + 2, x1 - x0 - 4, r.h - 4, accent)
            else if (hov) rounded(g, x0 + 2, r.y + 2, x1 - x0 - 4, r.h - 4, 0xFF2B3158.toInt())
            val label = fit(font, labels[i], x1 - x0 - 6)
            textCenter(
                g, font, Component.literal(label), (x0 + x1) / 2, r.y + (r.h - 8) / 2,
                if (i == selected) WHITE else MUTED
            )
        }
    }

    fun chip(g: GuiGraphicsExtractor, font: Font, r: UiRect, label: String, active: Boolean, hover: Boolean, color: Int) {
        val fill = when {
            active -> mix(CARD, color, 0.35f)
            hover -> CARD_HOVER
            else -> CARD
        }
        card(g, r.x, r.y, r.w, r.h, fill, if (active) color else BORDER)
        textCenter(
            g, font, Component.literal(fit(font, label, r.w - 6)),
            r.x + r.w / 2, r.y + (r.h - 8) / 2, if (active) WHITE else TEXT
        )
    }
}
