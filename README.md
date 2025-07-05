# Yaml Handler

A YAML configuration manager that aims to simplify reading and writing of YAML configurations, while being
independent of any Minecraft modding platform.

## Features

* Load and save YAML configurations from/to strings and files.
* Simple design for getting/setting keys and nodes.
* Uses [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) by default.
* Supports comments.
* Set block comments to sections and lists, or block and inline comments to nodes.
* Reads comments from existing configurations and maintains them.
* Supports NULL values.
* Custom section separators.
* Custom serializers that can serialize your class into readable YAML sections and automatically deserialize them back.
* Native support for
  spigot's [ConfigurationSerializable](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/configuration/serialization/ConfigurationSerializable.html).

## Why does this exist?

I often write Minecraft mods to many platforms, like Spigot, Sponge, Fabric and Forge. Each platform provides their own
configuration library, however, you have to adapt your code to read and write from each platform, and that can be
time-consuming and tedious.

With Yaml Handler, you are able to use this one library in any platform. By including SnakeYAML, it is possible to avoid
having to resort to JSON, and make your mod much simpler to be configured by the end user.

## Gradle/maven/sbt/leiningen dependency

Yaml Handler can be added as dependency through [jitpack](https://jitpack.io/#chrisrnj/yaml-handler) repository.
If you are using maven, add this repository and dependency. Latest available
version: [![](https://jitpack.io/v/chrisrnj/yaml-handler.svg)](https://jitpack.io/#chrisrnj/yaml-handler)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.chrisrnj</groupId>
        <artifactId>yaml-handler</artifactId>
        <version>-SNAPSHOT</version>
    </dependency>
</dependencies>
```