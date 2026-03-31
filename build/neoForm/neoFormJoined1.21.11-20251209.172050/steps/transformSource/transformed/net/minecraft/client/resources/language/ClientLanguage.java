package net.minecraft.client.resources.language;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLanguage extends Language {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, String> storage;
    private final Map<String, net.minecraft.network.chat.Component> componentStorage;
    private final boolean defaultRightToLeft;

    @Deprecated
    private ClientLanguage(Map<String, String> storage, boolean defaultRightToLeft) {
        this(storage, defaultRightToLeft, Map.of());
    }

    private ClientLanguage(Map<String, String> storage, boolean defaultRightToLeft, Map<String, net.minecraft.network.chat.Component> componentStorage) {
        this.storage = storage;
        this.defaultRightToLeft = defaultRightToLeft;
        this.componentStorage = componentStorage;
    }

    public static ClientLanguage loadFrom(ResourceManager resourceManager, List<String> filenames, boolean defaultRightToLeft) {
        Map<String, String> map = new HashMap<>();
        Map<String, net.minecraft.network.chat.Component> componentMap = new HashMap<>();

        for (String s : filenames) {
            String s1 = String.format(Locale.ROOT, "lang/%s.json", s);
            map.putAll(net.neoforged.fml.i18n.I18nManager.loadTranslations(s));

            for (String s2 : resourceManager.getNamespaces()) {
                try {
                    Identifier identifier = Identifier.fromNamespaceAndPath(s2, s1);
                    appendFrom(s, resourceManager.getResourceStack(identifier), map, componentMap);
                } catch (Exception exception) {
                    LOGGER.warn("Skipped language file: {}:{} ({})", s2, s1, exception.toString());
                }
            }
        }

        DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(map);
        return new ClientLanguage(Map.copyOf(map), defaultRightToLeft, Map.copyOf(componentMap));
    }

    @Deprecated
    private static void appendFrom(String languageName, List<Resource> resources, Map<String, String> destinationMap) {
        appendFrom(languageName, resources, destinationMap, new java.util.HashMap<>());
    }

    private static void appendFrom(String languageName, List<Resource> resources, Map<String, String> destinationMap, Map<String, net.minecraft.network.chat.Component> componentMap) {
        for (Resource resource : resources) {
            try (InputStream inputstream = resource.open()) {
                Language.loadFromJson(inputstream, destinationMap::put, componentMap::put);
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to load translations for {} from pack {}", languageName, resource.sourcePackId(), ioexception);
            }
        }
    }

    @Override
    public String getOrDefault(String p_118920_, String p_265273_) {
        return this.storage.getOrDefault(p_118920_, p_265273_);
    }

    @Override
    public boolean has(String p_118928_) {
        return this.storage.containsKey(p_118928_);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return this.defaultRightToLeft;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText p_118925_) {
        return FormattedBidiReorder.reorder(p_118925_, this.defaultRightToLeft);
    }

    @Override
    public Map<String, String> getLanguageData() {
        return storage;
    }

    @Override
    public net.minecraft.network.chat.@org.jspecify.annotations.Nullable Component getComponent(String key) {
        return componentStorage.get(key);
    }
}
