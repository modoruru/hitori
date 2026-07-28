package su.hitori.api.config;

import org.bukkit.configuration.file.YamlConfiguration;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.LoggerUtil;

import java.io.File;
import java.util.logging.Logger;

@Deprecated(since = "1.2.0")
public class YamlFile extends YamlConfiguration {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final File file;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public YamlFile(File file) {
        this.file = file;

        try {
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            load(file);
        }
        catch (Throwable exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
        }
    }


    public File getFile() {
        return file;
    }

    public void reload() {
        try {
            load(file);
        }
        catch (Throwable exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
        }
    }

    public void save() {
        try {
            save(file);
        }
        catch (Throwable exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
        }
    }

}
