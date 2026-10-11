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

    private static final java.util.concurrent.ScheduledExecutorService DEBOUNCE_EXECUTOR = 
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ItemFilter-ConfigWriter");
                t.setDaemon(true);
                return t;
            });
    private static java.util.concurrent.ScheduledFuture<?> pendingSaveTask;
    private static final Object SAVE_LOCK = new Object();

    public void save() {
        saveAsync();
    }

    /**
     * 500ms 防抖非同步寫入磁碟，避免頻繁點擊介面造成磁碟 I/O 洪流與主執行緒卡頓
     */
    public void saveAsync() {
        synchronized (SAVE_LOCK) {
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
                pendingSaveTask.cancel(false);
            }
            pendingSaveTask = DEBOUNCE_EXECUTOR.schedule(this::saveNow, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 立即將設定檔寫入磁碟（使用 BufferedWriter 與暫存檔原子替換，避免檔案損壞與未緩衝 I/O 阻塞）
     */
    public void saveNow() {
        synchronized (SAVE_LOCK) {
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
                pendingSaveTask.cancel(false);
            }
        }

        // 於同步塊內建立 JSON 字串快照，避免背景執行緒序列化時主執行緒正在修改 ArrayList
        String jsonOutput;
        synchronized (this) {
            jsonOutput = GSON.toJson(this);
        }

        File file = getConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(jsonOutput);
            }
            java.nio.file.Files.move(tempFile.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                ex.printStackTrace();
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
