package com.shumsic.itemfilter;

import com.shumsic.itemfilter.config.FilterItemRule;
import com.shumsic.itemfilter.config.FilterProfile;
import com.shumsic.itemfilter.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModConfigTest {

    @Test
    void testModConfigDefaults() {
        ModConfig config = new ModConfig();
        config.ensureDefaults();

        assertTrue(config.isFilterEnabled());
        assertTrue(config.isAutoFilterOnOpen());
        assertEquals(1, config.getDropDelayTicks());
        assertFalse(config.getProfiles().isEmpty());
        assertNotNull(config.getActiveProfile());
        assertEquals("預設過濾", config.getActiveProfile().getName());
    }

    @Test
    void testProfileSwitching() {
        ModConfig config = new ModConfig();
        config.ensureDefaults();

        FilterProfile p1 = new FilterProfile("土希拉");
        p1.addOrUpdateRule(new FilterItemRule("minecraft:cobblestone", 64, false, "", "鵝卵石"));
        config.getProfiles().add(p1);

        config.setActiveProfileId(p1.getId());
        assertEquals(p1.getId(), config.getActiveProfile().getId());
        assertEquals("土希拉", config.getActiveProfile().getName());
    }
}
