package net.minecraft.data;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.FileUtil;

public interface CachedOutput {
    CachedOutput NO_CACHE = (p_465955_, p_465956_, p_465957_) -> {
        FileUtil.createDirectoriesSafe(p_465955_.getParent());
        Files.write(p_465955_, p_465956_);
    };

    void writeIfNeeded(Path filePath, byte[] data, HashCode hashCode) throws IOException;
}
