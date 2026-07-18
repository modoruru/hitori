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
hitori's API is published via [JitPack](https://jitpack.io). Latest version: [![](https://jitpack.io/v/modoruru/hitori.svg)](https://jitpack.io/#modoruru/hitori)
<details>
<summary>maven</summary>

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

```xml
	<dependency>
	    <groupId>com.github.modoruru</groupId>
	    <artifactId>hitori</artifactId>
	    <version>version</version>
	</dependency>
```
</details>
<details>
<summary>gradle</summary>

```groovy
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}
```

```groovy
dependencies {
    // ...
    implementation 'com.github.modoruru:hitori:version'
}
```
</details>