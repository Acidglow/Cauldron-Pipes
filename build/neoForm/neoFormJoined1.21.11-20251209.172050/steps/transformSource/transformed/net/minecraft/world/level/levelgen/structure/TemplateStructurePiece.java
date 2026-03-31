package net.minecraft.world.level.levelgen.structure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.function.Function;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public abstract class TemplateStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final String templateName;
    protected StructureTemplate template;
    protected StructurePlaceSettings placeSettings;
    protected BlockPos templatePosition;

    public TemplateStructurePiece(
        StructurePieceType type,
        int genDepth,
        StructureTemplateManager structureTemplateManager,
        Identifier location,
        String templateName,
        StructurePlaceSettings placeSettings,
        BlockPos templatePosition
    ) {
        super(type, genDepth, structureTemplateManager.getOrCreate(location).getBoundingBox(placeSettings, templatePosition));
        this.setOrientation(Direction.NORTH);
        this.templateName = templateName;
        this.templatePosition = templatePosition;
        this.template = structureTemplateManager.getOrCreate(location);
        this.placeSettings = placeSettings;
    }

    public TemplateStructurePiece(
        StructurePieceType type, CompoundTag tag, StructureTemplateManager structureTemplateManager, Function<Identifier, StructurePlaceSettings> placeSettingsFactory
    ) {
        super(type, tag);
        this.setOrientation(Direction.NORTH);
        this.templateName = tag.getStringOr("Template", "");
        this.templatePosition = new BlockPos(tag.getIntOr("TPX", 0), tag.getIntOr("TPY", 0), tag.getIntOr("TPZ", 0));
        Identifier identifier = this.makeTemplateLocation();
        this.template = structureTemplateManager.getOrCreate(identifier);
        this.placeSettings = placeSettingsFactory.apply(identifier);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
    }

    protected Identifier makeTemplateLocation() {
        return Identifier.parse(this.templateName);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext p_192690_, CompoundTag p_192691_) {
        p_192691_.putInt("TPX", this.templatePosition.getX());
        p_192691_.putInt("TPY", this.templatePosition.getY());
        p_192691_.putInt("TPZ", this.templatePosition.getZ());
        p_192691_.putString("Template", this.templateName);
    }

    @Override
    public void postProcess(
        WorldGenLevel p_226899_,
        StructureManager p_226900_,
        ChunkGenerator p_226901_,
        RandomSource p_226902_,
        BoundingBox p_226903_,
        ChunkPos p_226904_,
        BlockPos p_226905_
    ) {
        this.placeSettings.setBoundingBox(p_226903_);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (this.template.placeInWorld(p_226899_, this.templatePosition, p_226905_, this.placeSettings, p_226902_, 2)) {
            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : this.template
                .filterBlocks(this.templatePosition, this.placeSettings, Blocks.STRUCTURE_BLOCK)) {
                if (structuretemplate$structureblockinfo.nbt() != null) {
                    StructureMode structuremode = structuretemplate$structureblockinfo.nbt().read("mode", StructureMode.LEGACY_CODEC).orElseThrow();
                    if (structuremode == StructureMode.DATA) {
                        this.handleDataMarker(
                            structuretemplate$structureblockinfo.nbt().getStringOr("metadata", ""),
                            structuretemplate$structureblockinfo.pos(),
                            p_226899_,
                            p_226902_,
                            p_226903_
                        );
                    }
                }
            }

            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 : this.template
                .filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW)) {
                if (structuretemplate$structureblockinfo1.nbt() != null) {
                    String s = structuretemplate$structureblockinfo1.nbt().getStringOr("final_state", "minecraft:air");
                    BlockState blockstate = Blocks.AIR.defaultBlockState();

                    try {
                        blockstate = BlockStateParser.parseForBlock(p_226899_.holderLookup(Registries.BLOCK), s, true).blockState();
                    } catch (CommandSyntaxException commandsyntaxexception) {
                        LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", s, structuretemplate$structureblockinfo1.pos());
                    }

                    p_226899_.setBlock(structuretemplate$structureblockinfo1.pos(), blockstate, 3);
                }
            }
        }
    }

    protected abstract void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box);

    @Deprecated
    @Override
    public void move(int p_73668_, int p_73669_, int p_73670_) {
        super.move(p_73668_, p_73669_, p_73670_);
        this.templatePosition = this.templatePosition.offset(p_73668_, p_73669_, p_73670_);
    }

    @Override
    public Rotation getRotation() {
        return this.placeSettings.getRotation();
    }

    public StructureTemplate template() {
        return this.template;
    }

    public BlockPos templatePosition() {
        return this.templatePosition;
    }

    public StructurePlaceSettings placeSettings() {
        return this.placeSettings;
    }
}
