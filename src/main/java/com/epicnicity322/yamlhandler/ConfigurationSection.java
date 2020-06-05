package com.epicnicity322.yamlhandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ConfigurationSection
{
    private static final @NotNull Pattern allowedPathCharsRegex = Pattern.compile("^[A-Za-z0-9_ -]+$");
    protected final @NotNull HashMap<String, Object> cache = new HashMap<>();
    private final @NotNull LinkedHashMap<String, Object> nodes = new LinkedHashMap<>();
    private final @NotNull String name;
    private final @NotNull String path;
    private final @Nullable ConfigurationSection parent;
    private final @NotNull Pattern sectionSeparatorPattern;
    private final char sectionSeparator;
    private Configuration root;

    protected ConfigurationSection(@NotNull String name, @NotNull String path, @Nullable ConfigurationSection parent,
                                   @NotNull Map<?, ?> nodes, char sectionSeparator)
    {
        this.name = name;
        this.path = path;
        this.parent = parent;
        this.sectionSeparator = sectionSeparator;
        sectionSeparatorPattern = Pattern.compile(Pattern.quote(Character.toString(sectionSeparator)));

        // Parent can only be null if this is constructed by the root.
        if (parent != null)
            root = parent.getRoot();

        convertToConfigurationSectionNodes(this, nodes, this.nodes);
    }

    private static void convertToConfigurationSectionNodes(ConfigurationSection input, Map<?, ?> nodes, Map<String, Object> output)
    {
        for (Map.Entry<?, ?> entry : nodes.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                String path;

                if (input instanceof Configuration)
                    path = key;
                else
                    path = input.getPath() + input.sectionSeparator + key;

                value = new ConfigurationSection(key, path, input, (Map<?, ?>) value, input.sectionSeparator);
            }

            output.put(key, value);
        }
    }

    private static void getAbsoluteNodes(ConfigurationSection input, Map<String, Object> output)
    {
        for (Map.Entry<String, Object> entry : input.nodes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                getAbsoluteNodes((ConfigurationSection) value, output);
            } else {
                String path;

                if (input instanceof Configuration)
                    path = key;
                else
                    path = input.getPath() + input.sectionSeparator + key;

                output.put(path, value);
            }
        }
    }

    /**
     * Gets the name of this configuration section. If this is the root section the name of the configuration file is
     * returned, if the configuration was loaded by a reader an empty string is returned.
     *
     * @return The name of this section.
     */
    public @NotNull String getName()
    {
        return name;
    }

    /**
     * The path of this configuration section. If this is the root section the path of the configuration file is
     * returned, if the configuration was loaded by a reader an empty string is returned.
     *
     * @return The path to this section with its name included.
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
     * Gets the root Configuration containing this configuration section and all other siblings.
     *
     * @return The root Configuration.
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

        getAbsoluteNodes(this, output);

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
        return Collections.unmodifiableMap(nodes);
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
     * Sets a value in the specified path. If the path has a section the value is set on the last section. If the section
     * doesn't exist the section is automatically created. Set the value to null to delete that node or section.
     *
     * @param path  The path with sections separated by section separator char.
     * @param value The value to be assigned to this path or null to delete the path.
     * @throws UnsupportedOperationException If a section on the path has chars that doesn't match the regex '[A-Za-z0-9_ -]'.
     * @throws IllegalArgumentException      If the path has sections with empty chars.
     */
    public void set(@NotNull String path, @Nullable Object value)
    {
        // Start checking if path is valid and fixing the sections with unnecessary spaces.

        if (path == null) throw new NullPointerException();

        if (!allowedPathCharsRegex.matcher(path.replace(Character.toString(sectionSeparator), "")).matches())
            throw new UnsupportedOperationException("Unsupported chars in path.");

        String[] sections = sectionSeparatorPattern.split(path);
        StringBuilder builder = new StringBuilder();

        for (String key : sections) {
            String fixedNode = key.trim();

            if (fixedNode.equals(""))
                throw new IllegalArgumentException("Path can not have sections with no chars.");

            builder.append(fixedNode).append(".");
        }

        sections = sectionSeparatorPattern.split(builder.substring(0, builder.length() - 1));

        // Finding the belonging section and assigning the value.

        ConfigurationSection section = this;
        int i = 1;

        for (String key : sections) {
            Object result = section.nodes.get(key);

            for (String cacheKey : section.cache.keySet())
                if (cacheKey.startsWith(key))
                    section.cache.remove(cacheKey);

            if (i++ == sections.length) {
                if (value == null)
                    section.nodes.remove(key);
                else
                    section.nodes.put(key, value);
            } else {
                if (result == null) {
                    if (value == null) {
                        break;
                    } else {
                        String sectionPath;

                        if (section instanceof Configuration)
                            sectionPath = key;
                        else
                            sectionPath = section.getPath() + section.sectionSeparator + key;

                        ConfigurationSection configurationSection = new ConfigurationSection(key, sectionPath, section,
                                new LinkedHashMap<>(), section.sectionSeparator);

                        section.nodes.put(key, configurationSection);
                        section = configurationSection;
                    }
                } else if (result instanceof ConfigurationSection) {
                    section = (ConfigurationSection) result;
                } else {
                    throw new IllegalStateException("Can't create new section because a node with the name '" + key +
                            "' already exists.");
                }
            }
        }
    }

    private @Nullable Object get(@NotNull String path)
    {
        if (cache.containsKey(Objects.requireNonNull(path)))
            return cache.get(path);

        String[] sections = sectionSeparatorPattern.split(path);
        ConfigurationSection section = this;
        int i = 1;

        for (String key : sections) {
            Object result = section.nodes.get(key);

            // Returning the result if this is the last key.
            if (i++ == sections.length) {
                // Add result to cache to improve performance.
                if (result != null)
                    cache.put(path, result);

                return result;
            }

            if (result instanceof ConfigurationSection)
                section = (ConfigurationSection) result;
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

        if (value instanceof Boolean)
            result = Boolean.TRUE.equals(value);

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

        if (value instanceof Character)
            result = (Character) value;

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

        if (value instanceof Collection)
            result = (Collection<?>) value;
        else
            result = new ArrayList<>();

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

        if (value instanceof Collection)
            for (Object object : (Collection<?>) value)
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

        if (value instanceof ConfigurationSection)
            result = (ConfigurationSection) value;

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

        if (value instanceof Number)
            result = (Number) value;

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

        if (value instanceof CharSequence)
            result = value.toString();

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
}
