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

import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.Function;

import static com.epicnicity322.yamlhandler.YamlHandlerUtil.convertToConfigurationSectionNodes;

public class ConfigurationSection
{
    /**
     * A value to serve as an identifier to {@code null} when setting nodes through {@link #set(String, Object)}.
     */
    public static final @NotNull Object NULL_VALUE = new Object();
    private final @NotNull LinkedHashMap<String, Object> nodes;
    private final @NotNull Map<String, Object> unmodifiableNodes;
    private final transient @NotNull HashMap<String, Object> cache = new HashMap<>();
    private final @NotNull String name;
    private final @NotNull String path;
    private final @NotNull Configuration root;
    private final @Nullable ConfigurationSection parent;
    private final char sectionSeparator;

    protected ConfigurationSection(@NotNull String name, @NotNull ConfigurationSection parent, @Nullable Map<?, ?> startingNodes)
    {
        this(name, parent, startingNodes, parent.root.getLoader());
    }

    @ApiStatus.Internal
    ConfigurationSection(@NotNull String name, @Nullable ConfigurationSection parent, @Nullable Map<?, ?> nodes, @NotNull ConfigurationLoader loader)
    {
        // Parent can only be null if this is constructed by the root.
        if (parent == null) {
            if (!(this instanceof Configuration))
                throw new IllegalArgumentException("Parent can not be null unless this is a root section.");
            this.root = (Configuration) this;
            this.name = "";
            this.path = "";
            this.sectionSeparator = loader.getSectionSeparator();
        } else {
            this.root = parent.getRoot();
            this.name = name;
            this.sectionSeparator = parent.sectionSeparator;
            this.path = parent instanceof Configuration ? name : parent.path + sectionSeparator + name;
        }

        this.parent = parent;

        int initialCapacity = nodes == null ? 2 : (int) (nodes.size() / .75f) + 1;

        this.nodes = new LinkedHashMap<>(initialCapacity);
        this.unmodifiableNodes = Collections.unmodifiableMap(this.nodes);

        if (nodes != null) convertToConfigurationSectionNodes(loader, this, nodes, this.nodes);
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
     * @return The parent section or null if this section is the root.
     * @since 1.0
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
    public final @NotNull Configuration getRoot()
    {
        return root;
    }

    /**
     * Gets the character used for separating nodes on this {@link Configuration}.
     * <p>
     * This value is guaranteed to be the same as it was when this class was instanced, unlike
     * {@link ConfigurationLoader#getSectionSeparator()} that immutability is not guaranteed.
     *
     * @return the section separator char
     */
    public final char getSectionSeparator()
    {
        return sectionSeparator;
    }

    /**
     * Builds a *flat* view of the configuration tree that starts at this
     * section, returning every node in a single {@link LinkedHashMap}.
     * <strong>How the map is populated</strong>
     * <ul>
     *   <li>The **key** is the node’s <em>absolute path</em>—all path segments
     *       joined with the character returned by {@link #getSectionSeparator()}.</li>
     *   <li>The **value** is the object stored at that node.
     *       If the node is an otherwise-empty {@code ConfigurationSection},
     *       the section instance itself is stored as the value.</li>
     * </ul>
     * The map is created on every call, so modifications to the returned map do <em>not</em> affect the underlying
     * configuration, and vice versa. Insertion order corresponds to a depth-first traversal of the tree, making the
     * output predictable and suitable for serialization.
     *
     * <h4>Example</h4>
     *
     * <pre>
     * {@code
     *     logging:
     *       file:
     *         path: "/var/log/app.log"
     *         maxSizeMB: 10
     *       console: true
     *     version: 1
     * }
     * </pre>
     * <p>
     * Calling {@code getAbsoluteNodes()} on the root section produces:
     *
     * <pre>
     * {
     *   "logging.file.path"      = "/var/log/app.log",
     *   "logging.file.maxSizeMB" = "10",
     *   "logging.console"        = true,
     *   "version"                = 1
     * }
     * </pre>
     *
     * @return a new, modifiable {@code LinkedHashMap}&lt;String,Object&gt;
     * containing every absolute node rooted at this section
     */
    public @NotNull LinkedHashMap<String, Object> getAbsoluteNodes()
    {
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        YamlHandlerUtil.getAbsoluteNodes(this, output);
        return output;
    }

    /**
     * Gets the nodes that are only inside this section. If the node is a {@link ConfigurationSection} then the name of
     * the section is the key and the object is the {@link ConfigurationSection} containing its inner nodes.
     *
     * @return An unmodifiable map with the nodes of this section.
     */
    @UnmodifiableView
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
        return get(path, true) != null;
    }

