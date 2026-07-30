package su.hitori.api.command;

import java.util.UUID;

/**
 * Created during {@link CommandsRegistryModifier#applyModificationsInBatch}
 * @see CommandsRegistryModifier#undoBatch(CommandsModificationInfo)
 */
public interface CommandsModificationInfo {

    UUID uuid();

    boolean active();

}
