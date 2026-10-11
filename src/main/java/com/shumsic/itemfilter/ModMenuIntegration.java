package com.shumsic.itemfilter;

import com.shumsic.itemfilter.gui.ItemFilterScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 模組清單設定頁面整合
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ItemFilterScreen::new;
    }
}
