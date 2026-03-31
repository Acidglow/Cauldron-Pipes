package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerProfessionLayer<S extends LivingEntityRenderState & VillagerDataHolderRenderState, M extends EntityModel<S> & VillagerLikeModel>
    extends RenderLayer<S, M> {
    private static final Int2ObjectMap<Identifier> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap<>(), p_465659_ -> {
        p_465659_.put(1, Identifier.withDefaultNamespace("stone"));
        p_465659_.put(2, Identifier.withDefaultNamespace("iron"));
        p_465659_.put(3, Identifier.withDefaultNamespace("gold"));
        p_465659_.put(4, Identifier.withDefaultNamespace("emerald"));
        p_465659_.put(5, Identifier.withDefaultNamespace("diamond"));
    });
    private final Object2ObjectMap<ResourceKey<VillagerType>, VillagerMetadataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<ResourceKey<VillagerProfession>, VillagerMetadataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap<>();
    private final ResourceManager resourceManager;
    private final String path;
    private final M noHatModel;
    private final M noHatBabyModel;

    public VillagerProfessionLayer(RenderLayerParent<S, M> renderer, ResourceManager resourceManager, String path, M noHatModel, M noHatBabyModel) {
        super(renderer);
        this.resourceManager = resourceManager;
        this.path = path;
        this.noHatModel = noHatModel;
        this.noHatBabyModel = noHatBabyModel;
    }

    public void submit(PoseStack p_434803_, SubmitNodeCollector p_433025_, int p_435851_, S p_434354_, float p_435110_, float p_433651_) {
        if (!p_434354_.isInvisible) {
            VillagerData villagerdata = p_434354_.getVillagerData();
            if (villagerdata != null) {
                Holder<VillagerType> holder = villagerdata.type();
                Holder<VillagerProfession> holder1 = villagerdata.profession();
                VillagerMetadataSection.Hat villagermetadatasection$hat = this.getHatData(this.typeHatCache, "type", holder);
                VillagerMetadataSection.Hat villagermetadatasection$hat1 = this.getHatData(this.professionHatCache, "profession", holder1);
                M m = this.getParentModel();
                Identifier identifier = this.getIdentifier("type", holder);
                boolean flag = villagermetadatasection$hat1 == VillagerMetadataSection.Hat.NONE
                    || villagermetadatasection$hat1 == VillagerMetadataSection.Hat.PARTIAL && villagermetadatasection$hat != VillagerMetadataSection.Hat.FULL;
                M m1 = p_434354_.isBaby ? this.noHatBabyModel : this.noHatModel;
                renderColoredCutoutModel(flag ? m : m1, identifier, p_434803_, p_433025_, p_435851_, p_434354_, -1, 1);
                if (!holder1.is(VillagerProfession.NONE) && !p_434354_.isBaby) {
                    Identifier identifier1 = this.getIdentifier("profession", holder1);
                    renderColoredCutoutModel(m, identifier1, p_434803_, p_433025_, p_435851_, p_434354_, -1, 2);
                    if (!holder1.is(VillagerProfession.NITWIT)) {
                        Identifier identifier2 = this.getIdentifier(
                            "profession_level", LEVEL_LOCATIONS.get(Mth.clamp(villagerdata.level(), 1, LEVEL_LOCATIONS.size()))
                        );
                        renderColoredCutoutModel(m, identifier2, p_434803_, p_433025_, p_435851_, p_434354_, -1, 3);
                    }
                }
            }
        }
    }

    private Identifier getIdentifier(String folder, Identifier id) {
        return id.withPath(p_247944_ -> "textures/entity/" + this.path + "/" + folder + "/" + p_247944_ + ".png");
    }

    private Identifier getIdentifier(String folder, Holder<?> holder) {
        return holder.unwrapKey().map(p_465661_ -> this.getIdentifier(folder, p_465661_.identifier())).orElse(MissingTextureAtlasSprite.getLocation());
    }

    public <K> VillagerMetadataSection.Hat getHatData(
        Object2ObjectMap<ResourceKey<K>, VillagerMetadataSection.Hat> cache, String folder, Holder<K> key
    ) {
        ResourceKey<K> resourcekey = key.unwrapKey().orElse(null);
        return resourcekey == null
            ? VillagerMetadataSection.Hat.NONE
            : cache.computeIfAbsent(
                resourcekey, p_465658_ -> this.resourceManager.getResource(this.getIdentifier(folder, resourcekey.identifier())).flatMap(p_389338_ -> {
                    try {
                        return p_389338_.metadata().getSection(VillagerMetadataSection.TYPE).map(VillagerMetadataSection::hat);
                    } catch (IOException ioexception) {
                        return Optional.empty();
                    }
                }).orElse(VillagerMetadataSection.Hat.NONE)
            );
    }
}
