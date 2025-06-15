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
import com.epicnicity322.yamlhandler.serializers.CustomSerializer;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loads YAML configuration sources into {@link Configuration} instances.
 * <p>
 * This loader wraps <a href="https://bitbucket.org/snakeyaml/snakeyaml">SnakeYAML</a>, pre-configuring
 * {@link org.yaml.snakeyaml.DumperOptions} and {@link org.yaml.snakeyaml.representer.Representer} with sensible
 * defaults while exposing a small surface for customization.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Section navigation</strong> – Hierarchical keys are delimited by a configurable
 *   <em>section separator</em> (default: {@code '.'}).</li>
 *   <li><strong>Pluggable serializers</strong> – Register one or more {@linkplain CustomSerializer custom serializers}
 *   to marshal types that YAML cannot represent directly.</li>
 *   <li><strong>Indent &amp; flow‐style control</strong> – Tune aesthetics with an indent range of 1-10 spaces and any
 *   {@link org.yaml.snakeyaml.DumperOptions.FlowStyle}.</li>
 * </ul>
 *
 * <h2>Default configuration</h2>
 * <table border="0" cellpadding="2" cellspacing="0">
 *   <tr><th align="left">Option</th><th align="left">Value</th></tr>
 *   <tr><td>Section separator</td><td>{@code '.'}</td></tr>
 *   <tr><td>Indent size</td><td>{@code 2}</td></tr>
 *   <tr><td>Flow style</td><td>{@link org.yaml.snakeyaml.DumperOptions.FlowStyle#BLOCK BLOCK}</td></tr>
 * </table>
 *
 * <h2>Thread-safety note</h2>
 * Instances of {@code YamlConfigurationLoader} are <em>not</em> thread-safe.
 * Either restrict an instance to a single thread or provide external synchronization when sharing.
 *
 * @see CustomSerializer
 * @see ConfigurationLoader
 * @since 1.0
 */
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
     * The defined serializers for specific types of classes.
     */
    private final @NotNull CustomSerializer<?>[] customSerializers;

    /**
     * Constructs a loader with the <a href="#default-configuration">default options</a>.
     * <p>
     * Equivalent to invoking {@link #YamlConfigurationLoader(char, int, org.yaml.snakeyaml.DumperOptions.FlowStyle)
     * YamlConfigurationLoader('.', 2, FlowStyle.BLOCK)}.
     *
     * @since 1.0
     */
    public YamlConfigurationLoader()
    {
        this(new CustomSerializer[0]);
    }

    /**
     * Constructs a loader with the default formatting options and a set of <em>custom</em> serializers.
     * <p>
     * Useful when you want to load configurations containing bespoke object graphs but are satisfied with the standard
     * separator, indent, and flow style.
     *
     * @param customSerializers one or more serializers to register
     * @throws NullPointerException if {@code customSerializers} is {@code null}
     * @since 1.5
     */
    public YamlConfigurationLoader(@NotNull CustomSerializer<?>... customSerializers)
    {
        this('.', 2, DumperOptions.FlowStyle.BLOCK, customSerializers);
    }

    /**
     * Constructs a loader with caller-supplied formatting options.
     *
     * @param sectionSeparator the character used to denote section boundaries
     *                         inside compound keys
     * @param indentSize       number of spaces to indent nested YAML nodes;
     *                         must be between 1 and 10 inclusive
     * @param flowStyle        the default flow style to apply when dumping YAML
     * @throws IllegalArgumentException if {@code indentSize} is outside the 1-10 range
     * @throws NullPointerException     if {@code flowStyle} is {@code null}
     * @since 1.0
     */
    public YamlConfigurationLoader(char sectionSeparator, @Range(from = 1, to = 10) int indentSize, @NotNull DumperOptions.FlowStyle flowStyle)
    {
        this(sectionSeparator, indentSize, flowStyle, new CustomSerializer[0]);
    }

    /**
     * Constructs a loader with caller-supplied formatting options <em>and</em> an explicit registry of custom
     * serializers.
     *
     * @param sectionSeparator  the character used to denote section boundaries
     *                          inside compound keys
     * @param indentSize        number of spaces to indent nested YAML nodes;
     *                          must be between 1 and 10 inclusive
     * @param flowStyle         the default flow style to apply when dumping YAML
     * @param customSerializers an array of serializers keyed by their
     *                          {@link CustomSerializer#type() handled type};
     * @throws IllegalArgumentException if {@code indentSize} is outside the 1-10 range
     * @throws NullPointerException     if {@code flowStyle} or
     *                                  {@code customSerializers} is {@code null}
     * @since 1.5
     */
    public YamlConfigurationLoader(char sectionSeparator, @Range(from = 1, to = 10) int indentSize, @NotNull DumperOptions.FlowStyle flowStyle, @NotNull CustomSerializer<?> @NotNull ... customSerializers)
    {
        this.customSerializers = Arrays.copyOf(customSerializers, customSerializers.length);
        this.sectionSeparator = sectionSeparator;

        // Set the indent size
        this.options.setIndent(indentSize);

        // Set the default flow style
        this.options.setDefaultFlowStyle(flowStyle);
        this.representer.setDefaultFlowStyle(flowStyle);
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
     * @throws IllegalArgumentException      If the path is a directory or a non-readable file.
     * @see #load(String contents)
     */
    public @NotNull Configuration load(@NotNull Path path) throws IOException, InvalidConfigurationException
    {
        if (Files.isDirectory(path)) throw new IllegalArgumentException("Given path is a directory");
        if (!Files.isReadable(path)) throw new IllegalArgumentException("Given path is not readable");

        try {
            return new Configuration(path, yaml.load(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)), this);
        } catch (YAMLException | IllegalArgumentException e) {
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
        } catch (YAMLException | IllegalArgumentException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }
    }

    @Override
    public @NotNull String dump(@NotNull Configuration configuration)
    {
        return yaml.dump(nodes);
        return yaml.dump(ConfigurationUtil.convertToMapNodes(configuration, true));
    }

    @Override
    public @NotNull CustomSerializer<?>[] getCustomSerializers()
    {
        return Arrays.copyOf(customSerializers, customSerializers.length);
    }

    @Override
    public char getSectionSeparator()
    {
        return sectionSeparator;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof YamlConfigurationLoader)) return false;

        YamlConfigurationLoader that = (YamlConfigurationLoader) o;

        return sectionSeparator == that.sectionSeparator && options.getIndent() == that.options.getIndent() && options.getDefaultFlowStyle() == that.options.getDefaultFlowStyle() && Arrays.equals(customSerializers, that.customSerializers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sectionSeparator, options.getIndent(), options.getDefaultFlowStyle(), Arrays.hashCode(customSerializers));
    }
}
