package su.hitori.api.module.enable;

import java.util.concurrent.CompletableFuture;

/**
 * Context module enables in. Should be used to register module-specific commands, listeners etc.
 * @param listeners listeners registrar
 * @param commands commands registrar
 * @param hasEnabledBefore has this module enabled before in this session
 * @param enableHooksFuture called when all enable hooks from other modules are finished
 */
public record EnableContext(ListenersRegistrar listeners, CommandsRegistrar commands, boolean hasEnabledBefore, CompletableFuture<Void> enableHooksFuture) {

}
