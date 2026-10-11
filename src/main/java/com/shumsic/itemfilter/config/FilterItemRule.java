package com.shumsic.itemfilter.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 代表單一物品保留規則
 */
public class FilterItemRule {
    /** 物品識別碼，例如 "minecraft:diamond" */
    private String itemId;
    /** 保留上限數量（超過即丟出；-1 代表無上限） */
    private int maxCount;
    /** 是否比對 NBT / DataComponents 資料組件 */
    private boolean matchNbt;
    /** 序列化的組件補丁字串（當 matchNbt 為 true 時比對） */
    private String componentsPatchString;
    /** 顯示名稱（方便 UI 檢視） */
    private String displayName;

    public FilterItemRule() {
        this.itemId = "";
        this.maxCount = 64;
        this.matchNbt = false;
        this.componentsPatchString = "";
        this.displayName = "";
    }

    public FilterItemRule(String itemId, int maxCount, boolean matchNbt, String componentsPatchString, String displayName) {
        this.itemId = itemId != null ? itemId : "";
        this.maxCount = maxCount;
        this.matchNbt = matchNbt;
        this.componentsPatchString = componentsPatchString != null ? componentsPatchString : "";
        this.displayName = displayName != null ? displayName : "";
    }

    /**
     * 從實際 ItemStack 快速建立規則
     */
    public static FilterItemRule fromItemStack(ItemStack stack, int maxCount, boolean matchNbt) {
        if (stack.isEmpty()) {
            return new FilterItemRule();
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String patchStr = stack.getComponentsPatch().isEmpty() ? "" : stack.getComponentsPatch().toString();
        String name = stack.getHoverName().getString();
        return new FilterItemRule(id, maxCount, matchNbt, patchStr, name);
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
        if (this.matchNbt) {
            String currentPatchStr = stack.getComponentsPatch().isEmpty() ? "" : stack.getComponentsPatch().toString();
            return Objects.equals(this.componentsPatchString, currentPatchStr);
        }
        return true;
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

    public boolean isMatchNbt() {
        return matchNbt;
    }

    public void setMatchNbt(boolean matchNbt) {
        this.matchNbt = matchNbt;
    }

    public String getComponentsPatchString() {
        return componentsPatchString;
    }

    public void setComponentsPatchString(String componentsPatchString) {
        this.componentsPatchString = componentsPatchString;
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
        if (matchNbt != that.matchNbt) return false;
        if (!Objects.equals(itemId, that.itemId)) return false;
        return !matchNbt || Objects.equals(componentsPatchString, that.componentsPatchString);
    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        result = 31 * result + (matchNbt ? 1 : 0);
        if (matchNbt && componentsPatchString != null) {
            result = 31 * result + componentsPatchString.hashCode();
        }
        return result;
    }
}
