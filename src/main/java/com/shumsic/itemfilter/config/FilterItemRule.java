package com.shumsic.itemfilter.config;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Objects;

/**
 * 代表單一物品保留規則
 */
public class FilterItemRule {
    /** 物品識別碼，例如 "minecraft:diamond" */
    private String itemId;
    /** 保留上限數量（超過即丟出；-1 代表無上限） */
    private int maxCount;
    /** 三段式比對模式 (預設 ITEM_ONLY) */
    private MatchMode matchMode;
    /** 舊版相容欄位：是否比對 NBT / DataComponents 資料組件 */
    private Boolean matchNbt;
    /** 序列化的組件補丁字串（當 matchMode 為 EXACT 時比對，已剔除耐久損耗） */
    private String componentsPatchString;
    /** 序列化的附魔字串（當 matchMode 為 ENCHANTMENTS 時比對） */
    private String enchantmentsString;
    /** 顯示名稱（方便 UI 檢視） */
    private String displayName;

    public FilterItemRule() {
        this.itemId = "";
        this.maxCount = 64;
        this.matchMode = MatchMode.ITEM_ONLY;
        this.componentsPatchString = "";
        this.enchantmentsString = "";
        this.displayName = "";
    }

    public FilterItemRule(String itemId, int maxCount, MatchMode matchMode, String componentsPatchString, String enchantmentsString, String displayName) {
        this.itemId = itemId != null ? itemId : "";
        this.maxCount = maxCount;
        this.matchMode = matchMode != null ? matchMode : MatchMode.ITEM_ONLY;
        this.componentsPatchString = componentsPatchString != null ? componentsPatchString : "";
        this.enchantmentsString = enchantmentsString != null ? enchantmentsString : "";
        this.displayName = displayName != null ? displayName : "";
    }

    /**
     * 舊版建構子向後相容
     */
    public FilterItemRule(String itemId, int maxCount, boolean matchNbt, String componentsPatchString, String displayName) {
        this(itemId, maxCount, matchNbt ? MatchMode.EXACT : MatchMode.ITEM_ONLY, componentsPatchString, "", displayName);
    }

    /**
     * 從實際 ItemStack 快速建立規則（自動解析附魔與標準化組件補丁，排除耐久損耗）
     */
    public static FilterItemRule fromItemStack(ItemStack stack, int maxCount, MatchMode mode) {
        if (stack == null || stack.isEmpty()) {
            return new FilterItemRule();
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name = stack.getHoverName().getString();
        String patchStr = extractNormalizedPatch(stack);
        String enchStr = extractEnchantmentsString(stack);

        return new FilterItemRule(id, maxCount, mode, patchStr, enchStr, name);
    }

    /**
     * 從實際 ItemStack 快速建立規則 (舊版呼叫點相容)
     */
    public static FilterItemRule fromItemStack(ItemStack stack, int maxCount, boolean matchNbt) {
        return fromItemStack(stack, maxCount, matchNbt ? MatchMode.EXACT : MatchMode.ITEM_ONLY);
    }

    /**
     * 擷取物品的附魔字串（包含標準附魔與附魔書儲存附魔，用於完全一致比對）
     */
    public static String extractEnchantmentsString(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        }
        return (enchants != null && !enchants.isEmpty()) ? enchants.toString() : "";
    }

    /**
     * 擷取去除耐久損耗後的組件補丁字串，避免受損物品與滿耐久物品比對失敗
     */
    public static String extractNormalizedPatch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return "";

        // 使用 DataComponentPatch.forget 排除原版耐久度損耗組件
        DataComponentPatch normalized = patch.forget(type -> Objects.equals(type, DataComponents.DAMAGE));
        return normalized.isEmpty() ? "" : normalized.toString();
    }

    /**
     * 檢查傳入的物品是否符合此規則
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String currentId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return matches(stack, currentId);
    }

    /**
     * 重用已解析的 itemId 進行快速比對，避免重複呼叫 Registry 與 toString()
     */
    public boolean matches(ItemStack stack, String currentId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!Objects.equals(this.itemId, currentId)) {
            return false;
        }

        MatchMode mode = getEffectiveMatchMode();
        switch (mode) {
            case ITEM_ONLY:
                return true;

            case ENCHANTMENTS:
                String currentEnch = extractEnchantmentsString(stack);
                return Objects.equals(this.enchantmentsString, currentEnch);

            case EXACT:
                String currentPatch = extractNormalizedPatch(stack);
                return Objects.equals(this.componentsPatchString, currentPatch);

            default:
                return true;
        }
    }

    /**
     * 取得實際運作的比對模式（相容舊版 matchNbt 欄位）
     */
    public MatchMode getEffectiveMatchMode() {
        if (this.matchMode != null) {
            return this.matchMode;
        }
        if (this.matchNbt != null && this.matchNbt) {
            return MatchMode.EXACT;
        }
        return MatchMode.ITEM_ONLY;
    }

    public MatchMode getMatchMode() {
        return getEffectiveMatchMode();
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
        this.matchNbt = (matchMode != MatchMode.ITEM_ONLY);
    }

    public boolean isMatchNbt() {
        return getEffectiveMatchMode() != MatchMode.ITEM_ONLY;
    }

    public void setMatchNbt(boolean matchNbt) {
        this.matchNbt = matchNbt;
        this.matchMode = matchNbt ? MatchMode.EXACT : MatchMode.ITEM_ONLY;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getComponentsPatchString() {
        return componentsPatchString;
    }

    public void setComponentsPatchString(String componentsPatchString) {
        this.componentsPatchString = componentsPatchString;
    }

    public String getEnchantmentsString() {
        return enchantmentsString;
    }

    public void setEnchantmentsString(String enchantmentsString) {
        this.enchantmentsString = enchantmentsString;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterItemRule that = (FilterItemRule) o;
        MatchMode thisMode = getEffectiveMatchMode();
        MatchMode thatMode = that.getEffectiveMatchMode();
        if (thisMode != thatMode) return false;
        if (!Objects.equals(itemId, that.itemId)) return false;

        switch (thisMode) {
            case ENCHANTMENTS:
                return Objects.equals(enchantmentsString, that.enchantmentsString);
            case EXACT:
                return Objects.equals(componentsPatchString, that.componentsPatchString);
            case ITEM_ONLY:
            default:
                return true;
        }
    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        MatchMode mode = getEffectiveMatchMode();
        result = 31 * result + mode.hashCode();
        if (mode == MatchMode.ENCHANTMENTS && enchantmentsString != null) {
            result = 31 * result + enchantmentsString.hashCode();
        } else if (mode == MatchMode.EXACT && componentsPatchString != null) {
            result = 31 * result + componentsPatchString.hashCode();
        }
        return result;
    }
}
