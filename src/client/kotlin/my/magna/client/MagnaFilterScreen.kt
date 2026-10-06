package my.magna.client

import my.magna.MagnetSettings
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class MagnaFilterScreen(private val parent: Screen) : Screen(Component.literal("Item Filter")) {
    private val d: MagnetSettings get() = ClientSettings.data

    private lateinit var searchBox: EditBox
    private lateinit var modeBtn: Button
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button

    private val allItems: List<Item> by lazy {
        BuiltInRegistries.ITEM.stream().filter { it != Items.AIR }.toList()
    }

    private var filteredItems: List<Item> = emptyList()
    private var currentPage = 0
    private val pageSize = 10
    private val itemButtons = ArrayList<Button>()

    private fun getItemName(item: Item): String {
        return runCatching { ItemStack(item).hoverName.string }.getOrDefault(BuiltInRegistries.ITEM.getKey(item).path)
    }

    private fun updateFilter() {
        val query = searchBox.value.trim().lowercase()
        filteredItems = if (query.isEmpty()) {
            allItems
        } else {
            allItems.filter { item ->
                val id = BuiltInRegistries.ITEM.getKey(item).toString().lowercase()
                val name = getItemName(item).lowercase()
                query in id || query in name
            }
        }
        currentPage = 0
        refreshPage()
    }

    private fun refreshPage() {
        val totalPages = maxOf(1, (filteredItems.size + pageSize - 1) / pageSize)
        currentPage = currentPage.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * pageSize
        val pageItems = filteredItems.drop(startIndex).take(pageSize)

        for (i in itemButtons.indices) {
            val btn = itemButtons[i]
            if (i < pageItems.size) {
                val item = pageItems[i]
                val id = BuiltInRegistries.ITEM.getKey(item).toString()
                val checked = d.filterItems.contains(id)

                // التحديد صار باليسار بصف الاسم مع لمسة جمالية ملونة
                val boxSymbol = if (checked) Component.literal("[✔] ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                else Component.literal("[  ] ").withStyle(ChatFormatting.DARK_GRAY)

                val itemName = Component.translatable(item.descriptionId).withStyle(if (checked) ChatFormatting.WHITE else ChatFormatting.GRAY)
                btn.message = Component.empty().append(boxSymbol).append(itemName)
                btn.visible = true
            } else {
                btn.visible = false
            }
        }

        prevBtn.active = currentPage > 0
        nextBtn.active = currentPage < totalPages - 1

        val modeLabel = if (d.filterWhitelist) "WHITELIST" else "BLACKLIST"
        val modeColor = if (d.filterWhitelist) ChatFormatting.AQUA else ChatFormatting.RED
        modeBtn.message = Component.literal("● ").withStyle(modeColor, ChatFormatting.BOLD)
            .append(Component.literal("Mode: ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(modeLabel).withStyle(modeColor, ChatFormatting.BOLD))
    }

    private fun toggleItem(indexInPage: Int) {
        val actualIndex = currentPage * pageSize + indexInPage
        if (actualIndex in filteredItems.indices) {
            val item = filteredItems[actualIndex]
            val id = BuiltInRegistries.ITEM.getKey(item).toString()
            if (d.filterItems.contains(id)) {
                d.filterItems.remove(id)
            } else {
                d.filterItems.add(id)
            }
            saveAndSync()
            refreshPage()
        }
    }

    private fun saveAndSync() {
        ClientSettings.save()
        MagnaClient.pushSettings()
    }

    override fun init() {
        val w = 310
        val left = width / 2 - w / 2
        var y = 6

        // عنوان فخم ومودرن
        val titleBtn = Button.builder(
            Component.literal("✦ MAGNA ITEM FILTER ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ) { _ -> }.bounds(left, y, w, 18).build()
        titleBtn.active = false
        addRenderableWidget(titleBtn)
        y += 22

        // شريط بحث عصري
        searchBox = EditBox(font, left, y, w, 18, Component.literal("Search..."))
        searchBox.setResponder { updateFilter() }
        addRenderableWidget(searchBox)
        y += 22

        // أزرار التحكم السريعة (مود + أزرار مجمعة)
        modeBtn = Button.builder(Component.empty()) { _ ->
            d.filterWhitelist = !d.filterWhitelist
            saveAndSync()
            refreshPage()
        }.bounds(left, y, 110, 18).build()
        addRenderableWidget(modeBtn)

        val oresBtn = Button.builder(Component.literal("★ Ores").withStyle(ChatFormatting.YELLOW)) { _ ->
            d.filterItems.clear()
            for (item in allItems) {
                val id = BuiltInRegistries.ITEM.getKey(item).path.lowercase()
                if (id.endsWith("_ore") || id.startsWith("raw_") ||
                    id in listOf("diamond", "emerald", "coal", "redstone", "lapis_lazuli", "amethyst_shard", "netherite_scrap", "ancient_debris", "iron_ingot", "gold_ingot", "copper_ingot", "netherite_ingot", "quartz")
                ) {
                    d.filterItems.add(BuiltInRegistries.ITEM.getKey(item).toString())
                }
            }
            saveAndSync()
            refreshPage()
        }.bounds(left + 114, y, 62, 18).build()
        addRenderableWidget(oresBtn)

        val selectAllBtn = Button.builder(Component.literal("All").withStyle(ChatFormatting.GREEN)) { _ ->
            for (item in filteredItems) {
                d.filterItems.add(BuiltInRegistries.ITEM.getKey(item).toString())
            }
            saveAndSync()
            refreshPage()
        }.bounds(left + 180, y, 62, 18).build()
        addRenderableWidget(selectAllBtn)

        val clearAllBtn = Button.builder(Component.literal("Clear").withStyle(ChatFormatting.RED)) { _ ->
            d.filterItems.clear()
            saveAndSync()
            refreshPage()
        }.bounds(left + 246, y, 64, 18).build()
        addRenderableWidget(clearAllBtn)
        y += 22

        // شبكة العناصر (صفين، التحديد باليسار بصف الاسم بوضوح تام)
        itemButtons.clear()
        val btnW = w / 2 - 3
        val startY = y
        for (i in 0 until pageSize) {
            val col = i % 2
            val row = i / 2
            val bx = left + col * (btnW + 6)
            val by = startY + row * 20

            val btn = Button.builder(Component.empty()) { _ -> toggleItem(i) }
                .bounds(bx, by, btnW, 18).build()
            itemButtons.add(btn)
            addRenderableWidget(btn)
        }
        y = startY + 5 * 20 + 4

        // أزرار التنقل والرجوع بأسلوب كبسولات سفلية أنيقة
        prevBtn = Button.builder(Component.literal("◀ Prev").withStyle(ChatFormatting.YELLOW)) { _ ->
            currentPage--
            refreshPage()
        }.bounds(left, y, 70, 18).build()
        addRenderableWidget(prevBtn)

        nextBtn = Button.builder(Component.literal("Next ▶").withStyle(ChatFormatting.YELLOW)) { _ ->
            currentPage++
            refreshPage()
        }.bounds(left + 74, y, 70, 18).build()
        addRenderableWidget(nextBtn)

        val backBtn = Button.builder(Component.literal("✔ Done").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)) { _ ->
            MagnaClient.openScreen(parent)
        }.bounds(left + w - 80, y, 80, 18).build()
        addRenderableWidget(backBtn)

        updateFilter()
    }
}
