package com.github.euonmyoji.epicbanitem.util;

import com.github.euonmyoji.epicbanitem.EpicBanItem;
import com.github.euonmyoji.epicbanitem.api.CheckResult;
import com.github.euonmyoji.epicbanitem.api.CheckRuleTrigger;
import com.github.euonmyoji.epicbanitem.util.nbt.NbtTagRenderer;
import com.github.euonmyoji.epicbanitem.util.nbt.QueryResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.stream.JsonWriter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.translation.Translatable;
import org.spongepowered.api.util.Tuple;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yinyangshi GiNYAi ustc_zzzz
 */
@SuppressWarnings("WeakerAccess")
public class TextUtil {

    private static BufferedReader delegationReader;
    private static BufferedWriter delegationWriter;
    private static final ConfigurationLoader<CommentedConfigurationNode> CONCISE_LOADER = getConciseLoader();
    private static final ConfigurationLoader<CommentedConfigurationNode> LOADER = getLoader();

    /**
     * @param origin origin string support FormatText
     * @param keySet placeholders in the string
     * @return TextTemplate
     */
    public static TextTemplate parseTextTemplate(String origin, Set<String> keySet) {
        if (keySet.isEmpty()) {
            return TextTemplate.of(parseFormatText(origin));
        }
        List<Object> objects = new ArrayList<>();
        String[] subStrings = origin.split("\\{");
        for (int i = 0; i < subStrings.length; i++) {
            String subString = subStrings[i];
            if (subString.isEmpty()) {
                continue;
            }
            if (i == 0) {
                objects.add(parseFormatText(subString));
                continue;
            }
            String[] muSub = subString.split("}");
            if (muSub.length == 1 && subString.endsWith("}") && keySet.contains(muSub[0])) {
                objects.add(TextTemplate.arg(muSub[0]));
            } else if (muSub.length > 1 && keySet.contains(muSub[0])) {
                objects.add(TextTemplate.arg(muSub[0]));
                StringBuilder left = new StringBuilder(muSub[1]);
                for (int j = 2; j < muSub.length; j++) {
                    left.append("}");
                    left.append(muSub[j]);
                }
                if (subString.endsWith("}")) {
                    left.append("}");
                }
                objects.add(parseFormatText(left.toString()));
            } else {
                objects.add(parseFormatText("{" + subString));
            }
        }
        return TextTemplate.of(objects.toArray());
    }

    public static Text parseFormatText(String in) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(in);
    }

    public static Text adjustLength(Text text, int length) {
        int spaces = length - text.toPlain().length();
        if (spaces <= 0) {
            return text;
        } else {
            return Text.of(text, String.format("%" + spaces + "s", ""));
        }
    }

    public static Text join(Text delimiter, Iterable<Text> elements) {
        Iterator<Text> iterator = elements.iterator();
        if (!iterator.hasNext()) {
            return Text.EMPTY;
        }
        Text.Builder builder = Text.builder().append(Text.EMPTY).style(TextStyles.RESET).append(iterator.next());
        while (iterator.hasNext()) {
            builder.append(delimiter).append(iterator.next());
        }
        return builder.toText();
    }

    public static Text serializeNbtToString(DataView nbt, QueryResult result) {
        return new NbtTagRenderer(result).render(nbt);
    }

    public static Text serializeNbtToString(DataView nbt) {
        return NbtTagRenderer.EMPTY_RENDERER.render(nbt);
    }
