package com.shumsic.itemfilter;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.MatchMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterProfileTest {

    @Test
    void testProfileCreationAndRuleManagement() {
        FilterProfile profile = new FilterProfile("土希拉");
        assertEquals("土希拉", profile.getName());
        assertNotNull(profile.getId());
        assertEquals(0, profile.getRules().size());

        FilterItemRule iron = new FilterItemRule("minecraft:iron_ingot", 64, MatchMode.ITEM_ONLY, "", "", "鐵錠");
        FilterItemRule gold = new FilterItemRule("minecraft:gold_ingot", 32, MatchMode.ITEM_ONLY, "", "", "金錠");

        profile.addOrUpdateRule(iron);
        profile.addOrUpdateRule(gold);
        assertEquals(2, profile.getRules().size());

        // 再次加入相同物品，更新數量
        FilterItemRule ironUpdate = new FilterItemRule("minecraft:iron_ingot", 128, MatchMode.ITEM_ONLY, "", "", "鐵錠");
        profile.addOrUpdateRule(ironUpdate);
        assertEquals(2, profile.getRules().size());
        assertEquals(128, profile.getRules().get(0).getMaxCount());
    }

    @Test
    void testUniformMaxCountReplacement() {
        FilterProfile profile = new FilterProfile("土希拉");
        profile.addOrUpdateRule(new FilterItemRule("minecraft:dirt", 64, MatchMode.ITEM_ONLY, "", "", "泥土"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:cobblestone", 128, MatchMode.ITEM_ONLY, "", "", "鵝卵石"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:oak_log", 32, MatchMode.ITEM_ONLY, "", "", "原木"));

        // 統一設定數量上限為 16（取代下去）
        profile.applyUniformMaxCount(16);
        assertEquals(16, profile.getDefaultMaxCount());
        for (FilterItemRule rule : profile.getRules()) {
            assertEquals(16, rule.getMaxCount());
        }
    }

    @Test
    void testUniformMatchModeToggle() {
        FilterProfile profile = new FilterProfile("測試設定檔");
        profile.addOrUpdateRule(new FilterItemRule("minecraft:iron_axe", 1, MatchMode.ITEM_ONLY, "", "", "鐵斧"));
        profile.addOrUpdateRule(new FilterItemRule("minecraft:bow", 1, MatchMode.ITEM_ONLY, "", "", "弓"));

        profile.applyUniformMatchMode(MatchMode.ENCHANTMENTS);
        assertEquals(MatchMode.ENCHANTMENTS, profile.getDefaultMatchMode());
        for (FilterItemRule rule : profile.getRules()) {
            assertEquals(MatchMode.ENCHANTMENTS, rule.getMatchMode());
        }

        profile.applyUniformMatchMode(MatchMode.EXACT);
        assertEquals(MatchMode.EXACT, profile.getDefaultMatchMode());
        for (FilterItemRule rule : profile.getRules()) {
            assertEquals(MatchMode.EXACT, rule.getMatchMode());
        }

        profile.applyUniformMatchMode(MatchMode.ITEM_ONLY);
        assertEquals(MatchMode.ITEM_ONLY, profile.getDefaultMatchMode());
        for (FilterItemRule rule : profile.getRules()) {
            assertEquals(MatchMode.ITEM_ONLY, rule.getMatchMode());
        }
    }
}
