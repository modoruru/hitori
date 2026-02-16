package su.hitori.plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import su.hitori.api.Pair;
import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.Messages;
import su.hitori.plugin.module.ModuleDescriptorImpl;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class HitoriCommand extends CommandAPICommand {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy (z)");

    private final CorePlugin corePlugin;

    public HitoriCommand(CorePlugin corePlugin) {
        super("hitori");
        this.corePlugin = corePlugin;

        withPermission("*");
        withSubcommands(
                new CommandAPICommand("reload")
                        .withArguments(moduleArgument(corePlugin))
                        .withOptionalArguments(new BooleanArgument("sure"))
                        .executes(this::reload),

                new CommandAPICommand("modules")
                        .executes(this::modules),

                new CommandAPICommand("dump")
                        .executes(this::dump)
        );
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "iB";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private Optional<Pair<String, String>> extractCiCdInfo() {
        try (InputStream inputStream = corePlugin.getResource("build-info.properties")) {
            if(inputStream == null) return Optional.empty();

            Properties properties = new Properties();
            properties.load(inputStream);

            return Optional.of(Pair.of(
                    (String) properties.getOrDefault("commit", "ide"),
                    (String) properties.getOrDefault("branch", "ide")
            ));
        }
        catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void dump(CommandSender sender, CommandArguments args) {
        DumpBuilder builder = new DumpBuilder();
        ModuleRepository moduleRepository = corePlugin.moduleRepository();
        var ciCdInfo = extractCiCdInfo().orElse(Pair.of("ide", "ide"));

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
            builder.append("    version: =").appendAqua(moduleMeta.version().toString()).newLine();
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

        sender.sendMessage(Messages.INFO.create(String.format(
                "Creating dump...\n%s\n<yellow><click:copy_to_clipboard:'%s'>[click to copy]</yellow>",
                builder.styledToString(),
                builder.baseToString()
        )));
    }

    private void modules(CommandSender sender, CommandArguments args) {
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

        sender.sendMessage(Messages.INFO.create(builder.toString()));
    }

    private void reload(CommandSender sender, CommandArguments args) {
        boolean areUserSure = args.getOrDefaultUnchecked("sure", false);
        NamespacedKey key = args.getUnchecked("module");
        assert key != null;
        ModuleDescriptor descriptor = corePlugin.moduleRepository().getModule(key).orElse(null);
        if(descriptor == null) {
            sender.sendMessage(Messages.ERROR.text("Module does not exists."));
            return;
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
                return;
            }
        }
        impl.reload(impl.getJar(), true, Set.of());
        sender.sendMessage(Messages.INFO.create("Module successfully reloaded."));
    }

    private static Argument<NamespacedKey> moduleArgument(CorePlugin corePlugin) {
        return new NamespacedKeyArgument("module").replaceSuggestions(ArgumentSuggestions.stringCollection(ignored ->
                corePlugin.moduleRepository().keySet().stream().map(Key::asString).toList()
        ));
    }

}
