package com.shumsic.itemfilter;

import com.shumsic.itemfilter.config.FilterItemRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterItemRuleTest {

    @Test
    void testRuleEqualityAndProperties() {
        FilterItemRule rule1 = new FilterItemRule("minecraft:diamond", 64, false, "", "鑽石");
        FilterItemRule rule2 = new FilterItemRule("minecraft:diamond", 32, false, "", "鑽石");

        // 當忽略 NBT 時，同物品 ID 視為相同規則（可更新數量）
        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertEquals("minecraft:diamond", rule1.getItemId());
        assertEquals(64, rule1.getMaxCount());
        assertFalse(rule1.isMatchNbt());
    }

    @Test
    void testRuleWithNbt() {
        FilterItemRule ruleWithNbt1 = new FilterItemRule("minecraft:potion", 1, true, "{potion:'minecraft:healing'}", "治療藥水");
        FilterItemRule ruleWithNbt2 = new FilterItemRule("minecraft:potion", 1, true, "{potion:'minecraft:healing'}", "治療藥水");
        FilterItemRule ruleWithDifferentNbt = new FilterItemRule("minecraft:potion", 1, true, "{potion:'minecraft:poison'}", "劇毒藥水");

        assertEquals(ruleWithNbt1, ruleWithNbt2);
        assertNotEquals(ruleWithNbt1, ruleWithDifferentNbt);
    }
}
