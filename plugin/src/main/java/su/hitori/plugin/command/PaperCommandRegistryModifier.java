package su.hitori.plugin.command;

import org.bukkit.Bukkit;
import su.hitori.api.util.Task;
import su.hitori.plugin.CorePlugin;

public final class PaperCommandRegistryModifier extends AbstractPaperBasedCommandsRegistryModifier {

    public PaperCommandRegistryModifier(CorePlugin corePlugin) {
        super(corePlugin);
    }

    @Override
    protected void reload() {
        Task.ensureSync(Bukkit::reloadData);
    }

}