    /**
     * Recursively copies every node from the supplied {@code section} into this section, overwriting nodes with
     * matching keys.
     * <p>
     * Nested sections in {@code section} are converted to new instances of {@link ConfigurationSection}, so that the
     * hierarchy behavior is preserved.
     *
     * @param section source section whose nodes should be merged into this one
     * @since 1.5
     */
    public void putAll(@NotNull ConfigurationSection section)
    {
        putAll(section.nodes);
    }

    /**
     * Adds all key-value pairs from the given {@code nodes} map into this configuration section.
     * <p>
     * Special handling:
     * <ul>
     *   <li>If a value in the map is a nested {@link java.util.Map Map} or {@link ConfigurationSection},
     *       it is recursively converted into a new {@link ConfigurationSection} with the correct parent hierarchy.</li>
     *   <li>Otherwise, the value is assigned directly at the corresponding key.</li>
     * </ul>
     *
     * @param nodes a map of keys and values to add as nodes in this section
     * @since 1.5
     */
    public void putAll(@NotNull Map<?, ?> nodes)
    {
        convertToConfigurationSectionNodes(root.getLoader(), this, nodes, this.nodes);
    }

    /**
     * Associates a value with the node identified by the given {@code path}.
     * <p>
     * Behaviour rules:
     * <ul>
     *   <li>If the intermediate section(s) in {@code path} do not yet exist, they are created automatically.</li>
     *   <li>If {@code value == null}, the node (or entire section) addressed by {@code path} is removed.</li>
     *   <li>If <code>value == {@link #NULL_VALUE}</code>, the path is assigned a {@code null} value.</li>
     *   <li>If {@code value} is a {@link ConfigurationSection}, a new section is created at {@code path} and all of
     *   that section’s nodes are copied into it via {@link #createSection(String, Map)}.</li>
     *   <li>If {@code value} is a {@link java.util.Map Map}, a new section is created at {@code path} and its key-value
     *   entries are added using {@link #createSection(String, Map)}.</li>
     *   <li>Otherwise, any existing value at {@code path} is replaced with {@code value}.</li>
     * </ul>
     *
     * @param path  hierarchical path to the target node, whose segments are separated by the configured
     *              section-separator character
     * @param value value to assign, or {@code null} to delete the node/section
     * @param <T>   compile-time type of {@code value}; the same reference is returned for fluent usage
     * @return the same object reference passed in as {@code value}
     * @implNote The node cache for the final segment of {@code path} is cleared before mutation.
     * @since 1.0
     */
    @Contract("_,_ -> param2")
    public <T> T set(@NotNull String path, @Nullable T value)
    {
        if (value instanceof ConfigurationSection) {
            createSection(path, ((ConfigurationSection) value).nodes);
            return value;
        }
        if (value instanceof Map) {
            createSection(path, (Map<?, ?>) value);
            return value;
        }

        StringTokenizer tokens = new StringTokenizer(path, Character.toString(sectionSeparator));
        ConfigurationSection current = this;

        while (tokens.hasMoreTokens()) {
            String part = tokens.nextToken();

            if (tokens.hasMoreTokens()) {
                current = current.createSection(part);
            } else {
                current.removeCaches(part);
                if (value == null) current.nodes.remove(part);
                else current.nodes.put(part, value == NULL_VALUE ? null : value);
            }
        }

        return value;
    }

