package com.shumsic.itemfilter.gui;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 物品過濾完整設定視窗：管理多設定檔、檢視規則清單、設定獨立/統一數量與 NBT 比對模式
 */
public class ItemFilterScreen extends Screen {
    private final Screen parentScreen;
    private int selectedProfileIndex = 0;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    private EditBox profileNameField;
    private EditBox batchCountField;
    private EditBox manualItemField;

    public ItemFilterScreen(Screen parentScreen) {
        super(Component.literal("物品過濾設定"));
        this.parentScreen = parentScreen;

        ModConfig config = ModConfig.getInstance();
        config.ensureDefaults();
        List<FilterProfile> profiles = config.getProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(config.getActiveProfileId())) {
                selectedProfileIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();

        ModConfig config = ModConfig.getInstance();
        List<FilterProfile> profiles = config.getProfiles();
        if (selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = Math.max(0, profiles.size() - 1);
        }
        FilterProfile currentProfile = !profiles.isEmpty() ? profiles.get(selectedProfileIndex) : null;

        int leftPaneWidth = 140;
        int leftX = 10;
        int rightX = leftX + leftPaneWidth + 15;
        int rightWidth = this.width - rightX - 10;

        // ================= 左側：設定檔管理 =================
        int y = 30;
        // 顯示設定檔切換清單（按鈕列表）
        for (int i = 0; i < Math.min(5, profiles.size()); i++) {
            final int index = i;
            FilterProfile p = profiles.get(i);
            boolean isSelected = (index == selectedProfileIndex);
            boolean isActive = p.getId().equals(config.getActiveProfileId());

            String prefix = isActive ? "§a✔ " : (isSelected ? "§e▶ " : "  ");
            String btnText = prefix + p.getName();
            if (btnText.length() > 14) {
                btnText = btnText.substring(0, 13) + "…";
            }

            this.addRenderableWidget(Button.builder(Component.literal(btnText), btn -> {
                selectedProfileIndex = index;
                currentPage = 0;
                init();
            }).bounds(leftX, y, leftPaneWidth, 20).build());
            y += 22;
        }

        // 新增 / 更名輸入框
        y = Math.max(y + 10, 145);
        this.profileNameField = new EditBox(this.font, leftX, y, leftPaneWidth, 18, Component.literal("設定檔名稱"));
        if (currentProfile != null) {
            this.profileNameField.setValue(currentProfile.getName());
        }
        this.profileNameField.setHint(Component.literal("輸入設定檔名稱…"));
        this.addRenderableWidget(this.profileNameField);
        y += 22;

        // 新增與更名按鈕
        int halfWidth = (leftPaneWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("＋ 新增"), btn -> {
            String name = profileNameField.getValue().trim();
            if (!name.isEmpty()) {
                FilterProfile newP = new FilterProfile(name);
                config.getProfiles().add(newP);
                selectedProfileIndex = config.getProfiles().size() - 1;
                config.setActiveProfileId(newP.getId());
                config.save();
                currentPage = 0;
                init();
            }
        }).bounds(leftX, y, halfWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("更名"), btn -> {
            String name = profileNameField.getValue().trim();
            if (!name.isEmpty() && currentProfile != null) {
                currentProfile.setName(name);
                config.save();
                init();
            }
        }).bounds(leftX + halfWidth + 4, y, halfWidth, 20).build());
        y += 22;

