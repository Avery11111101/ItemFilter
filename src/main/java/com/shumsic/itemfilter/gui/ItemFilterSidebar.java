package com.shumsic.itemfilter.gui;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.ModConfig;
import com.shumsic.itemfilter.logic.FilterExecutor;
import com.shumsic.itemfilter.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 在容器畫面左側空白處整合過濾控制面板（設定檔切換、手動過濾、擷取容器、設定視窗）
 */
public class ItemFilterSidebar {

    public static void attachToContainer(AbstractContainerScreen<?> screen) {
        if (!FilterExecutor.getInstance().isContainerScreen(screen)) {
            return;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.getLeftPos();
        int topPos = accessor.getTopPos();

        int buttonWidth = 72;
        int buttonHeight = 20;
        int spacing = 2;
        // 放置在箱子左側空白處，預留 4 像素間距
        int startX = Math.max(2, leftPos - buttonWidth - 4);
        int currentY = topPos;

        // 1. 設定檔切換 / 狀態按鈕
        Button profileBtn = Button.builder(getProfileButtonText(), btn -> {
            cycleProfile();
            btn.setMessage(getProfileButtonText());
            btn.setTooltip(getProfileButtonTooltip());
        }).bounds(startX, currentY, buttonWidth, buttonHeight)
          .tooltip(getProfileButtonTooltip())
          .build();
        Screens.getWidgets(screen).add(profileBtn);
        currentY += buttonHeight + spacing;

        // 2. 一鍵執行過濾按鈕
        Button filterBtn = Button.builder(Component.literal("🧹 過濾"), btn -> {
            FilterExecutor.getInstance().executeFilter(screen, true);
        }).bounds(startX, currentY, buttonWidth, buttonHeight)
          .tooltip(Tooltip.create(Component.literal("§6立即過濾§r\n比對目前設定檔規則，將超出數量或不在清單中的物品丟出。")))
          .build();
        Screens.getWidgets(screen).add(filterBtn);
        currentY += buttonHeight + spacing;

        // 3. 擷取此箱按鈕
        Button captureBtn = Button.builder(Component.literal("📥 擷取"), btn -> {
            FilterExecutor.getInstance().captureFromContainer(screen);
            profileBtn.setMessage(getProfileButtonText());
            profileBtn.setTooltip(getProfileButtonTooltip());
        }).bounds(startX, currentY, buttonWidth, buttonHeight)
          .tooltip(Tooltip.create(Component.literal("§b擷取此容器§r\n將目前箱子內的所有物品不重複導入當前設定檔清單。")))
          .build();
        Screens.getWidgets(screen).add(captureBtn);
        currentY += buttonHeight + spacing;

        // 4. 開啟完整設定介面按鈕
        Button configBtn = Button.builder(Component.literal("⚙️ 設定"), btn -> {
            Minecraft client = Minecraft.getInstance();
            if (client.gui != null) {
                client.gui.setScreen(new ItemFilterScreen(screen));
            }
        }).bounds(startX, currentY, buttonWidth, buttonHeight)
          .tooltip(Tooltip.create(Component.literal("§a開啟設定視窗§r\n管理設定檔、編輯物品保留數量上限與 NBT 比對模式。")))
          .build();
        Screens.getWidgets(screen).add(configBtn);
    }

    private static Component getProfileButtonText() {
        ModConfig config = ModConfig.getInstance();
        FilterProfile active = config.getActiveProfile();
        if (active == null) {
            return Component.literal("§7[無設定檔]");
        }
        String name = active.getName();
        if (name.length() > 6) {
            name = name.substring(0, 5) + "…";
        }
        return config.isFilterEnabled()
                ? Component.literal("§a[" + name + "]")
                : Component.literal("§7[" + name + "]");
    }

    private static Tooltip getProfileButtonTooltip() {
        ModConfig config = ModConfig.getInstance();
        FilterProfile active = config.getActiveProfile();
        String name = active != null ? active.getName() : "無";
        String status = config.isFilterEnabled() ? "§a啟用中" : "§c已暫停";

        String tip = String.format("§e當前設定檔：§f%s\n§e狀態：%s\n§7[左鍵] 切換設定檔\n§7[中鍵] 鎖定/暫停過濾", name, status);
        return Tooltip.create(Component.literal(tip));
    }

    private static void cycleProfile() {
        ModConfig config = ModConfig.getInstance();
        List<FilterProfile> profiles = config.getProfiles();
        if (profiles.isEmpty()) {
            config.ensureDefaults();
            config.save();
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(config.getActiveProfileId())) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % profiles.size();
        config.setActiveProfileId(profiles.get(nextIndex).getId());
        config.setFilterEnabled(true);
        config.save();

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(
                    Component.literal("§a[物品過濾] 已切換至設定檔「" + profiles.get(nextIndex).getName() + "」")
            );
        }
    }

    /**
     * 處理容器畫面上的滑鼠中鍵快捷操作
     */
    public static void handleMiddleClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        Slot hoveredSlot = accessor.getHoveredSlot();

        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack stack = hoveredSlot.getItem();
            ModConfig config = ModConfig.getInstance();
            FilterProfile active = config.getActiveProfile();
            if (active == null) {
                config.ensureDefaults();
                active = config.getActiveProfile();
            }

            FilterItemRule existing = active.findRule(stack);
            if (existing != null) {
                client.player.sendOverlayMessage(
                        Component.literal(String.format("§e[物品過濾] 設定檔「%s」中已包含「%s」(上限: %d)",
                                active.getName(), stack.getHoverName().getString(), existing.getMaxCount()))
                );
            } else {
                FilterItemRule newRule = FilterItemRule.fromItemStack(stack, active.getDefaultMaxCount(), active.isDefaultMatchNbt());
                active.addOrUpdateRule(newRule);
                config.save();
                client.player.sendOverlayMessage(
                        Component.literal(String.format("§a[物品過濾] 已快速將「%s」加入設定檔「%s」(上限: %d)",
                                stack.getHoverName().getString(), active.getName(), newRule.getMaxCount()))
                );
            }
        }
    }
}
