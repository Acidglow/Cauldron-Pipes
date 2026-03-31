package com.mojang.blaze3d.vertex;

import java.nio.ByteOrder;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class BufferBuilder implements VertexConsumer {
    private static final int MAX_VERTEX_COUNT = 16777215;
    private static final long NOT_BUILDING = -1L;
    private static final long UNKNOWN_ELEMENT = -1L;
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private final ByteBufferBuilder buffer;
    private long vertexPointer = -1L;
    private int vertices;
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final boolean fastFormat;
    private final boolean fullFormat;
    private final int vertexSize;
    private final int initialElementsToFill;
    private final int[] offsetsByElement;
    private int elementsToFill;
    private boolean building = true;

    public BufferBuilder(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (!format.contains(VertexFormatElement.POSITION)) {
            throw new IllegalArgumentException("Cannot build mesh with no position element");
        } else {
            this.buffer = buffer;
            this.mode = mode;
            this.format = format;
            this.vertexSize = format.getVertexSize();
            this.initialElementsToFill = format.getElementsMask() & ~VertexFormatElement.POSITION.mask();
            this.offsetsByElement = format.getOffsetsByElement();
            boolean flag = format == DefaultVertexFormat.NEW_ENTITY;
            boolean flag1 = format == DefaultVertexFormat.BLOCK;
            this.fastFormat = flag || flag1;
            this.fullFormat = flag;
        }
    }

    public @Nullable MeshData build() {
        this.ensureBuilding();
        this.endLastVertex();
        MeshData meshdata = this.storeMesh();
        this.building = false;
        this.vertexPointer = -1L;
        return meshdata;
    }

    public MeshData buildOrThrow() {
        MeshData meshdata = this.build();
        if (meshdata == null) {
            throw new IllegalStateException("BufferBuilder was empty");
        } else {
            return meshdata;
        }
    }

    private void ensureBuilding() {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
    }

    private @Nullable MeshData storeMesh() {
        if (this.vertices == 0) {
            return null;
        } else {
            ByteBufferBuilder.Result bytebufferbuilder$result = this.buffer.build();
            if (bytebufferbuilder$result == null) {
                return null;
            } else {
                int i = this.mode.indexCount(this.vertices);
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(this.vertices);
                return new MeshData(bytebufferbuilder$result, new MeshData.DrawState(this.format, this.vertices, i, this.mode, vertexformat$indextype));
            }
        }
    }

    private long beginVertex() {
        this.ensureBuilding();
        this.endLastVertex();
        if (this.vertices >= 16777215) {
            throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
        } else {
            this.vertices++;
            long i = this.buffer.reserve(this.vertexSize);
            this.vertexPointer = i;
            return i;
        }
    }

    private long beginElement(VertexFormatElement element) {
        int i = this.elementsToFill;
        int j = i & ~element.mask();
        if (j == i) {
            return -1L;
        } else {
            this.elementsToFill = j;
            long k = this.vertexPointer;
            if (k == -1L) {
                throw new IllegalArgumentException("Not currently building vertex");
            } else {
                return k + this.offsetsByElement[element.id()];
            }
        }
    }

    private void endLastVertex() {
        if (this.vertices != 0) {
            if (this.elementsToFill != 0) {
                String s = VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
                throw new IllegalStateException("Missing elements in vertex: " + s);
            } else {
                if (this.mode == VertexFormat.Mode.LINES) {
                    long i = this.buffer.reserve(this.vertexSize);
                    MemoryUtil.memCopy(i - this.vertexSize, i, this.vertexSize);
                    this.vertices++;
                }
            }
        }
    }

    private static void putRgba(long pointer, int color) {
        int i = ARGB.toABGR(color);
        MemoryUtil.memPutInt(pointer, IS_LITTLE_ENDIAN ? i : Integer.reverseBytes(i));
    }

    private static void putPackedUv(long pointer, int packedUv) {
        if (IS_LITTLE_ENDIAN) {
            MemoryUtil.memPutInt(pointer, packedUv);
        } else {
            MemoryUtil.memPutShort(pointer, (short)(packedUv & 65535));
            MemoryUtil.memPutShort(pointer + 2L, (short)(packedUv >> 16 & 65535));
        }
    }

    @Override
    public VertexConsumer addVertex(float p_350828_, float p_350614_, float p_350700_) {
        long i = this.beginVertex() + this.offsetsByElement[VertexFormatElement.POSITION.id()];
        this.elementsToFill = this.initialElementsToFill;
        MemoryUtil.memPutFloat(i, p_350828_);
        MemoryUtil.memPutFloat(i + 4L, p_350614_);
        MemoryUtil.memPutFloat(i + 8L, p_350700_);
        return this;
    }

    @Override
    public VertexConsumer setColor(int p_350581_, int p_350952_, int p_350275_, int p_350985_) {
        long i = this.beginElement(VertexFormatElement.COLOR);
        if (i != -1L) {
            MemoryUtil.memPutByte(i, (byte)p_350581_);
            MemoryUtil.memPutByte(i + 1L, (byte)p_350952_);
            MemoryUtil.memPutByte(i + 2L, (byte)p_350275_);
            MemoryUtil.memPutByte(i + 3L, (byte)p_350985_);
        }

        return this;
    }

    @Override
    public VertexConsumer setColor(int p_350530_) {
        long i = this.beginElement(VertexFormatElement.COLOR);
        if (i != -1L) {
            putRgba(i, p_350530_);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv(float p_350574_, float p_350773_) {
        long i = this.beginElement(VertexFormatElement.UV0);
        if (i != -1L) {
            MemoryUtil.memPutFloat(i, p_350574_);
            MemoryUtil.memPutFloat(i + 4L, p_350773_);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv1(int p_350396_, int p_350722_) {
        return this.uvShort((short)p_350396_, (short)p_350722_, VertexFormatElement.UV1);
    }

    @Override
    public VertexConsumer setOverlay(int p_350297_) {
        long i = this.beginElement(VertexFormatElement.UV1);
        if (i != -1L) {
            putPackedUv(i, p_350297_);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv2(int p_351058_, int p_350320_) {
        return this.uvShort((short)p_351058_, (short)p_350320_, VertexFormatElement.UV2);
    }

    @Override
    public VertexConsumer setLight(int p_350848_) {
        long i = this.beginElement(VertexFormatElement.UV2);
        if (i != -1L) {
            putPackedUv(i, p_350848_);
        }

        return this;
    }

    private VertexConsumer uvShort(short u, short v, VertexFormatElement element) {
        long i = this.beginElement(element);
        if (i != -1L) {
            MemoryUtil.memPutShort(i, u);
            MemoryUtil.memPutShort(i + 2L, v);
        }

        return this;
    }

    @Override
    public VertexConsumer setNormal(float p_351000_, float p_350982_, float p_350974_) {
        long i = this.beginElement(VertexFormatElement.NORMAL);
        if (i != -1L) {
            MemoryUtil.memPutByte(i, normalIntValue(p_351000_));
            MemoryUtil.memPutByte(i + 1L, normalIntValue(p_350982_));
            MemoryUtil.memPutByte(i + 2L, normalIntValue(p_350974_));
        }

        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float p_454791_) {
        long i = this.beginElement(VertexFormatElement.LINE_WIDTH);
        if (i != -1L) {
            MemoryUtil.memPutFloat(i, p_454791_);
        }

        return this;
    }

    private static byte normalIntValue(float value) {
        return (byte)((int)(Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
    }

    @Override
    public void addVertex(
        float p_350423_,
        float p_350381_,
        float p_350383_,
        int p_350371_,
        float p_350977_,
        float p_350674_,
        int p_350816_,
        int p_350690_,
        float p_350640_,
        float p_350490_,
        float p_350810_
    ) {
        if (this.fastFormat) {
            long i = this.beginVertex();
            MemoryUtil.memPutFloat(i + 0L, p_350423_);
            MemoryUtil.memPutFloat(i + 4L, p_350381_);
            MemoryUtil.memPutFloat(i + 8L, p_350383_);
            putRgba(i + 12L, p_350371_);
            MemoryUtil.memPutFloat(i + 16L, p_350977_);
            MemoryUtil.memPutFloat(i + 20L, p_350674_);
            long j;
            if (this.fullFormat) {
                putPackedUv(i + 24L, p_350816_);
                j = i + 28L;
            } else {
                j = i + 24L;
            }

            putPackedUv(j + 0L, p_350690_);
            MemoryUtil.memPutByte(j + 4L, normalIntValue(p_350640_));
            MemoryUtil.memPutByte(j + 5L, normalIntValue(p_350490_));
            MemoryUtil.memPutByte(j + 6L, normalIntValue(p_350810_));
        } else {
            VertexConsumer.super.addVertex(
                p_350423_, p_350381_, p_350383_, p_350371_, p_350977_, p_350674_, p_350816_, p_350690_, p_350640_, p_350490_, p_350810_
            );
        }
    }
}
