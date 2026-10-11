package com.shumsic.itemfilter.config;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 代表一個命名的物品過濾設定檔（如「土希拉」）
 */
public class FilterProfile {
    private String id;
    private String name;
    private int defaultMaxCount;
    private boolean defaultMatchNbt;
    private List<FilterItemRule> rules;

    public FilterProfile() {
        this.id = UUID.randomUUID().toString();
        this.name = "未命名設定檔";
        this.defaultMaxCount = 64;
        this.defaultMatchNbt = false;
        this.rules = new ArrayList<>();
    }

    public FilterProfile(String name) {
        this();
        this.name = (name != null && !name.trim().isEmpty()) ? name : "未命名設定檔";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDefaultMaxCount() {
        return defaultMaxCount;
    }

    public void setDefaultMaxCount(int defaultMaxCount) {
        this.defaultMaxCount = defaultMaxCount;
    }

    public boolean isDefaultMatchNbt() {
        return defaultMatchNbt;
    }

    public void setDefaultMatchNbt(boolean defaultMatchNbt) {
        this.defaultMatchNbt = defaultMatchNbt;
    }

    // 記憶體快取索引（不進行 GSON 序列化）
    private transient java.util.Map<String, List<FilterItemRule>> ruleIndexByItemId;

    public void invalidateCache() {
        this.ruleIndexByItemId = null;
    }

    private void ensureIndex() {
        if (ruleIndexByItemId != null) return;
        java.util.Map<String, List<FilterItemRule>> index = new java.util.HashMap<>();
        if (rules != null) {
            for (FilterItemRule rule : rules) {
                if (rule != null && rule.getItemId() != null) {
                    index.computeIfAbsent(rule.getItemId(), k -> new ArrayList<>()).add(rule);
                }
            }
        }
        this.ruleIndexByItemId = index;
    }

    public List<FilterItemRule> getRules() {
        if (rules == null) {
            rules = new ArrayList<>();
        }
        return rules;
    }

    public void setRules(List<FilterItemRule> rules) {
        this.rules = rules;
        invalidateCache();
    }

    /**
     * 尋找匹配特定 ItemStack 的規則（利用 Item ID 索引達成 O(1) 分組快速比對）
     */
    public FilterItemRule findRule(ItemStack stack) {
        if (stack == null || stack.isEmpty() || rules == null) {
            return null;
        }
        ensureIndex();

        net.minecraft.resources.Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return null;
        String idStr = itemId.toString();

        List<FilterItemRule> candidates = ruleIndexByItemId.get(idStr);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        for (FilterItemRule rule : candidates) {
            if (rule.matches(stack, idStr)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 新增或更新規則（若已存在同物品與比對模式，則更新數量）
     */
    public void addOrUpdateRule(FilterItemRule newRule) {
        if (newRule == null) return;
        if (rules == null) {
            rules = new ArrayList<>();
        }
        for (int i = 0; i < rules.size(); i++) {
            FilterItemRule existing = rules.get(i);
            if (existing.equals(newRule)) {
                existing.setMaxCount(newRule.getMaxCount());
                existing.setDisplayName(newRule.getDisplayName());
                invalidateCache();
                return;
            }
        }
        rules.add(newRule);
        invalidateCache();
    }

    /**
     * 統一設定數量上限，一併覆蓋所有已存在的物品規則
     */
    public void applyUniformMaxCount(int uniformCount) {
        this.defaultMaxCount = uniformCount;
        if (rules != null) {
            for (FilterItemRule rule : rules) {
                rule.setMaxCount(uniformCount);
            }
        }
        invalidateCache();
    }

    /**
     * 統一切換 NBT / 資料組件比對模式
     */
    public void applyUniformMatchNbt(boolean matchNbt) {
        this.defaultMatchNbt = matchNbt;
        if (rules != null) {
            for (FilterItemRule rule : rules) {
                rule.setMatchNbt(matchNbt);
            }
        }
        invalidateCache();
    }
}
