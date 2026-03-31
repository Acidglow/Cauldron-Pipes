package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.LegacyTagFixer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jspecify.annotations.Nullable;

public class LegacyStructureDataHandler implements LegacyTagFixer {
    public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
    private static final Map<String, String> CURRENT_TO_LEGACY_MAP = Util.make(Maps.newHashMap(), p_71337_ -> {
        p_71337_.put("Village", "Village");
        p_71337_.put("Mineshaft", "Mineshaft");
        p_71337_.put("Mansion", "Mansion");
        p_71337_.put("Igloo", "Temple");
        p_71337_.put("Desert_Pyramid", "Temple");
        p_71337_.put("Jungle_Pyramid", "Temple");
        p_71337_.put("Swamp_Hut", "Temple");
        p_71337_.put("Stronghold", "Stronghold");
        p_71337_.put("Monument", "Monument");
        p_71337_.put("Fortress", "Fortress");
        p_71337_.put("EndCity", "EndCity");
    });
    private static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), p_71325_ -> {
        p_71325_.put("Iglu", "Igloo");
        p_71325_.put("TeDP", "Desert_Pyramid");
        p_71325_.put("TeJP", "Jungle_Pyramid");
        p_71325_.put("TeSH", "Swamp_Hut");
    });
    private static final Set<String> OLD_STRUCTURE_REGISTRY_KEYS = Set.of(
        "pillager_outpost",
        "mineshaft",
        "mansion",
        "jungle_pyramid",
        "desert_pyramid",
        "igloo",
        "ruined_portal",
        "shipwreck",
        "swamp_hut",
        "stronghold",
        "monument",
        "ocean_ruin",
        "fortress",
        "endcity",
        "buried_treasure",
        "village",
        "nether_fossil",
        "bastion_remnant"
    );
    private final boolean hasLegacyData;
    private final Map<String, Long2ObjectMap<CompoundTag>> dataMap = Maps.newHashMap();
    private final Map<String, StructureFeatureIndexSavedData> indexMap = Maps.newHashMap();
    private final @Nullable DimensionDataStorage dimensionDataStorage;
    private final List<String> legacyKeys;
    private final List<String> currentKeys;
    private final DataFixer dataFixer;
    private boolean cachesInitialized;

    public LegacyStructureDataHandler(@Nullable DimensionDataStorage storage, List<String> legacyKeys, List<String> currentKeys, DataFixer dataFixer) {
        this.dimensionDataStorage = storage;
        this.legacyKeys = legacyKeys;
        this.currentKeys = currentKeys;
        this.dataFixer = dataFixer;
        boolean flag = false;

        for (String s : this.currentKeys) {
            flag |= this.dataMap.get(s) != null;
        }

        this.hasLegacyData = flag;
    }

    @Override
    public void markChunkDone(ChunkPos p_458919_) {
        long i = p_458919_.toLong();

        for (String s : this.legacyKeys) {
            StructureFeatureIndexSavedData structurefeatureindexsaveddata = this.indexMap.get(s);
            if (structurefeatureindexsaveddata != null && structurefeatureindexsaveddata.hasUnhandledIndex(i)) {
                structurefeatureindexsaveddata.removeIndex(i);
            }
        }
    }

    @Override
    public int targetDataVersion() {
        return 1493;
    }

    @Override
    public CompoundTag applyFix(CompoundTag p_459183_) {
        if (!this.cachesInitialized && this.dimensionDataStorage != null) {
            this.populateCaches(this.dimensionDataStorage);
        }

        int i = NbtUtils.getDataVersion(p_459183_);
        if (i < 1493) {
            p_459183_ = DataFixTypes.CHUNK.update(this.dataFixer, p_459183_, i, 1493);
            if (p_459183_.getCompound("Level").flatMap(p_458890_ -> p_458890_.getBoolean("hasLegacyStructureData")).orElse(false)) {
                p_459183_ = this.updateFromLegacy(p_459183_);
            }
        }

        return p_459183_;
    }

    private CompoundTag updateFromLegacy(CompoundTag tag) {
        CompoundTag compoundtag = tag.getCompoundOrEmpty("Level");
        ChunkPos chunkpos = new ChunkPos(compoundtag.getIntOr("xPos", 0), compoundtag.getIntOr("zPos", 0));
        if (this.isUnhandledStructureStart(chunkpos.x, chunkpos.z)) {
            tag = this.updateStructureStart(tag, chunkpos);
        }

        CompoundTag compoundtag1 = compoundtag.getCompoundOrEmpty("Structures");
        CompoundTag compoundtag2 = compoundtag1.getCompoundOrEmpty("References");

        for (String s : this.currentKeys) {
            boolean flag = OLD_STRUCTURE_REGISTRY_KEYS.contains(s.toLowerCase(Locale.ROOT));
            if (!compoundtag2.getLongArray(s).isPresent() && flag) {
                int i = 8;
                LongList longlist = new LongArrayList();

                for (int j = chunkpos.x - 8; j <= chunkpos.x + 8; j++) {
                    for (int k = chunkpos.z - 8; k <= chunkpos.z + 8; k++) {
                        if (this.hasLegacyStart(j, k, s)) {
                            longlist.add(ChunkPos.asLong(j, k));
                        }
                    }
                }

                compoundtag2.putLongArray(s, longlist.toLongArray());
            }
        }

        compoundtag1.put("References", compoundtag2);
        compoundtag.put("Structures", compoundtag1);
        tag.put("Level", compoundtag);
        return tag;
    }

    private boolean hasLegacyStart(int chunkX, int chunkZ, String key) {
        return !this.hasLegacyData
            ? false
            : this.dataMap.get(key) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(key)).hasStartIndex(ChunkPos.asLong(chunkX, chunkZ));
    }

    private boolean isUnhandledStructureStart(int chunkX, int chunkZ) {
        if (!this.hasLegacyData) {
            return false;
        } else {
            for (String s : this.currentKeys) {
                if (this.dataMap.get(s) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(s)).hasUnhandledIndex(ChunkPos.asLong(chunkX, chunkZ))) {
                    return true;
                }
            }

            return false;
        }
    }

    private CompoundTag updateStructureStart(CompoundTag tag, ChunkPos chunkPos) {
        CompoundTag compoundtag = tag.getCompoundOrEmpty("Level");
        CompoundTag compoundtag1 = compoundtag.getCompoundOrEmpty("Structures");
        CompoundTag compoundtag2 = compoundtag1.getCompoundOrEmpty("Starts");

        for (String s : this.currentKeys) {
            Long2ObjectMap<CompoundTag> long2objectmap = this.dataMap.get(s);
            if (long2objectmap != null) {
                long i = chunkPos.toLong();
                if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(s)).hasUnhandledIndex(i)) {
                    CompoundTag compoundtag3 = long2objectmap.get(i);
                    if (compoundtag3 != null) {
                        compoundtag2.put(s, compoundtag3);
                    }
                }
            }
        }

        compoundtag1.put("Starts", compoundtag2);
        compoundtag.put("Structures", compoundtag1);
        tag.put("Level", compoundtag);
        return tag;
    }

    private synchronized void populateCaches(DimensionDataStorage storage) {
        if (!this.cachesInitialized) {
            for (String s : this.legacyKeys) {
                CompoundTag compoundtag = new CompoundTag();

                try {
                    compoundtag = storage.readTagFromDisk(s, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, 1493)
                        .getCompoundOrEmpty("data")
                        .getCompoundOrEmpty("Features");
                    if (compoundtag.isEmpty()) {
                        continue;
                    }
                } catch (IOException ioexception) {
                }

                compoundtag.forEach(
                    (p_409547_, p_409548_) -> {
                        if (p_409548_ instanceof CompoundTag compoundtag1) {
                            long $$4x = ChunkPos.asLong(compoundtag1.getIntOr("ChunkX", 0), compoundtag1.getIntOr("ChunkZ", 0));
                            ListTag listtag = compoundtag1.getListOrEmpty("Children");
                            if (!listtag.isEmpty()) {
                                Optional<String> optional = listtag.getCompound(0).flatMap(p_409554_ -> p_409554_.getString("id"));
                                optional.map(LEGACY_TO_CURRENT_MAP::get).ifPresent(p_409553_ -> compoundtag1.putString("id", p_409553_));
                            }

                            compoundtag1.getString("id")
                                .ifPresent(
                                    p_409557_ -> this.dataMap.computeIfAbsent(p_409557_, p_71335_ -> new Long2ObjectOpenHashMap<>()).put($$4x, compoundtag1)
                                );
                        }
                    }
                );
                String s1 = s + "_index";
                StructureFeatureIndexSavedData structurefeatureindexsaveddata = storage.computeIfAbsent(StructureFeatureIndexSavedData.type(s1));
                if (structurefeatureindexsaveddata.getAll().isEmpty()) {
                    StructureFeatureIndexSavedData structurefeatureindexsaveddata1 = new StructureFeatureIndexSavedData();
                    this.indexMap.put(s, structurefeatureindexsaveddata1);
                    compoundtag.forEach((p_409550_, p_409551_) -> {
                        if (p_409551_ instanceof CompoundTag compoundtag1) {
                            structurefeatureindexsaveddata1.addIndex(ChunkPos.asLong(compoundtag1.getIntOr("ChunkX", 0), compoundtag1.getIntOr("ChunkZ", 0)));
                        }
                    });
                } else {
                    this.indexMap.put(s, structurefeatureindexsaveddata);
                }
            }

            this.cachesInitialized = true;
        }
    }

    public static Supplier<LegacyTagFixer> getLegacyTagFixer(
        ResourceKey<Level> dimension, Supplier<@Nullable DimensionDataStorage> storageSupplier, DataFixer dataFixer
    ) {
        if (dimension == Level.OVERWORLD) {
            return () -> new LegacyStructureDataHandler(
                storageSupplier.get(),
                ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
                ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"),
                dataFixer
            );
        } else if (dimension == Level.NETHER) {
            List<String> list1 = ImmutableList.of("Fortress");
            return () -> new LegacyStructureDataHandler(storageSupplier.get(), list1, list1, dataFixer);
        } else if (dimension == Level.END) {
            List<String> list = ImmutableList.of("EndCity");
            return () -> new LegacyStructureDataHandler(storageSupplier.get(), list, list, dataFixer);
        } else {
            return LegacyTagFixer.EMPTY;
        }
    }
}
