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

import com.epicnicity322.yamlhandler.loaders.ConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Configuration extends ConfigurationSection
{
    private final @Nullable Path filePath;
    private final @NotNull ConfigurationLoader loader;
    private @Nullable Map<String, Comment> comments;

    /**
     * Creates a configuration holder with no nodes.
     *
     * @param loader The loader used to get the configuration loading and saving options.
     * @see #Configuration(ConfigurationLoader, Map)
     * @since 1.5
     */
    public Configuration(@NotNull ConfigurationLoader loader)
    {
        this(loader, null);
    }

    /**
     * Creates a configuration holder with a preloaded map of nodes.
     *
     * @param loader The loader used to get the configuration loading and saving options.
     * @param nodes  The map of nodes, any nested {@link Map} or {@link ConfigurationSection} will be converted to new instances of {@link ConfigurationSection}.
     * @throws IllegalArgumentException If the map has a key which has no tokens, according to the behavior of {@link java.util.StringTokenizer} using {@link #getSectionSeparator()} as delimiter.
     * @throws IllegalArgumentException If a value of the map has failed to be deserialized by one of the {@link ConfigurationLoader}'s {@link com.epicnicity322.yamlhandler.serializers.CustomSerializer custom serializers}.
     * @since 1.5
     */
    public Configuration(@NotNull ConfigurationLoader loader, @Nullable Map<?, ?> nodes)
    {
        this(null, nodes, null, loader);
    }

    protected Configuration(@Nullable Path filePath, @Nullable Map<?, ?> nodes, @Nullable Map<String, Comment> comments, @NotNull ConfigurationLoader loader)
    {
        super("", null, nodes, loader);

        this.filePath = filePath;
        this.loader = loader;
        this.comments = comments;
    }

    /**
     * Gets the file holding this configuration. If this configuration was not loaded by a file, the {@link Optional} is
     * empty.
     *
     * @return The path of the file holding this configuration.
     * @since 1.0
     */
    public @NotNull Optional<Path> getFilePath()
    {
        return Optional.ofNullable(filePath);
    }

    public @NotNull ConfigurationLoader getLoader()
    {
        return loader;
    }

    /**
     * Save the configuration to the specified path. If the path or the parent directory does not exist, it will be
     * created automatically. If the path already exists, the configuration data will be appended to the end of the
     * file.
     *
     * @param path Path of the file to save the configuration in.
     * @throws IOException              If failed to save the configuration to the given path.
     * @throws IllegalArgumentException If the path is pointing to an existing directory.
     * @since 1.0
     */
    public void save(@NotNull Path path) throws IOException
    {
        // Check if the path is pointing to an existing directory
        if (Files.isDirectory(path)) throw new IllegalArgumentException("Given path is a directory");

        // Create the parent directories if they don't exist
        Path parent = path.getParent();

        // If the given path does not have a parent then "parent" variable will be null.
        if (parent != null && Files.notExists(parent)) Files.createDirectories(parent);

        // Create the path if it doesn't exist
        if (Files.notExists(path)) Files.createFile(path);

        // Append the data to the path
        Files.write(path, dump().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    /**
     * Serializes the nodes and sections to a readable configuration text.
     *
     * @return The contents of this configuration as string.
     * @since 1.0
     */
    public @NotNull String dump()
    {
        return loader.dump(this, comments);
    }

    /**
     * Represents the nodes and sections as a new map with the keys as the node's name, and value as the value stored by
     * the node.
     * <p>
     * Inner sections and objects which the loader has assigned a {@link com.epicnicity322.yamlhandler.serializers.CustomSerializer custom serializer}
     * are also included in the returned map as an inner map object.
     *
     * @return a new, mutable map representing this configuration's nodes
     * @since 1.5
     */
    public @NotNull Map<String, Object> asMap()
    {
        return ConfigurationUtil.convertToMapNodes(this, true);
    }

    /**
     * Sets a comment on the specified node, with the possibility to inline it on the same line as the declaration.
     * <p>
     * A comment 'Hello World' set on the path 'A.B' will make so the configuration is dumped as the following example:
     * <ul>
     *     <li>
     *         {@code inline = true}
     *         <pre>
     *             {@code
     *             A:
     *               B: true #Hello World
     *             }
     *         </pre>
     *     </li>
     *     <li>
     *         {@code inline = false}
     *         <pre>
     *             {@code
     *             A:
     *               #Hello World
     *               B: true
     *             }
     *         </pre>
     *     </li>
     * </ul>
     *
     * @param path    hierarchical path to the target node, whose segments are separated by the configured
     *                section-separator character
     * @param comment the comment to be assigned to the specified node, or {@code null} to remove any previously
     *                assigned comment
     * @param inline  whether to place the comment on the same line as the node declaration
     * @throws IllegalArgumentException If the path has no tokens, according to the behavior of {@link StringTokenizer} using {@link #getSectionSeparator()} as delimiter.
     * @since 1.5
     */
    public void setComment(@NotNull String path, @Nullable String comment, boolean inline)
    {
        StringTokenizer tokens = new StringTokenizer(path, Character.toString(getSectionSeparator()));
        if (!tokens.hasMoreTokens()) throw new IllegalArgumentException("Invalid path has no tokens: '" + path + '\'');
        if (comment == null && comments == null) return;

        StringJoiner joiner = new StringJoiner(Character.toString(getSectionSeparator()));
        while (tokens.hasMoreTokens()) joiner.add(tokens.nextToken());

        String commentPath = joiner.toString();
        Comment previous = comments == null ? Comment.NULL_COMMENT : comments.getOrDefault(commentPath, Comment.NULL_COMMENT);
        Comment newComment = Comment.of(inline ? previous.blockComment() : comment, inline ? comment : previous.inlineComment());
        if (newComment.blockComment() == null && newComment.inlineComment() == null) newComment = null;

        if (newComment == null) removeComment(commentPath);
        else putComment(commentPath, newComment);
    }

    private void putComment(@NotNull String commentPath, @NotNull Comment comment)
    {
        if (comments == null) this.comments = new HashMap<>();
        comments.put(commentPath, comment);
    }

    private void removeComment(@NotNull String commentPath)
    {
        if (comments != null) {
            comments.remove(commentPath);
            if (comments.isEmpty()) this.comments = null;
        }
    }

    /**
     * Checks if the specified object is a {@link Configuration} with the same nodes and the same path.
     *
     * @param o The object to compare.
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Configuration)) return false;

        Configuration that = (Configuration) o;

        return Objects.equals(filePath, that.filePath) && getNodes().equals(that.getNodes());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(filePath, getNodes());
    }
}
