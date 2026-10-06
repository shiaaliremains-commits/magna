package my.magna.client

import my.magna.MagnetSettings
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class MagnaFilterScreen(private val parent: Screen) : Screen(Component.literal("Item Filter")) {
    private class Entry(val item: Item, val id: String, val idLower: String, val nameLower: String)

    private val d: MagnetSettings get() = ClientSettings.data

    private val index: List<Entry> by lazy {
        BuiltInRegistries.ITEM.filter { it != Items.AIR }.map { item ->
            val key = BuiltInRegistries.ITEM.getKey(item).toString()
            val name = runCatching { ItemStack(item).hoverName.string }.getOrDefault(key)
            Entry(item, key, key.lowercase(), name.lowercase())
        }
    }

    private val oreExtras = setOf(
        "diamond", "emerald", "coal", "redstone", "lapis_lazuli", "amethyst_shard", "netherite_scrap",
        "ancient_debris", "iron_ingot", "gold_ingot", "copper_ingot", "netherite_ingot", "quartz"
    )

    private val stacks = HashMap<Item, ItemStack>()
    private var filtered: List<Entry> = emptyList()
    private var query = ""
    private var selectedOnly = false
    private var scroll = 0
    private var dragging = false
    private var hovered: Entry? = null

    private lateinit var searchBox: EditBox

    private var pX = 0
    private var pY = 0
    private var pW = 330
    private var pH = 300
    private var cols = 1
    private var visRows = 1
    private var gridX = 0
    private var gridY = 0
    private var backR = UiRect(0, 0, 0, 0)
    private var modeR = UiRect(0, 0, 0, 0)
    private var searchR = UiRect(0, 0, 0, 0)
    private var sbR = UiRect(0, 0, 0, 0)
    private var chipRs: List<UiRect> = emptyList()
    private val cell = 26

    private fun commit() {
        ClientSettings.save()
        MagnaClient.pushSettings()
    }

    private fun stackOf(item: Item): ItemStack = stacks.getOrPut(item) { ItemStack(item) }

    private fun totalRows(): Int = (filtered.size + cols - 1) / cols
    private fun maxScroll(): Int = maxOf(0, totalRows() - visRows)

    private fun applyFilter() {
        val q = query.trim().lowercase()
        val sel = d.filterItems
        filtered = index.filter { e ->
            (!selectedOnly || e.id in sel) && (q.isEmpty() || q in e.idLower || q in e.nameLower)
        }
        scroll = scroll.coerceIn(0, maxScroll())
    }

    private fun isOre(e: Entry): Boolean {
        val p = e.idLower.substringAfter(':')
        return p.endsWith("_ore") || p.startsWith("raw_") || p in oreExtras
    }

    private fun chipAction(i: Int) {
        when (i) {
            0 -> for (e in index) if (isOre(e)) d.filterItems.add(e.id)
            1 -> for (e in filtered) d.filterItems.add(e.id)
            2 -> d.filterItems.clear()
            3 -> selectedOnly = !selectedOnly
        }
        if (i != 3) commit()
        scroll = 0
        applyFilter()
    }

    private fun toggle(e: Entry) {
        if (d.filterItems.contains(e.id)) d.filterItems.remove(e.id) else d.filterItems.add(e.id)
        commit()
        if (selectedOnly) applyFilter()
    }

    private fun thumbH(): Int {
        val rows = maxOf(1, totalRows())
        return maxOf(14, sbR.h * visRows / rows).coerceAtMost(sbR.h)
    }

    private fun setScrollFromMouse(my: Double) {
        val th = thumbH()
        val avail = (sbR.h - th).coerceAtLeast(1)
        val t = ((my - sbR.y - th / 2.0) / avail).coerceIn(0.0, 1.0)
        scroll = Math.round(t * maxScroll()).toInt()
    }

    override fun onClose() {
        MagnaClient.openScreen(parent)
    }

    override fun init() {
        pW = minOf(330, width - 12)
        pH = minOf(height - 12, 300)
        pX = (width - pW) / 2
        pY = ((height - pH) / 2).coerceAtLeast(4)
        val ix = pX + 16
        val iw = pW - 32

        backR = UiRect(pX + pW - 16 - 56, pY + 10, 56, 20)
        modeR = UiRect(ix, pY + 44, iw, 20)
        searchR = UiRect(ix, pY + 84, iw, 20)

        val gap = 4
        val cw = (iw - gap * 3) / 4
        chipRs = (0 until 4).map { UiRect(ix + it * (cw + gap), pY + 110, cw, 18) }

        gridX = ix
        gridY = pY + 134
        val gridH = (pY + pH - 36) - gridY
        visRows = (gridH / cell).coerceAtLeast(1)
        val sbW = 6
        cols = ((iw - sbW - 4) / cell).coerceAtLeast(1)
        sbR = UiRect(ix + iw - sbW, gridY, sbW, visRows * cell - 2)

        searchBox = EditBox(font, searchR.x + 24, searchR.y + 6, searchR.w - 30, 12, Component.literal("Search"))
        searchBox.setBordered(false)
        searchBox.setHint(Component.literal("Search by name or id\u2026"))
        searchBox.value = query
        searchBox.setResponder {
            query = it
            scroll = 0
            applyFilter()
        }
        addWidget(searchBox)
        setInitialFocus(searchBox)
        applyFilter()
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        super.extractRenderState(g, mx, my, delta)
        val dmx = mx.toDouble()
        val dmy = my.toDouble()
        val ix = pX + 16
        val iw = pW - 32
        val whitelist = d.filterWhitelist
        val tint = if (whitelist) MagnaUi.GOOD else MagnaUi.BAD

        // اللوحة والرأس
        MagnaUi.card(g, pX, pY, pW, pH, MagnaUi.PANEL, MagnaUi.BORDER)
        MagnaUi.gradient(g, pX + 1, pY + 1, pW - 2, 2, MagnaUi.ACCENT, MagnaUi.ACCENT2)
        MagnaUi.text(g, font, Component.literal("Item Filter").withStyle(ChatFormatting.BOLD), ix, pY + 11, MagnaUi.TEXT)
        MagnaUi.text(g, font, Component.literal("Choose what the magnet picks up"), ix, pY + 24, MagnaUi.MUTED)

        val bHover = backR.hit(dmx, dmy)
        MagnaUi.card(g, backR.x, backR.y, backR.w, backR.h, if (bHover) MagnaUi.CARD_HOVER else MagnaUi.CARD, MagnaUi.BORDER)
        MagnaUi.textCenter(g, font, Component.literal("\u2039 Back"), backR.x + backR.w / 2, backR.y + 6, MagnaUi.TEXT)
        g.fill(ix, pY + 38, ix + iw, pY + 39, MagnaUi.BORDER)

        // الوضع
        MagnaUi.segmented(g, font, modeR, listOf("Blacklist", "Whitelist"), if (whitelist) 1 else 0, mx, my, tint)
        val count = d.filterItems.size
        val (desc, descColor) = when {
            whitelist && count == 0 -> "Whitelist is empty \u2014 nothing will be picked up!" to MagnaUi.WARN
            whitelist -> "Magnet collects only the selected items." to MagnaUi.MUTED
            else -> "Magnet ignores the selected items." to MagnaUi.MUTED
        }
        MagnaUi.text(g, font, Component.literal(desc), ix, pY + 69, descColor)

        // البحث
        MagnaUi.card(g, searchR.x, searchR.y, searchR.w, searchR.h, MagnaUi.CARD, if (searchBox.isFocused) MagnaUi.ACCENT else MagnaUi.BORDER)
        g.item(stackOf(Items.SPYGLASS), searchR.x + 4, searchR.y + 2)
        searchBox.extractRenderState(g, mx, my, delta)

        // الأزرار السريعة
        val labels = listOf("Ores", "Add shown", "Clear", "Selected")
        for (i in 0 until 4) {
            val r = chipRs[i]
            MagnaUi.chip(g, font, r, labels[i], i == 3 && selectedOnly, r.hit(dmx, dmy), MagnaUi.ACCENT)
        }

        // الشبكة
        scroll = scroll.coerceIn(0, maxScroll())
        hovered = null
        val total = filtered.size
        if (total == 0) {
            MagnaUi.textCenter(g, font, Component.literal("No items match"), ix + iw / 2, gridY + 20, MagnaUi.MUTED)
        }
        for (r in 0 until visRows) {
            for (c in 0 until cols) {
                val i = (scroll + r) * cols + c
                if (i >= total) break
                val e = filtered[i]
                val x = gridX + c * cell
                val y = gridY + r * cell
                val sel = d.filterItems.contains(e.id)
                val hov = mx >= x && mx < x + 24 && my >= y && my < y + 24
                if (hov) hovered = e
                val fill = if (sel) MagnaUi.mix(MagnaUi.CARD, tint, 0.28f) else MagnaUi.CARD
                val border = when {
                    sel -> tint
                    hov -> 0xFF5A6290.toInt()
                    else -> MagnaUi.BORDER
                }
                MagnaUi.card(g, x, y, 24, 24, fill, border)
                g.item(stackOf(e.item), x + 4, y + 4)
                if (sel) g.fill(x + 17, y + 3, x + 21, y + 7, tint)
            }
        }

        // شريط التمرير
        MagnaUi.rounded(g, sbR.x, sbR.y, sbR.w, sbR.h, MagnaUi.CARD)
        if (maxScroll() > 0) {
            val th = thumbH()
            val ty = sbR.y + (sbR.h - th) * scroll / maxScroll()
            MagnaUi.rounded(g, sbR.x, ty, sbR.w, th, if (dragging || sbR.grow(2).hit(dmx, dmy)) MagnaUi.ACCENT else 0xFF5A6290.toInt())
        }

        // الشريط السفلي
        val fy = pY + pH - 30
        g.fill(ix, fy - 2, ix + iw, fy - 1, MagnaUi.BORDER)
        val h = hovered
        if (h != null) {
            val name = runCatching { stackOf(h.item).hoverName.string }.getOrDefault(h.id)
            MagnaUi.text(g, font, Component.literal(MagnaUi.fit(font, name, iw / 2)), ix, fy + 2, MagnaUi.TEXT)
            MagnaUi.text(g, font, Component.literal(MagnaUi.fit(font, h.id, iw / 2)), ix, fy + 13, MagnaUi.MUTED)
        } else {
            MagnaUi.text(g, font, Component.literal("Click an item to toggle it"), ix, fy + 7, MagnaUi.MUTED)
        }
        MagnaUi.textRight(g, font, Component.literal("$count selected \u00B7 $total shown"), ix + iw, fy + 7, MagnaUi.MUTED)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        val mx = event.x()
        val my = event.y()

        if (backR.hit(mx, my)) {
            MagnaUi.click(); onClose(); return true
        }
        if (modeR.hit(mx, my)) {
            d.filterWhitelist = MagnaUi.segIndex(modeR, 2, mx) == 1
            MagnaUi.click(); commit(); return true
        }
        for (i in chipRs.indices) {
            if (chipRs[i].hit(mx, my)) {
                MagnaUi.click(); chipAction(i); return true
            }
        }
        if (sbR.grow(3).hit(mx, my) && maxScroll() > 0) {
            dragging = true
            setScrollFromMouse(my)
            return true
        }

        // خلايا الشبكة
        val col = ((mx - gridX) / cell).toInt()
        val row = ((my - gridY) / cell).toInt()
        if (mx >= gridX && my >= gridY && col in 0 until cols && row in 0 until visRows) {
            val inX = (mx - gridX) - col * cell < 24
            val inY = (my - gridY) - row * cell < 24
            val i = (scroll + row) * cols + col
            if (inX && inY && i in filtered.indices) {
                MagnaUi.click()
                toggle(filtered[i])
                return true
            }
        }
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (dragging) {
            setScrollFromMouse(event.y())
            return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging) {
            dragging = false
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        if (scrollY != 0.0) {
            val dir = if (scrollY > 0) -1 else 1
            scroll = (scroll + dir * 2).coerceIn(0, maxScroll())
            return true
        }
        return false
    }
}
