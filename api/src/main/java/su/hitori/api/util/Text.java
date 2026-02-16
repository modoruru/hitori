package su.hitori.api.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

/**
 * Text utils based on MiniMessage library
 */
public final class Text {

    private static MiniMessage miniMessage;
    private static final Map<UUID, TagResolver> additionalResolvers = new HashMap<>();

    private Text() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates Adventure Component based on minimessage text format
     * @param input text in minimessage format
     * @return created component
     */
    public static @NotNull Component create(@Nullable String input) {
        if(input == null || input.isEmpty()) return Component.empty();
        return miniMessage.deserialize(input);
    }

    /**
     * Creates a collection of Adventure Component's based on minimessage text format
     * @param first first entry to format
     * @param array other entries to format
     * @return collection containing all created component's
     */
    public static List<Component> create(String first, String... array) {
        List<Component> components = new ArrayList<>();
        components.add(create(first));
        for (String s : array) {
            components.add(create(s));
        }
        return List.copyOf(components);
    }

    /**
     * Creates a collection of Adventure Component's based on minimessage text format
     * @param input collection of text in minimessage format
     * @return collection containing all created component's
     */
    public static List<Component> create(Collection<String> input) {
        if(input == null || input.isEmpty()) return List.of();
        return input.stream().map(Text::create).toList();
    }

    /**
     * Serializes Adventure Component's to string in minimessage format
     * @param text component
     * @return component serialized as text
     */
    public static @NotNull String serialize(@Nullable Component text) {
        if(text == null) return "";
        return miniMessage.serialize(text);
    }

    /**
     * Screens all minimessage tags so they are ignored in component creation.
     * <p></p>
     * Can be used for untrusted input.
     * @param input text potentially containing tags
     * @return screened text
     */
    public static @NotNull String restrictTags(@NotNull String input) {
        return miniMessage.escapeTags(input);
//        return serialize(Component.text(input.replaceAll("(?<!\\\\)<", "\\\\<")));
    }

    /**
     * Creates Title object using minimessage text format
     * @param title the title text in minimessage format
     * @param subtitle the subtitle text in minimessage format
     * @param fadeInMillis time to fadeIn in milliseconds
     * @param stayMillis time to stay in milliseconds
     * @param fadeOutMillis time to fade out in milliseconds
     * @return created Title
     */
    public static Title createTitle(@Nullable String title, @Nullable String subtitle, long fadeInMillis, long stayMillis, long fadeOutMillis) {
        return Title.title(
                create(title), create(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeInMillis),
                        Duration.ofMillis(stayMillis),
                        Duration.ofMillis(fadeOutMillis)
                )
        );
    }

    /**
     * Adds TagResolver to this whole util. All components created after adding new TagResolver, will use that TagResolver.
     * @param resolver TagResolver to add
     * @return unique id of resolver. can be used to later remove that TagResolver using {@link #removeResolver(UUID)}
     */
    public static UUID addResolver(TagResolver resolver) {
        if(resolver == null) return null;
        UUID uuid = UUID.randomUUID();
        additionalResolvers.put(uuid, resolver);
        createMiniMessage();
        return uuid;
    }

    /**
     * Removes previously added TagResolver from this util.
     * @param uuid unique id of TagResolver you've got from {@link #addResolver(TagResolver)}
     */
    public static void removeResolver(UUID uuid) {
        if(uuid == null) return;
        additionalResolvers.remove(uuid);
        createMiniMessage();
    }

    private static void createMiniMessage() {
        Text.miniMessage = MiniMessage.builder().tags(
                TagResolver.builder()
                        .resolver(MiniMessage.miniMessage().tags())
                        .resolvers(additionalResolvers.values())
                        .build()
        ).build();
    }

    static {
        createMiniMessage();
    }

}
