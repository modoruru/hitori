package su.hitori.plugin;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Pair;
import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.SectionScheme;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Messages;
import su.hitori.api.util.SafeUtil;
import su.hitori.api.util.UnsafeUtil;
import su.hitori.plugin.module.ModuleDescriptorImpl;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

final class HitoriCommand {

    private static final Logger LOGGER = LoggerFactory.instance().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy (z)");

    private HitoriCommand() {}

    public static LiteralCommandNode<CommandSourceStack> boostrap(CorePlugin corePlugin) {
        return Commands.literal("hitori")
                .then(Commands.literal("modules")
                        .executes(context -> modules(corePlugin, context)))
                .then(Commands.literal("reload")
                        .then(moduleArgument(corePlugin)
                                .then(Commands.argument("sure", BoolArgumentType.bool())
                                        .executes(context -> reload(corePlugin, context)))
                                .executes(context -> reload(corePlugin, context))))
                .then(Commands.literal("dump")
                        .executes(context -> dump(corePlugin, context)))
                .then(Commands.literal("config")
                        .then(Commands.argument("key", ArgumentTypes.namespacedKey())
                                .suggests((_, builder) -> {
                                    for (Key key : corePlugin.configurationRegistry().keys()) {
                                        builder.suggest(key.asString());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("field")
                                        .then(Commands.argument("path", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    HitoriConfiguration<?> configuration = extractConfiguration(corePlugin, context, false);
                                                    assert configuration != null;

                                                    Map<String, SectionScheme.Node> lastSection = configuration.rootNode().section();
                                                    assert lastSection != null;

                                                    String currentValue = builder.getRemaining();
                                                    if(currentValue.isEmpty()) {
                                                        lastSection.keySet().forEach(builder::suggest);
                                                        return builder.buildFuture();
                                                    }
                                                    boolean endsWithDot = currentValue.charAt(currentValue.length() - 1) == '.';

                                                    String[] pathParts = builder.getRemaining().split("\\.");

                                                    int readPathPartsLength = 0;
                                                    Map<String, SectionScheme.Node> lastFoundSection = lastSection;
                                                    int sectionsToSeek = pathParts.length;
                                                    if(!endsWithDot) --sectionsToSeek;

                                                    for (int i = 0; i < sectionsToSeek; i++) {
                                                        String pathPart = pathParts[i];
                                                        readPathPartsLength += pathPart.length();
                                                        if(i != 0) ++readPathPartsLength;
                                                        SectionScheme.Node node = lastFoundSection.get(pathPart);
                                                        if(node == null || node.nodeType() != SectionScheme.NodeType.SECTION) {
                                                            throw new SimpleCommandExceptionType(new LiteralMessage(String.format(
                                                                    "Node %s on %s config either doesn't exists or is not a section",
                                                                    currentValue.substring(0, readPathPartsLength),
                                                                    configuration.key().asString()
                                                            ))).create();
                                                        }

                                                        lastFoundSection = node.section();
                                                        assert lastFoundSection != null;
                                                    }

                                                    String lastPart = pathParts[pathParts.length - 1];
                                                    for (String nodeKey : lastFoundSection.keySet()) {
                                                        if(endsWithDot || nodeKey.startsWith(lastPart)) {
                                                            if(pathParts.length == 1 && !endsWithDot) builder.suggest(nodeKey); // petrusha
                                                            else builder.suggest(currentValue.substring(0, readPathPartsLength) + '.' + nodeKey);
                                                        }
                                                    }

                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.literal("get")
                                                        .executes(context -> getConfigValue(corePlugin, context)))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                                .executes(context -> setConfigValue(corePlugin, context))))))
                                .then(Commands.literal("read")
                                        .executes(context -> readConfigFromSource(corePlugin, context)))
                                .then(Commands.literal("write")
                                        .executes(context -> writeConfigToSource(corePlugin, context)))))
                .build();
    }

    private static @Nullable HitoriConfiguration<?> extractConfiguration(CorePlugin corePlugin, CommandContext<CommandSourceStack> context, boolean messageInsteadOfException) throws CommandSyntaxException {
        Key key = context.getArgument("key", NamespacedKey.class);

        HitoriConfiguration<?> configuration = corePlugin.configurationRegistry().get(key);
        if(configuration == null) {
            if(messageInsteadOfException) {
                Messages.ERROR.createAndSendToSender(context.getSource(), "Can't find \"" + key.asString() + "\" configuration.");
                return null;
            }

            throw new SimpleCommandExceptionType(new LiteralMessage("Can't find \"" + key.asString() + "\" configuration.")).create();
        }

        return configuration;
    }