    /**
     * Creates a new empty section or gets an existing section on the given path. If the path has section separators, the
     * unexisting sections are created automatically.
     * <p>
     * Any intermediate value at the path that's not an existing {@link ConfigurationSection}, gets replaced by a new
     * section.
     *
     * @param path The path to create the section or get the already existing one.
     * @return The created section.
     * @see #createSection(String, Map)
     * @since 1.0
     */
    public @NotNull ConfigurationSection createSection(@NotNull String path)
    {
        return createSection(path, null);
    }

    /**
     * Creates a new section or gets an existing section on the given path. If the path has section separators, the
     * unexisting sections are created automatically.
     * <p>
     * Any intermediate value at the path that's not an existing {@link ConfigurationSection}, gets replaced by a new
     * section.
     * <p>
     * If the section did not exist, it is created preloaded with the specified nodes on {@code nodes} parameter. Any
     * nested {@link Map} or {@link ConfigurationSection} will be converted to new instances of
     * {@link ConfigurationSection}.
     *
     * @param path The path to create the section or get the already existing one.
     * @return The created section.
     * @since 1.5
     */
    public @NotNull ConfigurationSection createSection(@NotNull String path, @Nullable Map<?, ?> nodes)
    {
        StringTokenizer tokens = new StringTokenizer(path, Character.toString(sectionSeparator));
        ConfigurationSection current = this;

        while (tokens.hasMoreTokens()) {
            String part = tokens.nextToken();

            if (!(current.nodes.get(part) instanceof ConfigurationSection)) {
                ConfigurationSection newSection = new ConfigurationSection(part, current, nodes);

                current.removeCaches(part);
                current.nodes.put(part, newSection);
            }

            current = (ConfigurationSection) current.nodes.get(part);
        }

        return current;
    }

    protected void removeCaches(String key)
    {
        // Removing all caches associated to this key.
        cache.keySet().removeIf(cacheKey -> {
            int index = cacheKey.indexOf(sectionSeparator);
            String firstPart = index == -1 ? cacheKey : cacheKey.substring(0, index);

            return firstPart.equals(key);
        });
    }

    protected @Nullable Object get(@NotNull String path)
    {
        return get(path, false);
    }

    private @Nullable Object get(@NotNull String path, boolean useNullValue)
    {
        if (cache.containsKey(path)) {
            Object cached = cache.get(path);
            if (useNullValue && cached == null) return NULL_VALUE;
            return cached;
        }

        StringTokenizer tokens = new StringTokenizer(path, Character.toString(sectionSeparator));
        ConfigurationSection section = this;

        while (tokens.hasMoreTokens()) {
            String key = tokens.nextToken();
            Object result = section.nodes.get(key);

            if (tokens.hasMoreTokens()) {
                if (result instanceof ConfigurationSection) section = (ConfigurationSection) result;
                else return null; // Intermediate node not a section
            } else {
                if (section.nodes.containsKey(key)) cache.put(path, result);
                if (useNullValue && result == null) return NULL_VALUE;
                return result;
            }
        }

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

        if (value instanceof Boolean) result = (Boolean) value;

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
        else result = Collections.emptyList();

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
        if (!(value instanceof Collection)) return new ArrayList<>();

        Collection<?> collection = (Collection<?>) value;
        ArrayList<T> result = new ArrayList<>(collection.size());

        for (Object object : collection) result.add(transformer.apply(object));
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
     * Gets an {@link Object} if one exists on this path.
     * <p>
     * If the object exists on the path and has {@code null} assigned to it, {@link #NULL_VALUE} is the value on the
     * returned {@link Optional}.
     * <p>
     * This object can be an instance of any type that is supported by the root's {@link ConfigurationLoader}, which may
     * not be provided by a dedicated method of this class. It can also be a {@link ConfigurationSection}, or an object
     * manually set with {@link #set(String, Object)} and {@link #putAll(Map)}.
     *
     * @param path The path with sections separated by section separator char.
     * @return Optional with the object set on the path.
     * @see #contains(String)
     * @since 1.5
     */
    public @NotNull Optional<Object> getNullableObject(@NotNull String path)
    {
        Object result = get(path, true);
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

        return path.equals(that.path) && nodes.equals(that.nodes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(path, nodes);
    }
}
