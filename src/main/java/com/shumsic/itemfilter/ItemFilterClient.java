package com.shumsic.itemfilter;

import com.mojang.blaze3d.platform.InputConstants;
import com.shumsic.itemfilter.config.ModConfig;
import com.shumsic.itemfilter.gui.ItemFilterScreen;
import com.shumsic.itemfilter.gui.ItemFilterSidebar;
import com.shumsic.itemfilter.logic.FilterExecutor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

/**
 * 物品過濾 Fabric 客戶端入口點
 */
public class ItemFilterClient implements ClientModInitializer {
    public static final String MOD_ID = "itemfilter";

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "category")
    );

    public static KeyMapping OPEN_CONFIG_KEY;

    @Override
    public void onInitializeClient() {
        // 載入設定檔
        ModConfig.getInstance();

        // 註冊快捷鍵（預設為 O 鍵，可隨時在按鍵控制中自訂）
        OPEN_CONFIG_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.itemfilter.open_config",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_O,
                CATEGORY
        ));

        // 註冊畫面初始化事件
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                // 掛載容器左側懸浮控制按鈕
                ItemFilterSidebar.attachToContainer(containerScreen);
                FilterExecutor.getInstance().onScreenOpened(containerScreen);

                // 註冊滑鼠中鍵監聽 (Middle Click)
                ScreenMouseEvents.beforeMouseClick(screen).register((scr, event) -> {
                    if (event.button() == 2) {
                        ItemFilterSidebar.handleMiddleClick(containerScreen, event.x(), event.y());
                    }
                });

                // 註冊關閉畫面事件
                ScreenEvents.remove(screen).register(scr -> {
                    FilterExecutor.getInstance().onScreenClosed();
                });
            }
        });

        // 註冊每 Tick 事件，處理過濾延遲佇列與按鍵檢查
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FilterExecutor.getInstance().onClientTick(client);

            while (OPEN_CONFIG_KEY.consumeClick()) {
                if (client.gui != null && client.gui.screen() == null) {
                    client.gui.setScreen(new ItemFilterScreen(null));
                }
            }
        });
    }
}
