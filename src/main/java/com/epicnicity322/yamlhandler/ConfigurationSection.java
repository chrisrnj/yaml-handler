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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ConfigurationSection
{
    private final @NotNull LinkedHashMap<String, Object> nodes = new LinkedHashMap<>();
    private final @NotNull Map<String, Object> unmodifiableNodes = Collections.unmodifiableMap(nodes);
    private final @NotNull HashMap<String, Object> cache = new HashMap<>();
    private final @NotNull String name;
    private final @NotNull String path;
    private final @Nullable ConfigurationSection parent;
    private final Configuration root;
    private final char sectionSeparator;

    protected ConfigurationSection(@NotNull String name, @NotNull String path, @Nullable ConfigurationSection parent, @Nullable Map<?, ?> nodes, char sectionSeparator)
    {
        this.name = name;
        this.path = path;
        this.parent = parent;
        this.sectionSeparator = sectionSeparator;

        // Parent can only be null if this is constructed by the root.
        this.root = parent == null ? null : parent.getRoot();

        if (nodes != null)
            YamlHandlerUtil.convertToConfigurationSectionNodes(sectionSeparator, this, nodes, this.nodes);
    }

    /**
     * Gets the name of this configuration section. If this is the root section the path is an empty string.
     *
     * @return The name of this section.
     * @see Configuration#getFilePath()
     */
    public @NotNull String getName()
    {
        return name;
    }

    /**
     * The path of this configuration section. If this is the root section the path is an empty string.
     *
     * @return The path to this section with its name included.
     * @see Configuration#getFilePath()
     */
    public @NotNull String getPath()
    {
        return path;
    }

    /**
     * The parent section this section is nested in.
     *
     * @return The parent section or null if this section has no parent.
     */
    public @Nullable ConfigurationSection getParent()
    {
        return parent;
    }

    /**
     * Gets the root {@link Configuration} containing this configuration section and all other siblings.
     *
     * @return The root {@link Configuration}.
     */
    public @NotNull Configuration getRoot()
    {
        return root;
    }

    /**
     * Gets the separator char used to distinguish sections on a path.
     *
     * @return The section separation char.
     */
    public char getSectionSeparator()
    {
        return sectionSeparator;
    }

    /**
     * Gets the nodes from this section to the end. Which means this map will have no {@link ConfigurationSection} as
     * values, all the keys are the name of the configuration sections separated by section separator char.
     *
     * @return An immutable map with all the absolute nodes of this section.
     */
    public @NotNull LinkedHashMap<String, Object> getAbsoluteNodes()
    {
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();

        YamlHandlerUtil.getAbsoluteNodes(sectionSeparator, this, output);

        return output;
    }

    /**
     * Gets the nodes that are only inside this section. If the node is a {@link ConfigurationSection} then the name of
     * the section is the key and the object is the {@link ConfigurationSection} containing its inner nodes.
     *
     * @return An unmodifiable map with the nodes of this section.
     */
    public @NotNull Map<String, Object> getNodes()
    {
        return unmodifiableNodes;
    }

    /**
     * Tests whether this section contains a value or another section on the specified path.
     * <p>
     * This will return the same as if the {@link Optional} on {@link #getObject(String)} is present, but this is
     * preferred as no {@link Optional} instance is created.
     *
     * @param path The path with sections separated by section separator char.
     * @return If the path exists.
     */
    public boolean contains(@NotNull String path)
    {
        return get(path) != null;
    }

    /**
     * Sets a value in the specified path. If the path is a section path, the value is set on that section. If the section
     * doesn't exist, the section is automatically created. If a value is already assigned to this path, the old value is
     * replaced by the specified value. Set the value to null to delete that node or section.
     *
     * @param path  The path with sections separated by section separator char.
     * @param value The value to be assigned to this path or null to delete the path.
     * @return The value that was set.
     */
    public <T> T set(@NotNull String path, @Nullable T value)
    {
        String[] sections = path.split(Character.toString(sectionSeparator));
        ConfigurationSection section = this;
        int i = 0;

        for (String key : sections) {
            section.removeCaches(key);

            if (++i == sections.length) {
                if (value == null) section.nodes.remove(key);
                else {
                    section.nodes.put(key, value);
                    section.cache.put(key, value);
                }
            } else section = section.createSection(key);
        }

        cache.put(path, value);

        return value;
    }

    /**
     * Creates a new section or get an already existing section on the given path. If the path is a section path, the
     * sections that do not exist are automatically created.
     *
     * @param path The path to create the section or get the already existing one.
     * @return The created section.
     */
    public @NotNull ConfigurationSection createSection(@NotNull String path)
    {
        String[] sections = path.split(Character.toString(sectionSeparator));
        ConfigurationSection section = this;
        int i = 0;

        for (String key : sections) {
            section.removeCaches(key);

            if (++i == sections.length) {
                Object result = section.get(key);

                if (result instanceof ConfigurationSection) section = (ConfigurationSection) result;
                else {
                    String sectionPath;

                    if (section instanceof Configuration) sectionPath = key;
                    else sectionPath = section.getPath() + section.sectionSeparator + key;

                    ConfigurationSection newSection = new ConfigurationSection(key, sectionPath, section, new HashMap<>(), section.sectionSeparator);

                    section.nodes.put(key, newSection);
                    section.cache.put(key, newSection);
                    section = newSection;
                }

                // No need to break but who knows.
                break;
            } else section = section.createSection(key);
        }

        cache.put(path, section);
        return section;
    }

    private void removeCaches(String key)
    {
        // Removing all caches associated to this key.
        cache.keySet().removeIf(cacheKey -> {
            String firstKey;
            int index = cacheKey.indexOf(sectionSeparator);

            if (index == -1) firstKey = cacheKey;
            else firstKey = cacheKey.substring(0, index);

            return firstKey.equals(key);
        });
    }

    private @Nullable Object get(@NotNull String path)
    {
        if (cache.containsKey(path)) return cache.get(path);

        String[] sections = path.split(Character.toString(sectionSeparator));
        ConfigurationSection section = this;
        int i = 1;

        for (String key : sections) {
            Object result = section.nodes.get(key);

            // Returning the result if this is the last key.
            if (i++ == sections.length) {
                // Add result to cache to improve performance.
                if (result != null) cache.put(path, result);

                return result;
            }

            if (result instanceof ConfigurationSection) section = (ConfigurationSection) result;
        }

        // Fun fact: since the loop returns something when the last element is iterated, the code will never reach this.
        return null;
    }

    /**
     * Gets a {@link Boolean} if one exists on this path.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional containing boolean if found.
     */
    public @NotNull Optional<Boolean> getBoolean(@NotNull String path)
    {
        Object value = get(path);
        Boolean result = null;

        if (value instanceof Boolean) result = Boolean.TRUE.equals(value);

        return Optional.ofNullable(result);
    }

    /**
     * Gets a {@link Character} if one exists on this path.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional containing character if found.
     */
    public @NotNull Optional<Character> getCharacter(@NotNull String path)
    {
        Object value = get(path);
        Character result = null;

        if (value instanceof Character) result = (Character) value;

        return Optional.ofNullable(result);
    }

    /**
     * Gets a {@link Collection} in this path. If no collection was found then an empty {@link ArrayList} is returned.
     *
     * @param path The path with sections separated by section separator char.
     * @return The collection in this path.
     */
    public @NotNull Collection<?> getCollection(@NotNull String path)
    {
        Object value = get(path);
        Collection<?> result;

        if (value instanceof Collection) result = (Collection<?>) value;
        else result = new ArrayList<>();

        return result;
    }

    /**
     * Gets a collection in this path, iterates through its elements and applies the transformer function to the
     * objects, then adds the object to an {@link ArrayList}.
     *
     * @param path        The path with sections separated by section separator char.
     * @param transformer The function that when applied, the returned object will be added to the list.
     * @return An {@link ArrayList} containing the elements of the collection or an empty list if no collection was found.
     */
    public @NotNull <T> ArrayList<T> getCollection(@NotNull String path, @NotNull Function<Object, T> transformer)
    {
        Object value = get(path);
        ArrayList<T> result = new ArrayList<>();

        if (value instanceof Collection) for (Object object : (Collection<?>) value)
            result.add(transformer.apply(object));

        return result;
    }

    /**
     * Gets the {@link ConfigurationSection} in the end of the path.
     *
     * @param path The path with sections separated by section separator char.
     * @return The {@link ConfigurationSection} in the end of the path or null if this section does not exist.
     */
    public @Nullable ConfigurationSection getConfigurationSection(@NotNull String path)
    {
        Object value = get(path);
        ConfigurationSection result = null;

        if (value instanceof ConfigurationSection) result = (ConfigurationSection) value;

        return result;
    }

    /**
     * Gets a {@link Number} if one exists on this path.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional containing number if found.
     */
    public @NotNull Optional<Number> getNumber(@NotNull String path)
    {
        Object value = get(path);
        Number result = null;

        if (value instanceof Number) result = (Number) value;

        return Optional.ofNullable(result);
    }

    /**
     * Gets an {@link Object} if one exists on this path. This object can be instance of any type that can be get by
     * SnakeYAML and can also be a {@link ConfigurationSection}.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional containing object if found.
     */
    public @NotNull Optional<Object> getObject(@NotNull String path)
    {
        Object result = get(path);

        return Optional.ofNullable(result);
    }

    /**
     * Gets a {@link String} if one exists on this path.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional containing string if found.
     */
    public @NotNull Optional<String> getString(@NotNull String path)
    {
        Object value = get(path);
        String result = null;

        if (value instanceof CharSequence) result = value.toString();

        return Optional.ofNullable(result);
    }

    /**
     * Gets all the node mapping from this {@link ConfigurationSection} as string.
     *
     * @return String containing all the nodes from this section.
     */
    @Override
    public String toString()
    {
        return nodes.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ConfigurationSection)) return false;

        ConfigurationSection that = (ConfigurationSection) o;

        return nodes.equals(that.nodes) && path.equals(that.path) && Objects.equals(root, that.root);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(nodes, path, root);
    }
}