/*
  TODO: use this when sponge forge do not relocate 'com.typesafe.config' to 'configurate.typesafe.config'
    private static final ConfigurationLoader<CommentedConfigurationNode> CONCISE_LOADER = HoconConfigurationLoader.builder()
            .setSource(() -> delegationReader).setSink(() -> delegationWriter)
            .setRenderOptions(ConfigRenderOptions.concise())
            .build();
*/

    public static String escape(String unescapedString) {
        try (StringWriter out = new StringWriter()) {
            JsonWriter writer = new JsonWriter(out);
            writer.value(unescapedString).close();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigurationLoader<CommentedConfigurationNode> getConciseLoader() {
        HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder()
                .setSource(() -> delegationReader).setSink(() -> delegationWriter);
        try {
            for (Method method : HoconConfigurationLoader.Builder.class.getMethods()) {
                if ("setRenderOptions".equals(method.getName())) {
                    Class<?> configRenderOptionsClass = method.getParameterTypes()[0];
                    Object conciseRenderOptions = configRenderOptionsClass.getMethod("concise").invoke(null);
                    builder = (HoconConfigurationLoader.Builder) method.invoke(builder, conciseRenderOptions);
                }
            }
        } catch (IndexOutOfBoundsException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            EpicBanItem.getLogger().error("Error", e);
            return builder.build();
        }
        return builder.build();
    }

/*
  TODO: use this when sponge forge do not relocate 'com.typesafe.config' to 'configurate.typesafe.config'
    private static final ConfigurationLoader<CommentedConfigurationNode> LOADER = HoconConfigurationLoader.builder()
            .setSource(() -> delegationReader).setSink(() -> delegationWriter)
            .setParseOptions(ConfigParseOptions.defaults().setAllowMissing(true))
            .setRenderOptions(ConfigRenderOptions.defaults().setOriginComments(false))
            .build();
*/

    private static ConfigurationLoader<CommentedConfigurationNode> getLoader() {
        HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder()
                .setSource(() -> delegationReader).setSink(() -> delegationWriter);
        try {
            for (Method method : HoconConfigurationLoader.Builder.class.getMethods()) {
                if ("setParseOptions".equals(method.getName())) {
                    Class<?> parseOptionsClass = method.getParameterTypes()[0];
                    Method setAllowMissingMethod = parseOptionsClass.getMethod("setAllowMissing", boolean.class);
                    Object defaultParseOptions = parseOptionsClass.getMethod("defaults").invoke(null);
                    builder = (HoconConfigurationLoader.Builder) method.invoke(builder, setAllowMissingMethod.invoke(defaultParseOptions, true));
                } else if ("setRenderOptions".equals(method.getName())) {
                    Class<?> configRenderOptionsClass = method.getParameterTypes()[0];
                    Method setOriginCommentsMethod = configRenderOptionsClass.getMethod("setOriginComments", boolean.class);
                    Object defaultRenderOptions = configRenderOptionsClass.getMethod("defaults").invoke(null);
                    builder = (HoconConfigurationLoader.Builder) method.invoke(builder, setOriginCommentsMethod.invoke(defaultRenderOptions, false));
                }
            }
        } catch (IndexOutOfBoundsException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            EpicBanItem.getLogger().error("Error", e);
            return builder.build();
        }
        return builder.build();
    }

    public static ConfigurationNode serializeStringToConfigNode(String string) throws IOException {
        try (StringReader in = new StringReader(string); BufferedReader bufferedReader = new BufferedReader(in)) {
            delegationReader = bufferedReader;
            return LOADER.load();
        } finally {
            delegationReader = null;
        }
    }

    public static String deserializeConfigNodeToString(ConfigurationNode configNode) throws IOException {
        try (StringWriter out = new StringWriter(); BufferedWriter bufferedWriter = new BufferedWriter(out)) {
            delegationWriter = bufferedWriter;
            LOADER.save(configNode);
            return out.toString();
        } finally {
            delegationWriter = null;
        }
    }

    public static String deserializeConfigNodeToPlanString(ConfigurationNode configNode) throws IOException {
        try (StringWriter out = new StringWriter(); BufferedWriter bufferedWriter = new BufferedWriter(out)) {
            delegationWriter = bufferedWriter;
            CONCISE_LOADER.save(configNode);
            return out.toString();
        } finally {
            delegationWriter = null;
        }
    }

    public static <T extends ValueContainer<?> & Translatable> Text getDisplayName(T item) {
        return item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation()));
    }

    private static final Map<String, TextTemplate> customInfoMessageCache = new ConcurrentHashMap<>();
    private static final Set<String> INFO_TOKENS = ImmutableSet.of("rules", "trigger", "item_pre", "item_post");

    public static Collection<Text> prepareMessage(CheckRuleTrigger trigger, Text itemPre, List<Tuple<Text, Optional<String>>> banRules, boolean updated) {
        LinkedHashMap<String, Tuple<TextTemplate, List<Text>>> map = new LinkedHashMap<>();
        List<Text> undefined = new ArrayList<>();
        for (Tuple<Text, Optional<String>> rule : banRules) {
            if (rule.getSecond().isPresent()) {
                map.computeIfAbsent(
                        rule.getSecond().get(),
                        s -> new Tuple<>(
                                customInfoMessageCache.computeIfAbsent(s, s1 -> parseTextTemplate(s1, INFO_TOKENS)),
                                new ArrayList<>()
                        )
                ).getSecond().add(rule.getFirst());
            } else {
                undefined.add(rule.getFirst());
            }
        }
        Function<List<Text>, Map<String, Text>> toParams = checkRules -> ImmutableMap.of(
                "rules", Text.joinWith(Text.of(","), checkRules),
                "trigger", trigger.toText(),
                "item_pre", itemPre,
                "item_post", itemPre
        );
        List<Text> result = new ArrayList<>();
        if (!undefined.isEmpty()) {
            result.add(EpicBanItem.getMessages().getMessage(
                    updated ? "epicbanitem.info.defaultUpdateMessage" : "epicbanitem.info.defaultBanMessage",
                    toParams.apply(undefined)
            ));
        }
        for (Tuple<TextTemplate, List<Text>> tuple : map.values()) {
            result.add(tuple.getFirst().apply(toParams.apply(tuple.getSecond())).build());
        }
        return result.stream().filter(text -> !text.isEmpty()).collect(Collectors.toList());
    }
}
