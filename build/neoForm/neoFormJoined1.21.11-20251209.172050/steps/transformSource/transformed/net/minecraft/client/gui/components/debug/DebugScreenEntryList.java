package net.minecraft.client.gui.components.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DebugScreenEntryList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_DEBUG_PROFILE_VERSION = 4649;
    private Map<Identifier, DebugScreenEntryStatus> allStatuses;
    private final List<Identifier> currentlyEnabled = new ArrayList<>();
    private boolean isOverlayVisible = false;
    private @Nullable DebugScreenProfile profile;
    private final File debugProfileFile;
    private long currentlyEnabledVersion;
    private final Codec<DebugScreenEntryList.SerializedOptions> codec;

    public DebugScreenEntryList(File gameDirectory) {
        this.debugProfileFile = new File(gameDirectory, "debug-profile.json");
        this.codec = DataFixTypes.DEBUG_PROFILE.wrapCodec(DebugScreenEntryList.SerializedOptions.CODEC, Minecraft.getInstance().getFixerUpper(), 4649);
        this.load();
    }

    public void load() {
        try {
            if (!this.debugProfileFile.isFile()) {
                this.loadDefaultProfile();
                this.rebuildCurrentList();
                return;
            }

            Dynamic<JsonElement> dynamic = new Dynamic<>(
                JsonOps.INSTANCE, StrictJsonParser.parse(FileUtils.readFileToString(this.debugProfileFile, StandardCharsets.UTF_8))
            );
            DebugScreenEntryList.SerializedOptions debugscreenentrylist$serializedoptions = this.codec
                .parse(dynamic)
                .getOrThrow(p_434679_ -> new IOException("Could not parse debug profile JSON: " + p_434679_));
            if (debugscreenentrylist$serializedoptions.profile().isPresent()) {
                this.loadProfile(debugscreenentrylist$serializedoptions.profile().get());
            } else {
                this.allStatuses = new HashMap<>();
                if (debugscreenentrylist$serializedoptions.custom().isPresent()) {
                    this.allStatuses.putAll(debugscreenentrylist$serializedoptions.custom().get());
                }

                this.profile = null;
            }
        } catch (JsonSyntaxException | IOException ioexception) {
            LOGGER.error("Couldn't read debug profile file {}, resetting to default", this.debugProfileFile, ioexception);
            this.loadDefaultProfile();
            this.save();
        }

        this.rebuildCurrentList();
    }

    public void loadProfile(DebugScreenProfile profile) {
        this.profile = profile;
        Map<Identifier, DebugScreenEntryStatus> map = DebugScreenEntries.PROFILES.get(profile);
        this.allStatuses = new HashMap<>(map);
        this.rebuildCurrentList();
    }

    private void loadDefaultProfile() {
        this.profile = DebugScreenProfile.DEFAULT;
        this.allStatuses = new HashMap<>(DebugScreenEntries.PROFILES.get(DebugScreenProfile.DEFAULT));
    }

    public DebugScreenEntryStatus getStatus(Identifier entry) {
        DebugScreenEntryStatus debugscreenentrystatus = this.allStatuses.get(entry);
        return debugscreenentrystatus == null ? DebugScreenEntryStatus.NEVER : debugscreenentrystatus;
    }

    public boolean isCurrentlyEnabled(Identifier entry) {
        return this.currentlyEnabled.contains(entry);
    }

    public void setStatus(Identifier entry, DebugScreenEntryStatus status) {
        this.profile = null;
        this.allStatuses.put(entry, status);
        this.rebuildCurrentList();
        this.save();
    }

    public boolean toggleStatus(Identifier location) {
        switch ((DebugScreenEntryStatus)this.allStatuses.get(location)) {
            case ALWAYS_ON:
                this.setStatus(location, DebugScreenEntryStatus.NEVER);
                return false;
            case IN_OVERLAY:
                if (this.isOverlayVisible) {
                    this.setStatus(location, DebugScreenEntryStatus.NEVER);
                    return false;
                }

                this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
                return true;
            case NEVER:
                if (this.isOverlayVisible) {
                    this.setStatus(location, DebugScreenEntryStatus.IN_OVERLAY);
                } else {
                    this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
                }

                return true;
            case null:
            default:
                this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
                return true;
        }
    }

    public Collection<Identifier> getCurrentlyEnabled() {
        return this.currentlyEnabled;
    }

    public void toggleDebugOverlay() {
        this.setOverlayVisible(!this.isOverlayVisible);
    }

    public void setOverlayVisible(boolean overlayVisible) {
        if (this.isOverlayVisible != overlayVisible) {
            this.isOverlayVisible = overlayVisible;
            this.rebuildCurrentList();
        }
    }

    public boolean isOverlayVisible() {
        return this.isOverlayVisible;
    }

    public void rebuildCurrentList() {
        this.currentlyEnabled.clear();
        boolean flag = Minecraft.getInstance().showOnlyReducedInfo();

        for (Entry<Identifier, DebugScreenEntryStatus> entry : this.allStatuses.entrySet()) {
            if (entry.getValue() == DebugScreenEntryStatus.ALWAYS_ON || this.isOverlayVisible && entry.getValue() == DebugScreenEntryStatus.IN_OVERLAY) {
                DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(entry.getKey());
                if (debugscreenentry != null && debugscreenentry.isAllowed(flag)) {
                    this.currentlyEnabled.add(entry.getKey());
                }
            }
        }

        // Neo: Sort enabled debug entries to match order displayed in config screen
        this.currentlyEnabled.sort(net.neoforged.neoforge.common.CommonHooks.CMP_BY_NAMESPACE_VANILLA_FIRST);
        this.currentlyEnabledVersion++;
    }

    public long getCurrentlyEnabledVersion() {
        return this.currentlyEnabledVersion;
    }

    public boolean isUsingProfile(DebugScreenProfile profile) {
        return this.profile == profile;
    }

    public void save() {
        DebugScreenEntryList.SerializedOptions debugscreenentrylist$serializedoptions = new DebugScreenEntryList.SerializedOptions(
            Optional.ofNullable(this.profile), this.profile == null ? Optional.of(this.allStatuses) : Optional.empty()
        );

        try {
            FileUtils.writeStringToFile(
                this.debugProfileFile,
                this.codec.encodeStart(JsonOps.INSTANCE, debugscreenentrylist$serializedoptions).getOrThrow().toString(),
                StandardCharsets.UTF_8
            );
        } catch (IOException ioexception) {
            LOGGER.error("Failed to save debug profile file {}", this.debugProfileFile, ioexception);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record SerializedOptions(Optional<DebugScreenProfile> profile, Optional<Map<Identifier, DebugScreenEntryStatus>> custom) {
        private static final Codec<Map<Identifier, DebugScreenEntryStatus>> CUSTOM_ENTRIES_CODEC = Codec.unboundedMap(
            Identifier.CODEC, DebugScreenEntryStatus.CODEC
        );
        public static final Codec<DebugScreenEntryList.SerializedOptions> CODEC = RecordCodecBuilder.create(
            p_434734_ -> p_434734_.group(
                    DebugScreenProfile.CODEC.optionalFieldOf("profile").forGetter(DebugScreenEntryList.SerializedOptions::profile),
                    CUSTOM_ENTRIES_CODEC.optionalFieldOf("custom").forGetter(DebugScreenEntryList.SerializedOptions::custom)
                )
                .apply(p_434734_, DebugScreenEntryList.SerializedOptions::new)
        );
    }
}
