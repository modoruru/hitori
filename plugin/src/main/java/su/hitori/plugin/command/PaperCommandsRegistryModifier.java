package su.hitori.plugin.command;

import org.bukkit.Bukkit;
import su.hitori.api.util.Task;
import su.hitori.plugin.CorePlugin;

public final class PaperCommandsRegistryModifier extends AbstractPaperBasedCommandsRegistryModifier {

    public PaperCommandsRegistryModifier(CorePlugin corePlugin) {
        super(corePlugin);
    }

    @Override
    public void reload() {
        Task.ensureSync(Bukkit::reloadData);
    }

}
