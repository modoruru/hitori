# ひとり
hitori - "alone" from japanese

Hitori is minecraft server-side framework based on PaperMC plugin.\
The main goal of *hitori* is to implement reloadable modules to simplify and speed up development process.

## How it works?
The main problem with reloading in Java is removing old classes from runtime.
This problem can be solved by creating new ClassLoader each time module loads, and removing it before reloading.\

Of course, anything loaded and registered by module in runtime must be disabled/unregistered before removing classes from RAM.\
This is what *hitori* tries to do by providing an API for registering such things.

## Main features
- Modules
- Safe bukkit listeners and commands (via CommandAPI) registration
- Registries for custom content
- Many utils
- Logging system with modules support