package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class VertexArrayCache {
    public static VertexArrayCache create(GLCapabilities capabilities, GlDebugLabel debugLabel, Set<String> enabledExtensions) {
        if (capabilities.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
            enabledExtensions.add("GL_ARB_vertex_attrib_binding");
            return new VertexArrayCache.Separate(debugLabel);
        } else {
            return new VertexArrayCache.Emulated(debugLabel);
        }
    }

    public abstract void bindVertexArray(VertexFormat format, @Nullable GlBuffer buffer);

    @OnlyIn(Dist.CLIENT)
    static class Emulated extends VertexArrayCache {
        private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
        private final GlDebugLabel debugLabels;

        public Emulated(GlDebugLabel debugLabels) {
            this.debugLabels = debugLabels;
        }

        @Override
        public void bindVertexArray(VertexFormat p_410490_, @Nullable GlBuffer p_410612_) {
            VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_410490_);
            if (vertexarraycache$vertexarray == null) {
                int i = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(i);
                if (p_410612_ != null) {
                    GlStateManager._glBindBuffer(34962, p_410612_.handle);
                    setupCombinedAttributes(p_410490_, true);
                }

                VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_410490_, p_410612_);
                this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
                this.cache.put(p_410490_, vertexarraycache$vertexarray1);
            } else {
                GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
                if (p_410612_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_410612_) {
                    GlStateManager._glBindBuffer(34962, p_410612_.handle);
                    vertexarraycache$vertexarray.lastVertexBuffer = p_410612_;
                    setupCombinedAttributes(p_410490_, false);
                }
            }
        }

        private static void setupCombinedAttributes(VertexFormat vertexFormat, boolean enabled) {
            int i = vertexFormat.getVertexSize();
            List<VertexFormatElement> list = vertexFormat.getElements();

            for (int j = 0; j < list.size(); j++) {
                VertexFormatElement vertexformatelement = list.get(j);
                if (enabled) {
                    GlStateManager._enableVertexAttribArray(j);
                }

                switch (vertexformatelement.usage()) {
                    case POSITION:
                    case GENERIC:
                    case UV:
                        if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                            GlStateManager._vertexAttribPointer(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, i, vertexFormat.getOffset(vertexformatelement)
                            );
                        } else {
                            GlStateManager._vertexAttribIPointer(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), i, vertexFormat.getOffset(vertexformatelement)
                            );
                        }
                        break;
                    case NORMAL:
                    case COLOR:
                        GlStateManager._vertexAttribPointer(
                            j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, i, vertexFormat.getOffset(vertexformatelement)
                        );
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Separate extends VertexArrayCache {
        private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
        private final GlDebugLabel debugLabels;
        private final boolean needsMesaWorkaround;

        public Separate(GlDebugLabel debugLabels) {
            this.debugLabels = debugLabels;
            if ("Mesa".equals(GlStateManager._getString(7936))) {
                String s = GlStateManager._getString(7938);
                this.needsMesaWorkaround = s.contains("25.0.0") || s.contains("25.0.1") || s.contains("25.0.2");
            } else {
                this.needsMesaWorkaround = false;
            }
        }

        @Override
        public void bindVertexArray(VertexFormat p_410638_, @Nullable GlBuffer p_410508_) {
            VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_410638_);
            if (vertexarraycache$vertexarray != null) {
                GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
                if (p_410508_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_410508_) {
                    if (this.needsMesaWorkaround
                        && vertexarraycache$vertexarray.lastVertexBuffer != null
                        && vertexarraycache$vertexarray.lastVertexBuffer.handle == p_410508_.handle) {
                        ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
                    }

                    ARBVertexAttribBinding.glBindVertexBuffer(0, p_410508_.handle, 0L, p_410638_.getVertexSize());
                    vertexarraycache$vertexarray.lastVertexBuffer = p_410508_;
                }
            } else {
                int i = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(i);
                if (p_410508_ != null) {
                    List<VertexFormatElement> list = p_410638_.getElements();

                    for (int j = 0; j < list.size(); j++) {
                        VertexFormatElement vertexformatelement = list.get(j);
                        GlStateManager._enableVertexAttribArray(j);
                        switch (vertexformatelement.usage()) {
                            case POSITION:
                            case GENERIC:
                            case UV:
                                if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                                    ARBVertexAttribBinding.glVertexAttribFormat(
                                        j,
                                        vertexformatelement.count(),
                                        GlConst.toGl(vertexformatelement.type()),
                                        false,
                                        p_410638_.getOffset(vertexformatelement)
                                    );
                                } else {
                                    ARBVertexAttribBinding.glVertexAttribIFormat(
                                        j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), p_410638_.getOffset(vertexformatelement)
                                    );
                                }
                                break;
                            case NORMAL:
                            case COLOR:
                                ARBVertexAttribBinding.glVertexAttribFormat(
                                    j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, p_410638_.getOffset(vertexformatelement)
                                );
                        }

                        ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
                    }
                }

                if (p_410508_ != null) {
                    ARBVertexAttribBinding.glBindVertexBuffer(0, p_410508_.handle, 0L, p_410638_.getVertexSize());
                }

                VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_410638_, p_410508_);
                this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
                this.cache.put(p_410638_, vertexarraycache$vertexarray1);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VertexArray {
        final int id;
        final VertexFormat format;
        @Nullable GlBuffer lastVertexBuffer;

        VertexArray(int id, VertexFormat format, @Nullable GlBuffer lastVertexBuffer) {
            this.id = id;
            this.format = format;
            this.lastVertexBuffer = lastVertexBuffer;
        }
    }
}
