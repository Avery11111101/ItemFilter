package com.shumsic.itemfilter.config;

/**
 * 物品比對模式
 */
public enum MatchMode {
    /** 僅比對物品種類 (Item ID)，忽略附魔、損壞與組件 */
    ITEM_ONLY("一般", "§7一般§r", "只比對物品種類 (無視附魔與耐久)"),
    /** 僅比對附魔種類與等級 (必須完全一致，忽略耐久損耗與其他組件) */
    ENCHANTMENTS("附魔", "§b附魔§r", "比對附魔種類與等級 (完全一致，無視耐久)"),
    /** 完全精準比對 (比對所有組件，忽略耐久度損耗) */
    EXACT("精確", "§d精確§r", "比對所有組件與屬性 (無視耐久損耗)");

    private final String label;
    private final String formattedLabel;
    private final String description;

    MatchMode(String label, String formattedLabel, String description) {
        this.label = label;
        this.formattedLabel = formattedLabel;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getFormattedLabel() {
        return formattedLabel;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 循環切換到下一個比對模式
     */
    public MatchMode next() {
        switch (this) {
            case ITEM_ONLY:
                return ENCHANTMENTS;
            case ENCHANTMENTS:
                return EXACT;
            case EXACT:
            default:
                return ITEM_ONLY;
        }
    }
}
