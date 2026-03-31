package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MipmapGenerator {
    private static final String ITEM_PREFIX = "item/";
    private static final float ALPHA_CUTOFF = 0.5F;
    private static final float STRICT_ALPHA_CUTOFF = 0.3F;

    private MipmapGenerator() {
    }

    private static float alphaTestCoverage(NativeImage image, float alphaRef, float alphaScale) {
        int i = image.getWidth();
        int j = image.getHeight();
        float f = 0.0F;
        int k = 4;

        for (int l = 0; l < j - 1; l++) {
            for (int i1 = 0; i1 < i - 1; i1++) {
                float f1 = Math.clamp(ARGB.alphaFloat(image.getPixel(i1, l)) * alphaScale, 0.0F, 1.0F);
                float f2 = Math.clamp(ARGB.alphaFloat(image.getPixel(i1 + 1, l)) * alphaScale, 0.0F, 1.0F);
                float f3 = Math.clamp(ARGB.alphaFloat(image.getPixel(i1, l + 1)) * alphaScale, 0.0F, 1.0F);
                float f4 = Math.clamp(ARGB.alphaFloat(image.getPixel(i1 + 1, l + 1)) * alphaScale, 0.0F, 1.0F);
                float f5 = 0.0F;

                for (int j1 = 0; j1 < 4; j1++) {
                    float f6 = (j1 + 0.5F) / 4.0F;

                    for (int k1 = 0; k1 < 4; k1++) {
                        float f7 = (k1 + 0.5F) / 4.0F;
                        float f8 = f1 * (1.0F - f7) * (1.0F - f6) + f2 * f7 * (1.0F - f6) + f3 * (1.0F - f7) * f6 + f4 * f7 * f6;
                        if (f8 > alphaRef) {
                            f5++;
                        }
                    }
                }

                f += f5 / 16.0F;
            }
        }

        return f / ((i - 1) * (j - 1));
    }

    private static void scaleAlphaToCoverage(NativeImage image, float desiredCoverage, float alphaRef, float alphaCutoffBias) {
        float f = 0.0F;
        float f1 = 4.0F;
        float f2 = 1.0F;
        float f3 = 1.0F;
        float f4 = Float.MAX_VALUE;
        int i = image.getWidth();
        int j = image.getHeight();

        for (int k = 0; k < 5; k++) {
            float f5 = alphaTestCoverage(image, alphaRef, f2);
            float f6 = Math.abs(f5 - desiredCoverage);
            if (f6 < f4) {
                f4 = f6;
                f3 = f2;
            }

            if (f5 < desiredCoverage) {
                f = f2;
            } else {
                if (!(f5 > desiredCoverage)) {
                    break;
                }

                f1 = f2;
            }

            f2 = (f + f1) * 0.5F;
        }

        for (int l = 0; l < j; l++) {
            for (int i1 = 0; i1 < i; i1++) {
                int j1 = image.getPixel(i1, l);
                float f7 = ARGB.alphaFloat(j1);
                f7 = f7 * f3 + alphaCutoffBias + 0.025F;
                f7 = Math.clamp(f7, 0.0F, 1.0F);
                image.setPixel(i1, l, ARGB.color(f7, j1));
            }
        }
    }

    public static NativeImage[] generateMipLevels(Identifier texture, NativeImage[] mipLevels, int mipLevel, MipmapStrategy strategy, float alphaCutoffBias) {
        if (strategy == MipmapStrategy.AUTO) {
            strategy = hasTransparentPixel(mipLevels[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
        }

        if (mipLevels.length == 1 && !texture.getPath().startsWith("item/")) {
            if (strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT) {
                TextureUtil.solidify(mipLevels[0]);
            } else if (strategy == MipmapStrategy.DARK_CUTOUT) {
                TextureUtil.fillEmptyAreasWithDarkColor(mipLevels[0]);
            }
        }

        if (mipLevel + 1 <= mipLevels.length) {
            return mipLevels;
        } else {
            NativeImage[] anativeimage = new NativeImage[mipLevel + 1];
            anativeimage[0] = mipLevels[0];
            boolean flag = strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT || strategy == MipmapStrategy.DARK_CUTOUT;
            float f = strategy == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
            float f1 = flag ? alphaTestCoverage(mipLevels[0], f, 1.0F) : 0.0F;

            int maxMipmapLevel = net.neoforged.neoforge.client.ClientHooks.getMaxMipmapLevel(anativeimage[0].getWidth(), anativeimage[0].getHeight());
            for (int i = 1; i <= mipLevel; i++) {
                if (i < mipLevels.length) {
                    anativeimage[i] = mipLevels[i];
                } else {
                    NativeImage nativeimage = anativeimage[i - 1];
                    // Forge: Guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
                    NativeImage nativeimage1 = new NativeImage(Math.max(1, nativeimage.getWidth() >> 1), Math.max(1, nativeimage.getHeight() >> 1), false);
                    if (i <= maxMipmapLevel) {
                    int j = nativeimage1.getWidth();
                    int k = nativeimage1.getHeight();

                    for (int l = 0; l < j; l++) {
                        for (int i1 = 0; i1 < k; i1++) {
                            int j1 = nativeimage.getPixel(l * 2 + 0, i1 * 2 + 0);
                            int k1 = nativeimage.getPixel(l * 2 + 1, i1 * 2 + 0);
                            int l1 = nativeimage.getPixel(l * 2 + 0, i1 * 2 + 1);
                            int i2 = nativeimage.getPixel(l * 2 + 1, i1 * 2 + 1);
                            int j2;
                            if (strategy == MipmapStrategy.DARK_CUTOUT) {
                                j2 = darkenedAlphaBlend(j1, k1, l1, i2);
                            } else {
                                j2 = ARGB.meanLinear(j1, k1, l1, i2);
                            }

                            nativeimage1.setPixel(l, i1, j2);
                        }
                    }
                    }

                    anativeimage[i] = nativeimage1;
                }

                if (flag) {
                    scaleAlphaToCoverage(anativeimage[i], f1, f, alphaCutoffBias);
                }
            }

            return anativeimage;
        }
    }

    private static boolean hasTransparentPixel(NativeImage image) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                if (ARGB.alpha(image.getPixel(i, j)) == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int darkenedAlphaBlend(int col0, int col1, int col2, int col3) {
        float f = 0.0F;
        float f1 = 0.0F;
        float f2 = 0.0F;
        float f3 = 0.0F;
        if (ARGB.alpha(col0) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(col0));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(col0));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(col0));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(col0));
        }

        if (ARGB.alpha(col1) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(col1));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(col1));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(col1));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(col1));
        }

        if (ARGB.alpha(col2) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(col2));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(col2));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(col2));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(col2));
        }

        if (ARGB.alpha(col3) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(col3));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(col3));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(col3));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(col3));
        }

        f /= 4.0F;
        f1 /= 4.0F;
        f2 /= 4.0F;
        f3 /= 4.0F;
        return ARGB.color(ARGB.linearToSrgbChannel(f), ARGB.linearToSrgbChannel(f1), ARGB.linearToSrgbChannel(f2), ARGB.linearToSrgbChannel(f3));
    }
}
