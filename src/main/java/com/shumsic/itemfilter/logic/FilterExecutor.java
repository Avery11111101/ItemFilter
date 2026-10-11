package com.shumsic.itemfilter.logic;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 處理容器過濾、數量保留、超額與非白名單物品丟出（模擬玩家按 Q / Ctrl+Q 丟棄）
 */
public class FilterExecutor {
    private static final FilterExecutor INSTANCE = new FilterExecutor();

    public static FilterExecutor getInstance() {
        return INSTANCE;
    }

    /** 單次容器介面互動動作 */
    public static class WindowAction {
        public final int containerId;
        public final int slotIndex;
        public final int button;
        public final ContainerInput actionType;

        public WindowAction(int containerId, int slotIndex, int button, ContainerInput actionType) {
            this.containerId = containerId;
            this.slotIndex = slotIndex;
            this.button = button;
            this.actionType = actionType;
        }

        public static WindowAction dropStack(int containerId, int slotIndex) {
            return new WindowAction(containerId, slotIndex, 1, ContainerInput.THROW); // Ctrl+Q 整組丟棄
        }

        public static WindowAction dropSingle(int containerId, int slotIndex) {
            return new WindowAction(containerId, slotIndex, 0, ContainerInput.THROW); // Q 單顆丟棄
        }
    }

    private final Queue<WindowAction> actionQueue = new ArrayDeque<>();
    private int tickCounter = 0;
    private int ticksSinceScreenOpen = 0;
    private boolean autoFilterPending = false;

    /**
     * 檢查給定的畫面是否屬於容器（箱子、界伏盒、木桶、漏斗、發射器等）
     */
    public boolean isContainerScreen(AbstractContainerScreen<?> screen) {
        if (screen == null) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;

        // 忽略未處於有效連線狀態的情況
        if (client.getConnection() == null) return false;

        AbstractContainerMenu menu = screen.getMenu();
        if (menu == null) return false;

        // 排除常見的伺服器菜單與非一般實體容器（比對標題關鍵字）
        Component title = screen.getTitle();
        if (title != null) {
            String titleStr = title.getString().toLowerCase();
            if (titleStr.contains("menu") || titleStr.contains("選單") || titleStr.contains("菜單") || titleStr.contains("server")) {
                return false;
            }
        }

        for (Slot slot : menu.slots) {
            if (slot.container != client.player.getInventory()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 當容器畫面開啟時呼叫，重置延遲計時並排程自動過濾
     */
    public void onScreenOpened(AbstractContainerScreen<?> screen) {
        ticksSinceScreenOpen = 0;
        actionQueue.clear();
        ModConfig config = ModConfig.getInstance();
        if (config.isFilterEnabled() && config.isAutoFilterOnOpen()) {
            autoFilterPending = true;
        }
    }

    /**
     * 當容器關閉時清理
     */
    public void onScreenClosed() {
        actionQueue.clear();
        autoFilterPending = false;
    }

    /**
     * 每客戶端 Tick 執行排程與丟棄封包傳送
     */
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.gui == null) {
            actionQueue.clear();
            return;
        }

        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> containerScreen) || !isContainerScreen(containerScreen)) {
            actionQueue.clear();
            autoFilterPending = false;
            return;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();
        if (menu == null) {
            actionQueue.clear();
            return;
        }

        // 自動過濾延遲 3 ticks 等待伺服器將容器內容完全同步至客戶端
        if (autoFilterPending) {
            ticksSinceScreenOpen++;
            if (ticksSinceScreenOpen >= 3) {
                autoFilterPending = false;
                executeFilter(containerScreen, false);
            }
        }

        // 處理視窗互動與丟棄佇列
        if (!actionQueue.isEmpty()) {
            ModConfig config = ModConfig.getInstance();
            int delay = config.getDropDelayTicks();

            if (delay <= 0) {
                // 極速模式：每 tick 最多執行 10 個動作，避免瞬時 packet flood 斷線
                int actionsThisTick = 0;
                while (!actionQueue.isEmpty() && actionsThisTick < 10) {
                    WindowAction action = actionQueue.poll();
                    if (action.containerId == menu.containerId) {
                        client.gameMode.handleContainerInput(action.containerId, action.slotIndex, action.button, action.actionType, client.player);
                        actionsThisTick++;
                    }
                }
            } else {
                tickCounter++;
                if (tickCounter >= delay) {
                    tickCounter = 0;
                    WindowAction action = actionQueue.poll();
                    if (action != null && action.containerId == menu.containerId) {
                        client.gameMode.handleContainerInput(action.containerId, action.slotIndex, action.button, action.actionType, client.player);
                    }
                }
            }
        }
    }

