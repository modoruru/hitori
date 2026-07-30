# ひとり
hitori - "alone" (adverb) from japanese

hitori is a server-side framework for Minecraft based on the PaperMC plugin.\
The main goal of *hitori* is to implement reloadable modules to simplify and speed up the development process.

## How it works?
The main problem with reloading in Java is removing old classes from runtime.
This problem can be solved by creating new ClassLoader each time module loads, and removing old one before reloading.\

Of course, anything loaded and registered by module in runtime must be disabled/unregistered before removing classes from RAM.\
This is what *hitori* tries to do by providing an API for registering such things.

## Main features
- Modules
- Safe bukkit listeners and commands (via [CommandAPI](https://github.com/CommandAPI/CommandAPI)) registration
- More convenient logging system for modules
- Many utils regarding I/O, tasks scheduling with [Folia](https://github.com/PaperMC/Folia) support, creating YAML configurations, etc.

## Usage
You can get a jar from [Actions](https://github.com/modoruru/hitori/actions) tab. hitori is built almost every commit.\
Also, you can get module from [Releases](https://github.com/modoruru/hitori/releases) (if there's any).

After downloading the jar, just put it into plugins folder.

## API
> [!NOTE]
> We DO NOT include Paper API in our .pom, so you should add it by yourself.

<details>
<summary>maven</summary>

```xml
<repository>
    <id>modoru-releases</id>
    <name>modoru repository</name>
    <url>https://repository.modoru.fun/releases</url>
</repository>
```

```xml
<dependency>
    <groupId>su.hitori</groupId>
    <artifactId>hitori</artifactId>
    <version>1.2.1</version>
</dependency>
```
</details>
<details>
<summary>gradle</summary>

```groovy
maven {
    name = "modoruReleases"
    url = uri("https://repository.modoru.fun/releases")
}
```

```groovy
implementation("su.hitori:hitori:1.2.1")
```
</details>

## Credits
[justlofe](https://github.com/justlofe) - lead developer and maintainer\
[StreamVersus](https://github.com/StreamVersus) - wrote several utils\
[suiteark](https://github.com/suiteark) - stress tester for stability testing