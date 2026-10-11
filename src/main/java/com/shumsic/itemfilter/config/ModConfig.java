package com.shumsic.itemfilter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品過濾模組設定中心與磁碟存取
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;

    /** 總開關 */
    private boolean filterEnabled = true;
    /** 當前啟用 / 鎖定的設定檔 ID */
    private String activeProfileId = "";
    /** 開啟容器時是否自動執行過濾 */
    private boolean autoFilterOnOpen = true;
    /** 丟出物品間隔 Ticks（預設 1 tick，避免伺服器防作弊或封包堵塞） */
    private int dropDelayTicks = 1;
    /** 所有已建立的設定檔 */
    private List<FilterProfile> profiles = new ArrayList<>();

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("itemfilter.json").toFile();
    }

    public static ModConfig load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    config.ensureDefaults();
                    return config;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.ensureDefaults();
        config.save();
        return config;
    }

    public void save() {
        saveAsync();
    }

    /**
     * 非同步將設定檔序列化並寫入磁碟，避免阻塞主遊戲/渲染執行緒
     */
    public void saveAsync() {
        java.util.concurrent.CompletableFuture.runAsync(this::saveNow);
    }

    /**
     * 同步立即將設定檔寫入磁碟（用於關閉遊戲、關閉視窗等需要確保落盤的時機）
     */
    public synchronized void saveNow() {
        File file = getConfigFile();
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ensureDefaults() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        if (profiles.isEmpty()) {
            FilterProfile defaultProfile = new FilterProfile("預設過濾");
            profiles.add(defaultProfile);
            activeProfileId = defaultProfile.getId();
        } else if (activeProfileId == null || activeProfileId.isEmpty() || getProfileById(activeProfileId) == null) {
            activeProfileId = profiles.get(0).getId();
        }
    }

    public FilterProfile getActiveProfile() {
        if (activeProfileId == null || activeProfileId.isEmpty()) {
            return null;
        }
        return getProfileById(activeProfileId);
    }

    public FilterProfile getProfileById(String id) {
        if (id == null || profiles == null) return null;
        for (FilterProfile profile : profiles) {
            if (id.equals(profile.getId())) {
                return profile;
            }
        }
        return null;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public void setActiveProfileId(String activeProfileId) {
        this.activeProfileId = activeProfileId != null ? activeProfileId : "";
    }

    public boolean isAutoFilterOnOpen() {
        return autoFilterOnOpen;
    }

    public void setAutoFilterOnOpen(boolean autoFilterOnOpen) {
        this.autoFilterOnOpen = autoFilterOnOpen;
    }

    public int getDropDelayTicks() {
        return dropDelayTicks;
    }

    public void setDropDelayTicks(int dropDelayTicks) {
        this.dropDelayTicks = Math.max(0, Math.min(20, dropDelayTicks));
    }

    public List<FilterProfile> getProfiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        return profiles;
    }

    public void setProfiles(List<FilterProfile> profiles) {
        this.profiles = profiles;
    }
}
