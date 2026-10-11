package com.shumsic.itemfilter;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterProfileTest {

    @Test
    void testProfileCreationAndRuleManagement() {
        FilterProfile profile = new FilterProfile("土希拉");
        assertEquals("土希拉", profile.getName());
        assertNotNull(profile.getId());
        assertEquals(0, profile.getRules().size());

        FilterItemRule iron = new FilterItemRule("minecraft:iron_ingot", 64, false, "", "鐵錠");
        FilterItemRule gold = new FilterItemRule("minecraft:gold_ingot", 32, false, "", "金錠");

        profile.addOrUpdateRule(iron);
        profile.addOrUpdateRule(gold);
        assertEquals(2, profile.getRules().size());

        // 再次加入相同物品，更新數量
        FilterItemRule ironUpdate = new FilterItemRule("minecraft:iron_ingot", 128, false, "", "鐵錠");
        profile.addOrUpdateRule(ironUpdate);
        assertEquals(2, profile.getRules().size());
        assertEquals(128, profile.getRules().get(0).getMaxCount());
    }

    @Test
    void testUniformMaxCountReplacement() {
        FilterProfile profile = new FilterProfile("土希拉");
        profile.addOrUpdateRule(new FilterItemRule("minecraft:dirt", 64, false, "", "泥土"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:cobblestone", 128, false, "", "鵝卵石"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:oak_log", 32, false, "", "原木"));

        // 統一設定數量上限為 16（取代下去）
        profile.applyUniformMaxCount(16);
        assertEquals(16, profile.getDefaultMaxCount());
        for (FilterItemRule rule : profile.getRules()) {
            assertEquals(16, rule.getMaxCount());
        }
    }

    @Test
    void testUniformMatchNbtToggle() {
        FilterProfile profile = new FilterProfile("測試設定檔");
        profile.addOrUpdateRule(new FilterItemRule("minecraft:sword", 1, false, "", "劍"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:bow", 1, false, "", "弓"));

        profile.applyUniformMatchNbt(true);
        assertTrue(profile.isDefaultMatchNbt());
        for (FilterItemRule rule : profile.getRules()) {
            assertTrue(rule.isMatchNbt());
        }

        profile.applyUniformMatchNbt(false);
        assertFalse(profile.isDefaultMatchNbt());
        for (FilterItemRule rule : profile.getRules()) {
            assertFalse(rule.isMatchNbt());
        }
    }
}