    private static SectionScheme.@Nullable Node extractNode(CommandContext<CommandSourceStack> context, HitoriConfiguration<?> configuration){
        String path = context.getArgument("path", String.class);
        String[] pathParts = path.split("\\.");

        Map<String, SectionScheme.Node> section = configuration.rootNode().section();
        assert section != null;

        int readPathPartsLength = 0;
        for(int i = 0, length = pathParts.length - 1; i < length; i++) {
            String pathPart = pathParts[i];
            readPathPartsLength += pathPart.length();
            if(i != 0) ++readPathPartsLength;

            SectionScheme.Node node = section.get(pathPart);
            if(node == null || node.nodeType() != SectionScheme.NodeType.SECTION) {
                Messages.ERROR.createAndSendToSender(context.getSource(), String.format(
                        "Node <aqua>%s</aqua> on <yellow>%s</yellow> config either doesn't exists or is not a section.",
                        path.substring(0, readPathPartsLength),
                        configuration.key().asString()
                ));
                return null;
            }

            section = node.section();
            assert section != null;
        }

        return section.get(pathParts[pathParts.length - 1]);
    }

    private static int setConfigValue(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HitoriConfiguration<?> configuration = extractConfiguration(corePlugin, context, true);
        if(configuration == null) return 0;

        SectionScheme.Node node = extractNode(context, configuration);
        if(node == null) return 0;

        String path = context.getArgument("path", String.class);
        switch (node.nodeType()) {
            case SECTION -> Messages.ERROR.createAndSendToSender(context.getSource(), String.format(
                    "Node <aqua>%s</aqua> is a section: only reading specific values is supported.",
                    path.substring(path.lastIndexOf('.') + 1) // even if there's only one node, lastIndexOf returns -1 and +1 will move index to 0
            ));
            case LIST -> throw new UnsupportedOperationException();
            case PRIMITIVE -> {
                Field<?> field = node.field();
                assert field != null;

                String userPassedValue = context.getArgument("value", String.class);
                Object castValue = switch (field.defaultValue()) {
                    case Byte _ -> SafeUtil.parseByte(userPassedValue);
                    case Short _ -> SafeUtil.parseShort(userPassedValue);
                    case Integer _ -> SafeUtil.parseInt(userPassedValue);
                    case Long _ -> SafeUtil.parseLong(userPassedValue);
                    case Float _ -> SafeUtil.parseFloat(userPassedValue);
                    case Double _ -> SafeUtil.parseDouble(userPassedValue);
                    case Boolean _ -> SafeUtil.parseBoolean(userPassedValue);
                    case Character _ -> userPassedValue.charAt(0);
                    case String _ -> userPassedValue;
                    case Enum<?> asEnum -> SafeUtil.enumValueOf(asEnum.getClass(), userPassedValue);
                    default -> throw new UnsupportedOperationException();
                };

                if(castValue == null) {
                    Messages.ERROR.createAndSendToSender(context.getSource(), String.format(
                            "Unable to set new value to <aqua>%s</aqua> node on config <yellow>%s</yellow>: %s",
                            path,
                            configuration.key().asString(),
                            switch (field.defaultValue()) {
                                case Enum<?> asEnum -> {
                                    Enum<?>[] constants = asEnum.getClass().getEnumConstants();

                                    StringBuilder builder = new StringBuilder("node only accepts ");

                                    for (int i = 0, length = constants.length; i < length; i++) {
                                        Enum<?> constant = constants[i];
                                        builder.append("<aqua>").append(constant.name()).append("</aqua>");

                                        if(i == length - 2) builder.append(" and ");
                                        else if(i < length - 2) builder.append(", ");
                                    }

                                    builder.append(" fields");
                                    yield builder.toString();
                                }
                                case Boolean _ -> "node only accepts <aqua>true</aqua> and <aqua>false</aqua> values.";
                                case Float _, Double _ -> "node only accepts floating point numbers";
                                case Integer _, Long _ -> "node only accepts integer numbers";
                                case Byte _ -> String.format("node only accepts integer numbers between %s and %s", Byte.MIN_VALUE, Byte.MAX_VALUE);
                                case Short _ -> String.format("node only accepts integer numbers between %s and %s", Short.MIN_VALUE, Short.MAX_VALUE);
                                default -> throw new UnsupportedOperationException();
                            }
                    ));
                    return 0;
                }

                field.set(UnsafeUtil.cast(castValue));
                context.getSource().getSender().sendMessage(
                        Messages.INFO.create(String.format(
                                "<aqua>%s</aqua> node value on config <yellow>%s</yellow> set to: ",
                                path,
                                configuration.key().asString()
                        )).append(Component.text(displayFieldValueBeauty(castValue)).color(NamedTextColor.AQUA))
                );
            }
        }

        return 1;
    }

