package net.minecraft.client.renderer;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MaterialMapper(Identifier sheet, String prefix) {
    public Material apply(Identifier name) {
        return new Material(this.sheet, name.withPrefix(this.prefix + "/"));
    }

    public Material defaultNamespaceApply(String name) {
        return this.apply(Identifier.withDefaultNamespace(name));
    }
}
