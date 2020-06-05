/*
 * Copyright (c) 2020 Christiano Rangel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epicnicity322.yamlhandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Configuration extends ConfigurationSection
{
    private final @NotNull YamlConfigurationLoader loader;

    public Configuration(@NotNull YamlConfigurationLoader loader)
    {
        super("", "", null, new LinkedHashMap<>(), Objects.requireNonNull(loader).sectionSeparator);

        this.loader = loader;
    }

    protected Configuration(@NotNull String name, @NotNull String path, @NotNull YamlConfigurationLoader loader,
                            @NotNull Map<?, ?> nodes)
    {
        super(name, path, null, nodes, loader.sectionSeparator);

        this.loader = loader;
    }

    private static void convertToMapNodes(ConfigurationSection input, Map<String, Object> output)
    {
        for (Map.Entry<String, Object> entry : input.getNodes().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();

                convertToMapNodes((ConfigurationSection) value, map);
                value = map;
            }

            output.put(key, value);
        }
    }

    @Override
    public @NotNull Configuration getRoot()
    {
        return this;
    }

    /**
     * Save the configuration to the specified path. If the path or the parent path does not exist it will be created
     * automatically. If the path already exists, the configuration data will be appended to the end of this file.
     *
     * @param path Path of the file to save the configuration in.
     * @throws IOException              If failed to save the configuration to the given path.
     * @throws IllegalArgumentException If the path is pointing to a directory.
     */
    public void save(@NotNull Path path) throws IOException
    {
        // Check if the path is pointing to a existing directory
        if (Files.isDirectory(Objects.requireNonNull(path, "path is null")))
            throw new IllegalArgumentException("Given path is a directory");

        // Create the parent directories if they don't exist
        Path parent = path.getParent();

        // If "path" parameter does not have a parent then "parent" variable will be null.
        if (parent != null && Files.notExists(parent))
            Files.createDirectories(parent);

        // Create the path if it doesn't exist
        if (Files.notExists(path))
            Files.createFile(path);

        // Append the data to the path
        Files.write(path, dump().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    /**
     * Serializes the nodes and sections to a readable YAML text.
     *
     * @return The contents of this YAML.
     */
    public String dump()
    {
        LinkedHashMap<String, Object> mapNodes = new LinkedHashMap<>();

        convertToMapNodes(this, mapNodes);
        return loader.yaml.dump(mapNodes);
    }
}