    private static String displayFieldValueBeauty(Object value) {
        return switch (value) {
            case String asString -> '"' + asString + '"';
            case Enum<?> asEnum -> asEnum.name();
            default -> String.valueOf(value);
        };
    }

    private static int getConfigValue(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HitoriConfiguration<?> configuration = extractConfiguration(corePlugin, context, true);
        if(configuration == null) return 0;

        SectionScheme.Node node = extractNode(context, configuration);
        if(node == null) return 0;

        String path = context.getArgument("path", String.class);
        switch (node.nodeType()) {
            case PRIMITIVE -> {
                Field<?> field = node.field();
                assert field != null;

                Object value = field.get();
                context.getSource().getSender().sendMessage(
                        Messages.INFO.create(String.format(
                                "<aqua>%s</aqua> node value on config <yellow>%s</yellow> is: ",
                                path,
                                configuration.key().asString()
                        )).append(Component.text(displayFieldValueBeauty(value)).color(NamedTextColor.AQUA))
                );
            }
            case LIST -> throw new UnsupportedOperationException();
            case SECTION -> Messages.ERROR.createAndSendToSender(context.getSource(), String.format(
                    "Node <aqua>%s</aqua> is a section: only reading specific values is supported.",
                    path.substring(path.lastIndexOf('.') + 1) // even if there's only node, lastIndexOf returns -1 and +1 will move index to 0
            ));
        }

        return 1;
    }

    private static int writeConfigToSource(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HitoriConfiguration<?> configuration = extractConfiguration(corePlugin, context, true);
        if(configuration == null) return 0;

        if(!configuration.hasConfigurationSource()) {
            Messages.ERROR.createAndSendToSender(context.getSource(), "Configuration <yellow>" + configuration.key().asString() + "</yellow> has no default source.");
            return 0;
        }

        configuration.writeToSource();
        Messages.INFO.createAndSendToSender(context.getSource(), "Configuration <yellow>" + configuration.key().asString() + "</yellow> was written to it's default source.");
        return 1;
    }

