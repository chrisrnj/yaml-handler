/*
 * Copyright (c) 2020-2025 Christiano Rangel
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

import com.epicnicity322.yamlhandler.exceptions.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

public class YamlConfigurationLoader implements ConfigurationLoader
{
    /**
     * The char separating the sections.
     */
    protected final char sectionSeparator;

    /**
     * YAML dumper options.
     */
    private final @NotNull DumperOptions options = new DumperOptions();

    /**
     * YAML representer.
     */
    private final @NotNull Representer representer = new Representer(options);

    /**
     * YAML instance, holding the configuration.
     */
    protected final @NotNull Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), representer, options);

    /**
     * Creates a {@link YamlConfigurationLoader} with default options.
     * <p>
     * Defaults:
     * - Section separator char: '.'
     * - Indent size: 2
     * - Dumper flow style: {@link DumperOptions.FlowStyle#BLOCK}
     */
    public YamlConfigurationLoader()
    {
        this.sectionSeparator = '.';

        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }

    /**
     * Creates a {@link YamlConfigurationLoader} with specific options.
     *
     * @param sectionSeparator The separator char used to distinguish section breaks.
     * @param indentSize       The amount of spaces to indent each nested node line.
     * @param flowStyle        The flow style the YAML should have when dumped as string.
     */
    public YamlConfigurationLoader(char sectionSeparator, @Range(from = 1, to = 10) int indentSize, @NotNull DumperOptions.FlowStyle flowStyle)
    {
        this.sectionSeparator = sectionSeparator;

        // Set the indent size
        options.setIndent(indentSize);

        // Set the default flow style
        options.setDefaultFlowStyle(flowStyle);
        representer.setDefaultFlowStyle(flowStyle);
    }

    /**
     * Reads the contents of a reader and loads a configuration from it.
     *
     * @param reader The reader to read the configuration from.
     * @return The loaded configuration.
     * @throws IOException                   If failed to read from this reader.
     * @throws InvalidConfigurationException If something went wrong while parsing the reader data as yaml.
     * @see #load(Path)
     */
    public @NotNull Configuration load(@NotNull Reader reader) throws IOException, InvalidConfigurationException
    {
        String data;

        try (BufferedReader input = new BufferedReader(reader)) {
            data = input.lines().collect(Collectors.joining("\n"));
        }

        return load(data);
    }

    /**
     * Reads the contents of a path and loads a configuration from it.
     *
     * @param path Path of the file holding the configuration to load.
     * @throws IOException                   Thrown if failed to load the configuration.
     * @throws InvalidConfigurationException If something went wrong while parsing the path contents as yaml.
     * @throws IllegalArgumentException      If the path is a directory or not a readable file.
     * @see #load(String contents)
     */
    public @NotNull Configuration load(@NotNull Path path) throws IOException, InvalidConfigurationException
    {
        if (Files.isDirectory(path)) throw new IllegalArgumentException("Given path is a directory");
        if (!Files.isReadable(path)) throw new IllegalArgumentException("Given path is not readable");

        try {
            return new Configuration(path, yaml.load(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)), this);
        } catch (YAMLException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }
    }

    /**
     * Loads the configuration from the given string.
     *
     * @param contents Configuration string.
     * @throws InvalidConfigurationException If something went wrong while parsing the string as yaml.
     */
    public @NotNull Configuration load(@NotNull String contents) throws InvalidConfigurationException
    {
        try {
            return new Configuration(null, yaml.load(contents), this);
        } catch (YAMLException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof YamlConfigurationLoader)) return false;

        YamlConfigurationLoader that = (YamlConfigurationLoader) o;

        return sectionSeparator == that.sectionSeparator && options.getIndent() == that.options.getIndent() && options.getDefaultFlowStyle() == that.options.getDefaultFlowStyle();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sectionSeparator, options.getIndent(), options.getDefaultFlowStyle());
    }
}
