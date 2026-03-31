package net.minecraft.server.packs.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;

public abstract class PackDetector<T> {
    private final DirectoryValidator validator;

    protected PackDetector(DirectoryValidator validator) {
        this.validator = validator;
    }

    public @Nullable T detectPackResources(Path p_path, List<ForbiddenSymlinkInfo> forbiddenSymlinkInfos) throws IOException {
        Path path = p_path;

        BasicFileAttributes basicfileattributes;
        try {
            basicfileattributes = Files.readAttributes(p_path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException nosuchfileexception) {
            return null;
        }

        if (basicfileattributes.isSymbolicLink()) {
            this.validator.validateSymlink(p_path, forbiddenSymlinkInfos);
            if (!forbiddenSymlinkInfos.isEmpty()) {
                return null;
            }

            path = Files.readSymbolicLink(p_path);
            basicfileattributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }

        if (basicfileattributes.isDirectory()) {
            this.validator.validateKnownDirectory(path, forbiddenSymlinkInfos);
            if (!forbiddenSymlinkInfos.isEmpty()) {
                return null;
            } else {
                return !Files.isRegularFile(path.resolve("pack.mcmeta")) ? null : this.createDirectoryPack(path);
            }
        } else {
            return basicfileattributes.isRegularFile() && path.getFileName().toString().endsWith(".zip") ? this.createZipPack(path) : null;
        }
    }

    protected abstract @Nullable T createZipPack(Path path) throws IOException;

    protected abstract @Nullable T createDirectoryPack(Path path) throws IOException;
}
