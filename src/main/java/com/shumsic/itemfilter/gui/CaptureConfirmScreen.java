package com.shumsic.itemfilter.gui;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.MatchMode;
import com.shumsic.itemfilter.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 提取/擷取防誤觸確認介面：展示即將導入設定檔的物品，提供標準 Tooltip、NBT 細節、單項與全部確認加入
 */
public class CaptureConfirmScreen extends Screen {
    private final Screen parentScreen;
    private final FilterProfile targetProfile;
    private final List<CaptureCandidate> candidates;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    public static class CaptureCandidate {
        public final ItemStack stack;
        public final int defaultMaxCount;
        public final MatchMode matchMode;
        public final boolean matchNbt;
        public final String itemId;
        public final String displayName;
        public final String nbtPreview;

        public CaptureCandidate(ItemStack stack, int defaultMaxCount, MatchMode matchMode) {
            this.stack = stack.copy();
            this.defaultMaxCount = defaultMaxCount;
            this.matchMode = matchMode != null ? matchMode : MatchMode.ITEM_ONLY;
            this.matchNbt = this.matchMode != MatchMode.ITEM_ONLY;
            this.itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            this.displayName = stack.getHoverName().getString();

            if (this.matchMode == MatchMode.ENCHANTMENTS) {
                String ench = FilterItemRule.extractEnchantmentsString(stack);
                this.nbtPreview = ench.isEmpty() ? "無附魔" : ench;
            } else {
                DataComponentPatch patch = stack.getComponentsPatch();
                if (patch.isEmpty()) {
                    this.nbtPreview = "無特殊組件 (預設)";
                } else {
                    String str = patch.toString();
                    this.nbtPreview = (str.length() > 60) ? str.substring(0, 57) + "..." : str;
                }
            }
        }

        public CaptureCandidate(ItemStack stack, int defaultMaxCount, boolean matchNbt) {
            this(stack, defaultMaxCount, matchNbt ? MatchMode.EXACT : MatchMode.ITEM_ONLY);
        }
    }