    private static int readConfigFromSource(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HitoriConfiguration<?> configuration = extractConfiguration(corePlugin, context, true);
        if(configuration == null) return 0;

        if(!configuration.hasConfigurationSource()) {
            Messages.ERROR.createAndSendToSender(context.getSource(), "Configuration <yellow>" + configuration.key().asString() + "</yellow> has no default source.");
            return 0;
        }

        configuration.readFromSource();
        Messages.INFO.createAndSendToSender(context.getSource(), "Configuration <yellow>" + configuration.key().asString() + "</yellow> was read from it's default source.");

        return 1;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "iB";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private static Optional<Pair<String, String>> extractCiCdInfo(CorePlugin corePlugin) {
        try (InputStream inputStream = corePlugin.getResource("build-info.properties")) {
            if(inputStream == null) return Optional.empty();

            Properties properties = new Properties();
            properties.load(inputStream);

            return Optional.of(Pair.of(
                    (String) properties.getOrDefault("commit", "ide"),
                    (String) properties.getOrDefault("branch", "ide")
            ));
        }
        catch (IOException exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
            return Optional.empty();
        }
    }

    private static int dump(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) {
        DumpBuilder builder = new DumpBuilder();
        ModuleRepository moduleRepository = corePlugin.moduleRepository();
        var ciCdInfo = extractCiCdInfo(corePlugin).orElse(Pair.of("ide", "ide"));

        // plugin info, server info, hardware info
        builder.append("- hitori\n");
        builder.append("  version: ").appendAqua(corePlugin.getPluginMeta().getVersion()).newLine();
        builder.append("  commit: ").appendAqua(ciCdInfo.first()).newLine();
        builder.append("  branch: ").appendAqua(ciCdInfo.second()).newLine();
        builder.append("  modules installed: ").appendAqua(moduleRepository.keySet().size()).newLine();
        builder.append("  modules:\n");

        for (Key key : moduleRepository.keySet()) {
            builder.append("  - ").appendYellow(key.asString()).newLine();

            var opt = moduleRepository.getModule(key)
                    .map(ModuleDescriptor::getInstance)
                    .map(Module::moduleMeta);
            assert opt.isPresent();
            ModuleMeta moduleMeta = opt.get();
            builder.append("    version: ").appendAqua(moduleMeta.version().toString()).newLine();
            builder.append("    description: \"").appendYellow(moduleMeta.description()).append("\"\n");
        }

        ServerBuildInfo serverBuildInfo = ServerBuildInfo.buildInfo();
        builder.append("- server\n");
        builder.append("  game version: ").appendAqua(serverBuildInfo.minecraftVersionId()).newLine();
        builder.append("  brand id: ").appendAqua(serverBuildInfo.brandId()).newLine();
        builder.append("  brand name: \"").appendYellow(serverBuildInfo.brandName()).append("\"\n");
        builder.append("  - build\n");
        builder.append("    number: ").appendAqua(
                Optional.of(serverBuildInfo.buildNumber().orElse(-1))
                        .filter(value -> value != -1)
                        .map(String::valueOf)
                        .orElse("unknown")
        ).newLine();
        builder.append("    timestamp: ").appendAqua(DATE_FORMAT.format(serverBuildInfo.buildTime().atZone(ZoneId.of("UTC")))).newLine();

        builder.append("- system\n");
        builder.append("  - os\n");
        builder.append("    name: \"").appendYellow(System.getProperty("os.name")).append("\"\n");
        builder.append("    version: ").appendAqua(System.getProperty("os.version")).newLine();
        builder.append("    architecture: ").appendAqua(System.getProperty("os.arch")).newLine();
        builder.append("  - hardware\n");
        builder.append("    RAM: ").appendAqua(formatBytes(Runtime.getRuntime().maxMemory()));

        context.getSource().getSender().sendMessage(Messages.INFO.create(String.format(
                "Creating dump...\n%s\n<yellow><click:copy_to_clipboard:'%s'>[click to copy]</yellow>",
                builder.styledToString(),
                builder.baseToString()
        )));
        return 1;
    }

    private static int modules(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) {
        TreeMap<String, ModuleDescriptor> modules = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Key key : corePlugin.moduleRepository().keySet()) {
            ModuleDescriptor descriptor = corePlugin.moduleRepository().getModule(key).orElse(null);
            assert descriptor != null;
            modules.put(key.asString(), descriptor);
        }

        int size = modules.size();

        StringBuilder builder = new StringBuilder();
        builder.append("Modules (").append(size).append("):");
        if(size > 0) {
            builder.append("\n - ");
            var iterator = modules.values().iterator();
            while (iterator.hasNext()) {
                ModuleDescriptor descriptor = iterator.next();
                builder.append("<color:").append(descriptor.isEnabled() ? "green" : "red").append('>')
                        .append(descriptor.key().asString())
                        .append("</color>");
                if(iterator.hasNext()) builder.append(", ");
            }
        }

        context.getSource().getSender().sendMessage(Messages.INFO.create(builder.toString()));
        return 1;
    }

    private static int reload(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        boolean areUserSure = context.getArgument("sure", Boolean.class);
        NamespacedKey key = context.getArgument("module", NamespacedKey.class);
        ModuleDescriptor descriptor = corePlugin.moduleRepository().getModule(key).orElse(null);
        if(descriptor == null) {
            sender.sendMessage(Messages.ERROR.text("Module does not exists."));
            return 0;
        }

        ModuleDescriptorImpl impl = (ModuleDescriptorImpl) descriptor;

        if(!areUserSure) {
            Optional<List<Key>> affectedModules = impl.getReloadAffectedModules();
            if(affectedModules.isPresent()) {
                sender.sendMessage(Messages.WARNING.create(String.format(
                        "This module will also reload such modules: %s. If you sure in reloading this module, run this command: <yellow>\"/hitori reload %s true\"</yellow>",
                        String.join(
                                ", ",
                                affectedModules.get().stream()
                                        .map(Key::asString)
                                        .map(string -> "<aqua>\"" + string + "\"</aqua>")
                                        .toList()
                        ),
                        key.asString()
                )));
                return 1;
            }
        }
        assert impl.getJar() != null;
        impl.reload(impl.getJar(), true, true, Set.of());
        sender.sendMessage(Messages.INFO.create("Module successfully reloaded."));

        return 1;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, NamespacedKey> moduleArgument(CorePlugin corePlugin) {
        return Commands.argument("module", ArgumentTypes.namespacedKey()).suggests((_, builder) -> {
            corePlugin.moduleRepository().keySet().stream().map(Key::asString).forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

}
