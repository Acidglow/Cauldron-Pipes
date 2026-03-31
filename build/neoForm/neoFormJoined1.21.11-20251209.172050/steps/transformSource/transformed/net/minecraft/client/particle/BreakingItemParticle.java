package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreakingItemParticle extends SingleQuadParticle {
    private final float uo;
    private final float vo;
    private final SingleQuadParticle.Layer layer;

    public BreakingItemParticle(
        ClientLevel p_105665_,
        double p_105666_,
        double p_105667_,
        double p_105668_,
        double p_445900_,
        double p_446322_,
        double p_446995_,
        TextureAtlasSprite p_447201_
    ) {
        this(p_105665_, p_105666_, p_105667_, p_105668_, p_447201_);
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += p_445900_;
        this.yd += p_446322_;
        this.zd += p_446995_;
    }

    public BreakingItemParticle(ClientLevel p_105646_, double p_105647_, double p_105648_, double p_105649_, TextureAtlasSprite p_445587_) {
        super(p_105646_, p_105647_, p_105648_, p_105649_, 0.0, 0.0, 0.0, p_445587_);
        this.gravity = 1.0F;
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
        this.layer = p_445587_.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? SingleQuadParticle.Layer.TERRAIN : SingleQuadParticle.Layer.ITEMS;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.layer;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CobwebProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_338579_,
            ClientLevel p_338749_,
            double p_338877_,
            double p_338362_,
            double p_338343_,
            double p_338303_,
            double p_338217_,
            double p_338683_,
            RandomSource p_447103_
        ) {
            return new BreakingItemParticle(p_338749_, p_338877_, p_338362_, p_338343_, this.getSprite(new ItemStack(Items.COBWEB), p_338749_, p_447103_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class ItemParticleProvider<T extends ParticleOptions> implements ParticleProvider<T> {
        private final ItemStackRenderState scratchRenderState = new ItemStackRenderState();

        protected TextureAtlasSprite getSprite(ItemStack stack, ClientLevel level, RandomSource random) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(this.scratchRenderState, stack, ItemDisplayContext.GROUND, level, null, 0);
            TextureAtlasSprite textureatlassprite = this.scratchRenderState.pickParticleIcon(random);
            return textureatlassprite != null ? textureatlassprite : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider extends BreakingItemParticle.ItemParticleProvider<ItemParticleOption> {
        public Particle createParticle(
            ItemParticleOption p_447284_,
            ClientLevel p_105687_,
            double p_105688_,
            double p_105689_,
            double p_105690_,
            double p_105691_,
            double p_105692_,
            double p_105693_,
            RandomSource p_445691_
        ) {
            return new BreakingItemParticle(
                p_105687_, p_105688_, p_105689_, p_105690_, p_105691_, p_105692_, p_105693_, this.getSprite(p_447284_.getItem(), p_105687_, p_445691_)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SlimeProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_446828_,
            ClientLevel p_105697_,
            double p_105698_,
            double p_105699_,
            double p_105700_,
            double p_105701_,
            double p_105702_,
            double p_105703_,
            RandomSource p_445657_
        ) {
            return new BreakingItemParticle(p_105697_, p_105698_, p_105699_, p_105700_, this.getSprite(new ItemStack(Items.SLIME_BALL), p_105697_, p_445657_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SnowballProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_445680_,
            ClientLevel p_105716_,
            double p_105717_,
            double p_105718_,
            double p_105719_,
            double p_105720_,
            double p_105721_,
            double p_105722_,
            RandomSource p_446275_
        ) {
            return new BreakingItemParticle(p_105716_, p_105717_, p_105718_, p_105719_, this.getSprite(new ItemStack(Items.SNOWBALL), p_105716_, p_446275_));
        }
    }
}
