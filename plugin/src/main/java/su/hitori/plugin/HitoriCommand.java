package su.hitori.plugin;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import su.hitori.api.Pair;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Messages;
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
                                                    Key key = context.getArgument("key", NamespacedKey.class);

                                                    HitoriConfiguration<?> configuration = corePlugin.configurationRegistry().get(key);
                                                    if(configuration == null) throw new SimpleCommandExceptionType(new LiteralMessage("Can't find \"" + key.asString() + "\" configuration.")).create();


                                                })))
                                .then(Commands.literal("read").executes())))
                .build();
    }

    private static int readConfigFromSource(CorePlugin corePlugin, CommandContext<CommandSourceStack> context) {

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
            return 1;
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
