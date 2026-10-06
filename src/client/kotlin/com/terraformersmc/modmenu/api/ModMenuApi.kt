package com.terraformersmc.modmenu.api

import net.minecraft.client.gui.screens.Screen

fun interface ConfigScreenFactory<T : Screen> {
    fun create(parent: Screen): T
}

interface ModMenuApi {
    fun getModConfigScreenFactory(): ConfigScreenFactory<*>
}
