/*
 * Copyright (c) 2025 Christiano Rangel
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

package com.epicnicity322.yamlhandler.serializers;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a strategy for serializing and deserializing objects of a specific type to and from a
 * configuration-compatible data structure.
 * <p>
 * Implementations of this interface allow complex or non-standard objects to be converted into a format that can be
 * persisted in a {@link com.epicnicity322.yamlhandler.ConfigurationSection}, and later reconstructed from that data.
 * <p>
 * Custom serializers are typically used when default serialization is insufficient or when fine-grained control over
 * the serialization process is required.
 *
 * @param <T> the type of object this serializer handles
 * @since 1.5
 */
public interface CustomSerializer<T>
{
    /**
     * Returns the class token representing the type {@code T} this serializer handles.
     * <p>
     * This is used internally to associate serializers with specific types and
     * should accurately reflect the target type.
     *
     * @return the non-null {@code Class} instance for {@code T}
     * @since 1.5
     */
    @NotNull Class<T> type();

    /**
     * Returns {@code true} if this serializer is compatible with
     * {@link com.epicnicity322.yamlhandler.ConfigurationSection} values on the serialized {@link Map}.
     * <p>
     * This should be {@code false} if the serializer uses plain maps on its deserialization and serialization process.
     *
     * @return whether the serializer will use {@link com.epicnicity322.yamlhandler.ConfigurationSection} on the
     * {@link Map} object of deserialization/serialization
     * @since 1.5
     */
    boolean usesConfigurationSectionNodes();

    /**
     * Converts an object of type {@code T} into a map representation that can be stored in YAML.
     *
     * @param obj the object to serialize; must not be {@code null}
     * @return a non-null map containing key-value pairs representing the object's state
     * @throws IllegalArgumentException if the object cannot be serialized
     * @since 1.5
     */
    @NotNull Map<String, Object> serialize(@NotNull T obj);

    /**
     * Whether the nodes can be deserialized with this custom serializer.
     * <p>
     * The {@code nodes} map will never have inner maps, instead it will have
     * {@link com.epicnicity322.yamlhandler.ConfigurationSection}, despite the value of
     * {@link #usesConfigurationSectionNodes()}.
     *
     * @param nodes a non-null map of keys and values that might or might not be the object's serialized state
     * @return if the map can be deserialized as an object of this custom serializer
     * @since 1.5
     */
    boolean isDeserializable(@NotNull Map<String, Object> nodes);

    /**
     * Reconstructs an object of type {@code T} from a previously serialized map.
     * <p>
     * The input map is expected to conform to the structure produced by {@link #serialize(Object)}.
     * Implementations should validate the map and handle missing or malformed data appropriately.
     *
     * @param nodes a non-null map of keys and values representing the object's serialized state
     * @return a fully reconstructed instance of {@code T}
     * @throws IllegalArgumentException if deserialization fails due to invalid or incomplete data
     * @since 1.5
     */
    @NotNull T deserialize(@NotNull Map<String, Object> nodes);
}