    /**
     * 執行過濾：比對設定檔規則，計算多餘與超額物品並加入丟棄佇列
     * @param screen 當前容器畫面
     * @param isManual 是否為手動點擊觸發（手動觸發會給予提示音或提示訊息）
     */
    public void executeFilter(AbstractContainerScreen<?> screen, boolean isManual) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu == null) return;

        ModConfig config = ModConfig.getInstance();
        FilterProfile activeProfile = config.getActiveProfile();
        if (activeProfile == null) {
            if (isManual) {
                client.player.sendOverlayMessage(Component.translatable("itemfilter.message.no_active_profile"));
            }
            return;
        }

        // 容器槽位清單（不包含玩家身上的物品槽）
        List<Slot> containerSlots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container != client.player.getInventory()) {
                containerSlots.add(slot);
            }
        }

        if (containerSlots.isEmpty()) return;

        Map<FilterItemRule, Integer> retainedCountMap = new HashMap<>();
        int totalDroppedItems = 0;

        for (Slot slot : containerSlots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            FilterItemRule matchedRule = activeProfile.findRule(stack);
            int slotCount = stack.getCount();

            if (matchedRule == null) {
                // 不在白名單中：整格丟出！
                actionQueue.add(WindowAction.dropStack(menu.containerId, slot.index)); // 整組丟出 (Ctrl+Q)
                totalDroppedItems += slotCount;
            } else {
                int maxAllowed = matchedRule.getMaxCount();
                if (maxAllowed < 0) {
                    // 無上限保留
                    continue;
                }

                int alreadyKept = retainedCountMap.getOrDefault(matchedRule, 0);
                int spaceLeft = Math.max(0, maxAllowed - alreadyKept);
                int keepFromSlot = Math.min(slotCount, spaceLeft);
                int dropFromSlot = slotCount - keepFromSlot;

                retainedCountMap.put(matchedRule, alreadyKept + keepFromSlot);

                if (dropFromSlot == slotCount) {
                    // 整疊都超出上限：安全整組拋出 (Ctrl+Q)
                    actionQueue.add(WindowAction.dropStack(menu.containerId, slot.index));
                    totalDroppedItems += slotCount;
                } else if (dropFromSlot > 0) {
                    // 部分超出上限：僅精確丟出 dropFromSlot 數量，絕不能呼叫 dropStack
                    for (int i = 0; i < dropFromSlot; i++) {
                        actionQueue.add(WindowAction.dropSingle(menu.containerId, slot.index));
                    }
                    totalDroppedItems += dropFromSlot;
                }
            }
        }

        if (totalDroppedItems > 0 || isManual) {
            String msg = totalDroppedItems > 0
                    ? String.format("§a[物品過濾] 設定檔「%s」：已丟出 %d 件多餘物品！", activeProfile.getName(), totalDroppedItems)
                    : String.format("§e[物品過濾] 設定檔「%s」：容器符合規則，無多餘物品。", activeProfile.getName());
            client.player.sendOverlayMessage(Component.literal(msg));
        }
    }

    /**
     * 從目前容器將所有不重複的物品捕獲並加入目前設定檔
     */
    public void captureFromContainer(AbstractContainerScreen<?> screen) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu == null) return;

        ModConfig config = ModConfig.getInstance();
        FilterProfile activeProfile = config.getActiveProfile();
        if (activeProfile == null) {
            client.player.sendOverlayMessage(Component.literal("§c[物品過濾] 請先選取或建立一個設定檔！"));
            return;
        }

        // 收集容器槽位
        Map<String, ItemStack> capturedItems = new LinkedHashMap<>();
        Map<String, Integer> itemCounts = new LinkedHashMap<>();

        for (Slot slot : menu.slots) {
            if (slot.container != client.player.getInventory()) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    String key = activeProfile.isDefaultMatchNbt()
                            ? stack.getItem().toString() + "_" + (stack.getComponentsPatch().isEmpty() ? "" : stack.getComponentsPatch().toString())
                            : stack.getItem().toString();

                    capturedItems.putIfAbsent(key, stack.copy());
                    itemCounts.put(key, itemCounts.getOrDefault(key, 0) + stack.getCount());
                }
            }
        }

        if (capturedItems.isEmpty()) {
            client.player.sendOverlayMessage(Component.literal("§e[物品過濾] 當前容器是空的，無法擷取！"));
            return;
        }

        int addedCount = 0;
        for (Map.Entry<String, ItemStack> entry : capturedItems.entrySet()) {
            ItemStack stack = entry.getValue();
            int totalInContainer = itemCounts.getOrDefault(entry.getKey(), 64);
            // 預設將設定檔預設值（包含 -1 無上限）或箱子內當前物品總數量作為上限
            int defaultMax = activeProfile.getDefaultMaxCount();
            int maxCount = (defaultMax == -1 || defaultMax > 0) ? defaultMax : totalInContainer;

            FilterItemRule rule = FilterItemRule.fromItemStack(stack, maxCount, activeProfile.isDefaultMatchNbt());
            activeProfile.addOrUpdateRule(rule);
            addedCount++;
        }

        config.save();
        client.player.sendOverlayMessage(
                Component.literal(String.format("§a[物品過濾] 已將此容器中 %d 種物品導入設定檔「%s」！", addedCount, activeProfile.getName()))
        );
    }
}
