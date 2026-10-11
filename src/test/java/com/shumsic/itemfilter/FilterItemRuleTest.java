package com.shumsic.itemfilter;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.MatchMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterItemRuleTest {

    @Test
    void testRuleEqualityAndProperties() {
        FilterItemRule rule1 = new FilterItemRule("minecraft:iron_axe", 64, MatchMode.ITEM_ONLY, "", "", "鐵斧");
        FilterItemRule rule2 = new FilterItemRule("minecraft:iron_axe", 32, MatchMode.ITEM_ONLY, "", "", "鐵斧");

        // 當只比對物品種類時，同物品 ID 視為相同規則（可更新數量）
        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertEquals("minecraft:iron_axe", rule1.getItemId());
        assertEquals(64, rule1.getMaxCount());
        assertEquals(MatchMode.ITEM_ONLY, rule1.getMatchMode());
        assertFalse(rule1.isMatchNbt());
    }

    @Test
    void testRuleWithEnchantments() {
        FilterItemRule axeWithEff5 = new FilterItemRule("minecraft:iron_axe", 1, MatchMode.ENCHANTMENTS, "", "minecraft:efficiency 5", "鐵斧 (效率V)");
        FilterItemRule axeWithEff5Copy = new FilterItemRule("minecraft:iron_axe", 1, MatchMode.ENCHANTMENTS, "", "minecraft:efficiency 5", "鐵斧 (效率V)");
        FilterItemRule axeWithUnb3 = new FilterItemRule("minecraft:iron_axe", 1, MatchMode.ENCHANTMENTS, "", "minecraft:unbreaking 3", "鐵斧 (耐久III)");
        FilterItemRule plainAxe = new FilterItemRule("minecraft:iron_axe", 1, MatchMode.ITEM_ONLY, "", "", "普通鐵斧");

        assertEquals(axeWithEff5, axeWithEff5Copy);
        assertNotEquals(axeWithEff5, axeWithUnb3);
        assertNotEquals(axeWithEff5, plainAxe);
    }

    @Test
    void testRuleWithExactComponents() {
        FilterItemRule ruleExact1 = new FilterItemRule("minecraft:potion", 1, MatchMode.EXACT, "{potion:'minecraft:healing'}", "", "治療藥水");
        FilterItemRule ruleExact2 = new FilterItemRule("minecraft:potion", 1, MatchMode.EXACT, "{potion:'minecraft:healing'}", "", "治療藥水");
        FilterItemRule ruleDifferent = new FilterItemRule("minecraft:potion", 1, MatchMode.EXACT, "{potion:'minecraft:poison'}", "", "劇毒藥水");

        assertEquals(ruleExact1, ruleExact2);
        assertNotEquals(ruleExact1, ruleDifferent);
    }
}
