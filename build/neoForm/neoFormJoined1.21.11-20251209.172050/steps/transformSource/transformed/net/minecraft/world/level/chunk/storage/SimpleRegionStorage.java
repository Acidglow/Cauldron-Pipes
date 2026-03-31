package net.minecraft.world.level.chunk.storage;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class SimpleRegionStorage implements AutoCloseable {
    private final IOWorker worker;
    private final DataFixer fixerUpper;
    private final DataFixTypes dataFixType;
    private final Supplier<LegacyTagFixer> legacyFixer;

    public SimpleRegionStorage(RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean sync, DataFixTypes dataFixType) {
        this(info, folder, fixerUpper, sync, dataFixType, LegacyTagFixer.EMPTY);
    }

    public SimpleRegionStorage(
        RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean sync, DataFixTypes dataFixType, Supplier<LegacyTagFixer> legacyFixer
    ) {
        this.fixerUpper = fixerUpper;
        this.dataFixType = dataFixType;
        this.worker = new IOWorker(info, folder, sync);
        this.legacyFixer = Suppliers.memoize(legacyFixer::get);
    }

    public boolean isOldChunkAround(ChunkPos chunkPos, int radius) {
        return this.worker.isOldChunkAround(chunkPos, radius);
    }

    public CompletableFuture<Optional<CompoundTag>> read(ChunkPos chunkPos) {
        return this.worker.loadAsync(chunkPos);
    }

    public CompletableFuture<Void> write(ChunkPos chunkPos, CompoundTag data) {
        return this.write(chunkPos, () -> data);
    }

    public CompletableFuture<Void> write(ChunkPos chunkPos, Supplier<CompoundTag> dataSupplier) {
        this.markChunkDone(chunkPos);
        return this.worker.store(chunkPos, dataSupplier);
    }

    public CompoundTag upgradeChunkTag(CompoundTag tag, int version, @Nullable CompoundTag context) {
        int i = NbtUtils.getDataVersion(tag, version);
        if (i == SharedConstants.getCurrentVersion().dataVersion().version()) {
            return tag;
        } else {
            try {
                tag = this.legacyFixer.get().applyFix(tag);
                injectDatafixingContext(tag, context);
                tag = this.dataFixType.updateToCurrentVersion(this.fixerUpper, tag, Math.max(this.legacyFixer.get().targetDataVersion(), i));
                removeDatafixingContext(tag);
                NbtUtils.addCurrentDataVersion(tag);
                return tag;
            } catch (Exception exception) {
                CrashReport crashreport = CrashReport.forThrowable(exception, "Updated chunk");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Updated chunk details");
                crashreportcategory.setDetail("Data version", i);
                throw new ReportedException(crashreport);
            }
        }
    }

    public CompoundTag upgradeChunkTag(CompoundTag tag, int version) {
        return this.upgradeChunkTag(tag, version, null);
    }

    public Dynamic<Tag> upgradeChunkTag(Dynamic<Tag> tag, int version) {
        return new Dynamic<>(tag.getOps(), this.upgradeChunkTag((CompoundTag)tag.getValue(), version, null));
    }

    public static void injectDatafixingContext(CompoundTag tag, @Nullable CompoundTag context) {
        if (context != null) {
            tag.put("__context", context);
        }
    }

    private static void removeDatafixingContext(CompoundTag tag) {
        tag.remove("__context");
    }

    protected void markChunkDone(ChunkPos chunkPos) {
        this.legacyFixer.get().markChunkDone(chunkPos);
    }

    public CompletableFuture<Void> synchronize(boolean flushStorage) {
        return this.worker.synchronize(flushStorage);
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }

    public RegionStorageInfo storageInfo() {
        return this.worker.storageInfo();
    }
}
