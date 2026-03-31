package net.minecraft.util;

public class Ease {
    public static float inBack(float value) {
        float f = 1.70158F;
        float f1 = 2.70158F;
        return Mth.square(value) * (2.70158F * value - 1.70158F);
    }

    public static float inBounce(float value) {
        return 1.0F - outBounce(1.0F - value);
    }

    public static float inCubic(float value) {
        return Mth.cube(value);
    }

    public static float inElastic(float value) {
        if (value == 0.0F) {
            return 0.0F;
        } else if (value == 1.0F) {
            return 1.0F;
        } else {
            float f = (float) (Math.PI * 2.0 / 3.0);
            return (float)(-Math.pow(2.0, 10.0 * value - 10.0) * Math.sin((value * 10.0 - 10.75) * (float) (Math.PI * 2.0 / 3.0)));
        }
    }

    public static float inExpo(float value) {
        return value == 0.0F ? 0.0F : (float)Math.pow(2.0, 10.0 * value - 10.0);
    }

    public static float inQuart(float value) {
        return Mth.square(Mth.square(value));
    }

    public static float inQuint(float value) {
        return Mth.square(Mth.square(value)) * value;
    }

    public static float inSine(float value) {
        return 1.0F - Mth.cos(value * (float) (Math.PI / 2));
    }

    public static float inOutBounce(float value) {
        return value < 0.5F ? (1.0F - outBounce(1.0F - 2.0F * value)) / 2.0F : (1.0F + outBounce(2.0F * value - 1.0F)) / 2.0F;
    }

    public static float inOutCirc(float value) {
        return value < 0.5F
            ? (float)((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * value, 2.0))) / 2.0)
            : (float)((Math.sqrt(1.0 - Math.pow(-2.0 * value + 2.0, 2.0)) + 1.0) / 2.0);
    }

    public static float inOutCubic(float value) {
        return value < 0.5F ? 4.0F * Mth.cube(value) : (float)(1.0 - Math.pow(-2.0 * value + 2.0, 3.0) / 2.0);
    }

    public static float inOutQuad(float value) {
        return value < 0.5F ? 2.0F * Mth.square(value) : (float)(1.0 - Math.pow(-2.0 * value + 2.0, 2.0) / 2.0);
    }

    public static float inOutQuart(float value) {
        return value < 0.5F ? 8.0F * Mth.square(Mth.square(value)) : (float)(1.0 - Math.pow(-2.0 * value + 2.0, 4.0) / 2.0);
    }

    public static float inOutQuint(float value) {
        return value < 0.5 ? 16.0F * value * value * value * value * value : (float)(1.0 - Math.pow(-2.0 * value + 2.0, 5.0) / 2.0);
    }

    public static float outBounce(float value) {
        float f = 7.5625F;
        float f1 = 2.75F;
        if (value < 0.36363637F) {
            return 7.5625F * Mth.square(value);
        } else if (value < 0.72727275F) {
            return 7.5625F * Mth.square(value - 0.54545456F) + 0.75F;
        } else {
            return value < 0.9090909090909091
                ? 7.5625F * Mth.square(value - 0.8181818F) + 0.9375F
                : 7.5625F * Mth.square(value - 0.95454544F) + 0.984375F;
        }
    }

    public static float outElastic(float value) {
        float f = (float) (Math.PI * 2.0 / 3.0);
        if (value == 0.0F) {
            return 0.0F;
        } else {
            return value == 1.0F
                ? 1.0F
                : (float)(Math.pow(2.0, -10.0 * value) * Math.sin((value * 10.0 - 0.75) * (float) (Math.PI * 2.0 / 3.0)) + 1.0);
        }
    }

    public static float outExpo(float value) {
        return value == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * value);
    }

    public static float outQuad(float value) {
        return 1.0F - Mth.square(1.0F - value);
    }

    public static float outQuint(float value) {
        return 1.0F - (float)Math.pow(1.0 - value, 5.0);
    }

    public static float outSine(float value) {
        return Mth.sin(value * (float) (Math.PI / 2));
    }

    public static float inOutSine(float value) {
        return -(Mth.cos((float) Math.PI * value) - 1.0F) / 2.0F;
    }

    public static float outBack(float value) {
        float f = 1.70158F;
        float f1 = 2.70158F;
        return 1.0F + 2.70158F * Mth.cube(value - 1.0F) + 1.70158F * Mth.square(value - 1.0F);
    }

    public static float outQuart(float value) {
        return 1.0F - Mth.square(Mth.square(1.0F - value));
    }

    public static float outCubic(float value) {
        return 1.0F - Mth.cube(1.0F - value);
    }

    public static float inOutExpo(float value) {
        if (value < 0.5F) {
            return value == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0 * value - 10.0) / 2.0);
        } else {
            return value == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0 * value + 10.0)) / 2.0);
        }
    }

    public static float inQuad(float value) {
        return value * value;
    }

    public static float outCirc(float value) {
        return (float)Math.sqrt(1.0F - Mth.square(value - 1.0F));
    }

    public static float inOutElastic(float value) {
        float f = (float) Math.PI * 4.0F / 9.0F;
        if (value == 0.0F) {
            return 0.0F;
        } else if (value == 1.0F) {
            return 1.0F;
        } else {
            double d0 = Math.sin((20.0 * value - 11.125) * (float) Math.PI * 4.0F / 9.0F);
            return value < 0.5F
                ? (float)(-(Math.pow(2.0, 20.0 * value - 10.0) * d0) / 2.0)
                : (float)(Math.pow(2.0, -20.0 * value + 10.0) * d0 / 2.0 + 1.0);
        }
    }

    public static float inCirc(float value) {
        return (float)(-Math.sqrt(1.0F - value * value)) + 1.0F;
    }

    public static float inOutBack(float value) {
        float f = 1.70158F;
        float f1 = 2.5949094F;
        if (value < 0.5F) {
            return 4.0F * value * value * (7.189819F * value - 2.5949094F) / 2.0F;
        } else {
            float f2 = 2.0F * value - 2.0F;
            return (f2 * f2 * (3.5949094F * f2 + 2.5949094F) + 2.0F) / 2.0F;
        }
    }
}