        // 設為啟用 / 刪除按鈕
        this.addRenderableWidget(Button.builder(Component.literal("設為啟用"), btn -> {
            if (currentProfile != null) {
                config.setActiveProfileId(currentProfile.getId());
                config.setFilterEnabled(true);
                config.save();
                init();
            }
        }).bounds(leftX, y, halfWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c刪除"), btn -> {
            if (profiles.size() > 1 && currentProfile != null) {
                profiles.remove(selectedProfileIndex);
                selectedProfileIndex = Math.max(0, selectedProfileIndex - 1);
                config.setActiveProfileId(profiles.get(selectedProfileIndex).getId());
                config.save();
                currentPage = 0;
                init();
            }
        }).bounds(leftX + halfWidth + 4, y, halfWidth, 20).build());

        // ================= 右側：規則清單與批量設定 =================
        if (currentProfile != null) {
            int rightY = 30;

            // 批量控制列（統一數量上限與 NBT 設定）
            this.batchCountField = new EditBox(this.font, rightX + 60, rightY, 40, 18, Component.literal("統一數量"));
            this.batchCountField.setValue(String.valueOf(currentProfile.getDefaultMaxCount()));
            this.addRenderableWidget(this.batchCountField);

            this.addRenderableWidget(Button.builder(Component.literal("統一套用數量"), btn -> {
                try {
                    int uniformCount = Integer.parseInt(batchCountField.getValue().trim());
                    currentProfile.applyUniformMaxCount(uniformCount);
                    config.save();
                    init();
                } catch (NumberFormatException ignored) {}
            }).bounds(rightX + 105, rightY - 1, 95, 20)
              .tooltip(Tooltip.create(Component.literal("將此數量覆蓋套用至目前設定檔內的所有物品規則。")))
              .build());

            this.addRenderableWidget(Button.builder(
                    Component.literal(currentProfile.isDefaultMatchNbt() ? "§a全部比對NBT" : "§7全部忽略NBT"),
                    btn -> {
                        boolean nextState = !currentProfile.isDefaultMatchNbt();
                        currentProfile.applyUniformMatchNbt(nextState);
                        config.save();
                        init();
                    }
            ).bounds(rightX + 205, rightY - 1, 95, 20)
             .tooltip(Tooltip.create(Component.literal("切換所有物品比對 NBT / 資料組件（嚴格比對 vs 純物品 ID 模糊比對）。")))
             .build());

            rightY += 28;

            // 物品清單（分頁渲染）
            List<FilterItemRule> rules = currentProfile.getRules();
            int totalPages = Math.max(1, (int) Math.ceil((double) rules.size() / ITEMS_PER_PAGE));
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
            }

            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(rules.size(), startIndex + ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                final int ruleIndex = i;
                FilterItemRule rule = rules.get(i);
                int rowY = rightY + (i - startIndex) * 24;

                // 數量調整按鈕: -64, -1, [數量顯示], +1, +64
                this.addRenderableWidget(Button.builder(Component.literal("-64"), btn -> {
                    rule.setMaxCount(Math.max(-1, rule.getMaxCount() - 64));
                    config.save();
                    init();
                }).bounds(rightX + 130, rowY, 26, 18).build());

                this.addRenderableWidget(Button.builder(Component.literal("-1"), btn -> {
                    rule.setMaxCount(Math.max(-1, rule.getMaxCount() - 1));
                    config.save();
                    init();
                }).bounds(rightX + 158, rowY, 22, 18).build());

                // 點擊數量切換無上限或 64
                String countText = rule.getMaxCount() < 0 ? "無上限" : String.valueOf(rule.getMaxCount());
                this.addRenderableWidget(Button.builder(Component.literal(countText), btn -> {
                    rule.setMaxCount(rule.getMaxCount() == 64 ? -1 : 64);
                    config.save();
                    init();
                }).bounds(rightX + 182, rowY, 44, 18)
                  .tooltip(Tooltip.create(Component.literal("點擊快速切換 64 或無上限")))
                  .build());

                this.addRenderableWidget(Button.builder(Component.literal("+1"), btn -> {
                    rule.setMaxCount(Math.max(1, (rule.getMaxCount() < 0 ? 0 : rule.getMaxCount()) + 1));
                    config.save();
                    init();
                }).bounds(rightX + 228, rowY, 22, 18).build());

                this.addRenderableWidget(Button.builder(Component.literal("+64"), btn -> {
                    rule.setMaxCount(Math.max(64, (rule.getMaxCount() < 0 ? 0 : rule.getMaxCount()) + 64));
                    config.save();
                    init();
                }).bounds(rightX + 252, rowY, 26, 18).build());

                // NBT 比對開關
                this.addRenderableWidget(Button.builder(
                        Component.literal(rule.isMatchNbt() ? "§a比對NBT" : "§7忽略NBT"),
                        btn -> {
                            rule.setMatchNbt(!rule.isMatchNbt());
                            config.save();
                            init();
                        }
                ).bounds(rightX + 282, rowY, 56, 18).build());

                // 刪除規則按鈕
                this.addRenderableWidget(Button.builder(Component.literal("§c✕"), btn -> {
                    rules.remove(ruleIndex);
                    config.save();
                    init();
                }).bounds(rightX + 342, rowY, 20, 18)
                  .tooltip(Tooltip.create(Component.literal("從清單中移除此物品")))
                  .build());
            }

            // 分頁導航列
            int navY = rightY + ITEMS_PER_PAGE * 24 + 4;
            this.addRenderableWidget(Button.builder(Component.literal("◀ 上一頁"), btn -> {
                if (currentPage > 0) {
                    currentPage--;
                    init();
                }
            }).bounds(rightX, navY, 65, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("下一頁 ▶"), btn -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    init();
                }
            }).bounds(rightX + 70, navY, 65, 20).build());

            // 手動新增物品欄
            this.manualItemField = new EditBox(this.font, rightX + 150, navY + 1, 130, 18, Component.literal("物品ID"));
            this.manualItemField.setHint(Component.literal("如 diamond / iron_ingot"));
            this.addRenderableWidget(this.manualItemField);

            this.addRenderableWidget(Button.builder(Component.literal("＋ 加入"), btn -> {
                String input = manualItemField.getValue().trim();
                if (!input.isEmpty()) {
                    if (!input.contains(":")) {
                        input = "minecraft:" + input;
                    }
                    try {
                        Identifier id = Identifier.parse(input);
                        Item item = BuiltInRegistries.ITEM.getValue(id);
                        if (item != Items.AIR) {
                            ItemStack dummyStack = new ItemStack(item);
                            FilterItemRule newRule = FilterItemRule.fromItemStack(dummyStack, currentProfile.getDefaultMaxCount(), currentProfile.isDefaultMatchNbt());
                            currentProfile.addOrUpdateRule(newRule);
                            config.save();
                            manualItemField.setValue("");
                            init();
                        }
                    } catch (Exception ignored) {}
                }
            }).bounds(rightX + 285, navY, 50, 20).build());
        }

        // ================= 底部：全局設定與關閉 =================
        int bottomY = this.height - 28;

        this.addRenderableWidget(Button.builder(
                Component.literal("開箱自動過濾: " + (config.isAutoFilterOnOpen() ? "§a開" : "§c關")),
                btn -> {
                    config.setAutoFilterOnOpen(!config.isAutoFilterOnOpen());
                    config.save();
                    btn.setMessage(Component.literal("開箱自動過濾: " + (config.isAutoFilterOnOpen() ? "§a開" : "§c關")));
                }
        ).bounds(leftX, bottomY, 130, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("丟棄間隔: " + config.getDropDelayTicks() + " Ticks"),
                btn -> {
                    int next = (config.getDropDelayTicks() + 1) % 4; // 0, 1, 2, 3
                    config.setDropDelayTicks(next);
                    config.save();
                    btn.setMessage(Component.literal("丟棄間隔: " + config.getDropDelayTicks() + " Ticks"));
                }
        ).bounds(leftX + 135, bottomY, 110, 20)
         .tooltip(Tooltip.create(Component.literal("每丟出一件/組物品間隔的 Tick 數（0 為極速，1-2 最安全流暢）")))
         .build());

        this.addRenderableWidget(Button.builder(Component.literal("✔ 完成並返回"), btn -> {
            this.onClose();
        }).bounds(this.width - 110, bottomY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        // 背景調暗
        this.extractMenuBackground(extractor);

        // 頂部大標題
        extractor.centeredText(this.font, "§6§lItemFilter 物品過濾設定中心§r", this.width / 2, 10, 0xFFFFFF);

        int leftPaneWidth = 140;
        int leftX = 10;
        int rightX = leftX + leftPaneWidth + 15;

        // 左側標題
        extractor.text(this.font, "§e設定檔列表§r", leftX, 18, 0xFFFFFF);

        // 右側標題
        ModConfig config = ModConfig.getInstance();
        List<FilterProfile> profiles = config.getProfiles();
        FilterProfile currentProfile = (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size())
                ? profiles.get(selectedProfileIndex) : null;

        if (currentProfile != null) {
            extractor.text(this.font, "§e保留物品清單 - " + currentProfile.getName() + "§r", rightX, 18, 0xFFFFFF);
            extractor.text(this.font, "§7統一數量:§r", rightX, 35, 0xAAAAAA);

            // 渲染分頁資訊
            List<FilterItemRule> rules = currentProfile.getRules();
            int totalPages = Math.max(1, (int) Math.ceil((double) rules.size() / ITEMS_PER_PAGE));
            String pageInfo = String.format("§7第 %d / %d 頁 (共 %d 種物品)§r", currentPage + 1, totalPages, rules.size());
            extractor.text(this.font, pageInfo, rightX + 10, 30 + ITEMS_PER_PAGE * 24 + 28, 0xAAAAAA);

            // 渲染當前頁面的物品圖示與名稱
            int rightY = 58;
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(rules.size(), startIndex + ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                FilterItemRule rule = rules.get(i);
                int rowY = rightY + (i - startIndex) * 24;

                ItemStack iconStack = ItemStack.EMPTY;
                try {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(rule.getItemId()));
                    if (item != Items.AIR) {
                        iconStack = new ItemStack(item);
                    }
                } catch (Exception ignored) {}

                if (!iconStack.isEmpty()) {
                    extractor.fakeItem(iconStack, rightX, rowY + 1);
                }

                String displayName = rule.getDisplayName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = rule.getItemId();
                }
                if (displayName.length() > 9) {
                    displayName = displayName.substring(0, 8) + "…";
                }
                extractor.text(this.font, displayName, rightX + 22, rowY + 5, 0xFFFFFF);
            }
        }

        super.extractRenderState(extractor, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        ModConfig.getInstance().save();
        if (this.parentScreen != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
