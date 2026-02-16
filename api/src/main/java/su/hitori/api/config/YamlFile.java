package su.hitori.api.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlFile extends YamlConfiguration {

    private final File file;

    public YamlFile(File file) {
        this.file = file;

        try {
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            load(file);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public File getFile() {
        return file;
    }

    public void reload() {
        if(file != null) {
            try {
                load(file);
            }
            catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try {save(file);}
        catch (IOException e) {e.printStackTrace();}
    }

}