    public CaptureConfirmScreen(Screen parentScreen, FilterProfile targetProfile, List<CaptureCandidate> candidates) {
        super(Component.literal("確認導入物品規則"));
        this.parentScreen = parentScreen;
        this.targetProfile = targetProfile;
        this.candidates = new ArrayList<>(candidates);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int totalPages = Math.max(1, (int) Math.ceil((double) candidates.size() / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int centerX = this.width / 2;
        int listWidth = Math.min(360, this.width - 40);
        int listLeft = centerX - listWidth / 2;
        int startY = 50;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, candidates.size());

        for (int i = startIndex; i < endIndex; i++) {
            final int index = i;
            final CaptureCandidate candidate = candidates.get(index);
            int rowY = startY + (i - startIndex) * 26;

            // 單項 [✔ 加入] 按鈕
            this.addRenderableWidget(Button.builder(Component.literal("✔ 加入"), btn -> {
                addCandidateToProfile(candidate);
                candidates.remove(index);
                if (candidates.isEmpty()) {
                    finishAndReturn();
                } else {
                    init();
                }
            }).bounds(listLeft + listWidth - 110, rowY, 52, 20).build());

            // 單項 [✕ 略過] 按鈕
            this.addRenderableWidget(Button.builder(Component.literal("✕ 略過"), btn -> {
                candidates.remove(index);
                if (candidates.isEmpty()) {
                    finishAndReturn();
                } else {
                    init();
                }
            }).bounds(listLeft + listWidth - 54, rowY, 52, 20).build());
        }

        // 底部導航與操作列
        int bottomY = this.height - 35;

        // 分頁按鈕
        this.addRenderableWidget(Button.builder(Component.literal("◀ 上一頁"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                init();
            }
        }).bounds(listLeft, bottomY, 65, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("下一頁 ▶"), btn -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                init();
            }
        }).bounds(listLeft + 70, bottomY, 65, 20).build());

        // 全部加入按鈕
        this.addRenderableWidget(Button.builder(Component.literal("§a✔ 全部確認加入§r"), btn -> {
            for (CaptureCandidate c : candidates) {
                addCandidateToProfile(c);
            }
            candidates.clear();
            finishAndReturn();
        }).bounds(centerX - 10, bottomY, 110, 20).build());

        // 取消按鈕
        this.addRenderableWidget(Button.builder(Component.literal("§c取消§r"), btn -> {
            this.onClose();
        }).bounds(listLeft + listWidth - 75, bottomY, 75, 20).build());
    }

    private void addCandidateToProfile(CaptureCandidate candidate) {
        if (targetProfile == null) return;
        FilterItemRule rule = FilterItemRule.fromItemStack(candidate.stack, candidate.defaultMaxCount, candidate.matchMode);
        targetProfile.addOrUpdateRule(rule);
    }

    private void finishAndReturn() {
        ModConfig.getInstance().save();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(
                    Component.literal("§a[物品過濾] 提取規則已更新至設定檔「" + targetProfile.getName() + "」！")
            );
        }
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        this.extractMenuBackground(extractor);

        int centerX = this.width / 2;
        int listWidth = Math.min(360, this.width - 40);
        int listLeft = centerX - listWidth / 2;

        // 標題與說明
        extractor.centeredText(this.font, "§6§l確認導入物品至設定檔: " + targetProfile.getName() + "§r", centerX, 12, 0xFFFFFF);
        extractor.centeredText(this.font, "§7請確認即將加入的物品。懸浮於物品圖示或名稱可檢視完整 Tooltip 與組件 NBT§r", centerX, 28, 0xAAAAAA);

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, candidates.size());
        int startY = 50;

        ItemStack hoveredStack = ItemStack.EMPTY;
        int hoverTooltipX = mouseX;
        int hoverTooltipY = mouseY;

        for (int i = startIndex; i < endIndex; i++) {
            CaptureCandidate candidate = candidates.get(i);
            int rowY = startY + (i - startIndex) * 26;

            // 繪製行底色
            extractor.fill(listLeft, rowY - 2, listLeft + listWidth, rowY + 22, 0x22FFFFFF);

            // 繪製物品圖示
            if (!candidate.stack.isEmpty()) {
                extractor.fakeItem(candidate.stack, listLeft + 4, rowY + 2);
            }

            // 物品名稱與數量上限 + 模式標籤
            String countText = (candidate.defaultMaxCount == -1) ? "無上限" : String.valueOf(candidate.defaultMaxCount);
            String title = String.format("§f%s §7(上限: %s) [%s]§r", candidate.displayName, countText, candidate.matchMode.getFormattedLabel());
            extractor.text(this.font, title, listLeft + 26, rowY + 2, 0xFFFFFF);

            // NBT / 附魔 / 組件摘要
            String nbtText = (candidate.matchMode == MatchMode.ENCHANTMENTS)
                    ? "§b附魔: " + candidate.nbtPreview + "§r"
                    : "§8組件: " + candidate.nbtPreview + "§r";
            extractor.text(this.font, nbtText, listLeft + 26, rowY + 12, 0x888888);

            // 檢查滑鼠是否懸浮於物品或文字區域，以觸發 Tooltip
            if (mouseX >= listLeft && mouseX <= listLeft + listWidth - 120 && mouseY >= rowY - 2 && mouseY <= rowY + 22) {
                hoveredStack = candidate.stack;
            }
        }

        // 分頁文字
        int totalPages = Math.max(1, (int) Math.ceil((double) candidates.size() / ITEMS_PER_PAGE));
        String pageInfo = String.format("§7第 %d / %d 頁 (共 %d 項待確認)§r", currentPage + 1, totalPages, candidates.size());
        extractor.text(this.font, pageInfo, listLeft + 145, this.height - 30, 0xAAAAAA);

        super.extractRenderState(extractor, mouseX, mouseY, delta);

        // 若有懸浮物品，渲染原生 Tooltip (包含物品標準屬性與組件)
        if (!hoveredStack.isEmpty() && this.minecraft != null) {
            List<Component> tooltip = Screen.getTooltipFromItem(this.minecraft, hoveredStack);
            // 額外附加上 NBT 完整組件字串
            if (!hoveredStack.getComponentsPatch().isEmpty()) {
                tooltip.add(Component.literal("§6[組件 NBT 詳細]§r"));
                String patchStr = hoveredStack.getComponentsPatch().toString();
                // 適當換行避免超出螢幕
                if (patchStr.length() > 80) {
                    for (int s = 0; s < patchStr.length(); s += 80) {
                        int end = Math.min(s + 80, patchStr.length());
                        tooltip.add(Component.literal("§8" + patchStr.substring(s, end) + "§r"));
                    }
                } else {
                    tooltip.add(Component.literal("§8" + patchStr + "§r"));
                }
            } else {
                tooltip.add(Component.literal("§8[無額外 NBT 組件]§r"));
            }
            extractor.setComponentTooltipForNextFrame(this.font, tooltip, hoverTooltipX, hoverTooltipY);
        }
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null && this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
