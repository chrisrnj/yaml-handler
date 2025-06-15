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

import com.epicnicity322.yamlhandler.serializers.CustomSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.nio.file.Path;

public interface ConfigurationLoader
{
    @NotNull Configuration load(@NotNull Reader reader) throws Exception;

    @NotNull Configuration load(@NotNull Path path) throws Exception;

    @NotNull Configuration load(@NotNull String contents) throws Exception;

    @NotNull String dump(@NotNull Configuration configuration);

    @NotNull CustomSerializer<?>[] getCustomSerializers();

    /**
     * Returns the {@link CustomSerializer} that has been registered for the given type, or {@code null} if no
     * serializer has been registered.
     * <p>
     * Custom serializers enable the {@code ConfigurationLoader} to marshal and unmarshal complex objects that
     * configurations cannot represent natively. Registration is typically performed through the loader’s builder or a
     * configuration module; consult your loader implementation for details.
     *
     * <h2>Type Compatibility</h2>
     * The returned serializer is guaranteed to be compatible with the provided {@code type}. Specifically, the
     * serializer’s {@link CustomSerializer#type()} will satisfy {@code serializer.type().isAssignableFrom(type)}.
     *
     * <h2>Usage example</h2>
     * <pre>{@code
     * ConfigurationLoader loader = ...;
     *
     * CustomSerializer<MyPojo> serializer =
     *         loader.getCustomSerializer(MyPojo.class);
     *
     * if (serializer != null) {
     *     Map<String, Object> data = serializer.serialize(myPojo);
     *     // persist data in a ConfigurationSection ...
     * }
     * }</pre>
     *
     * <h2>Thread safety</h2>
     * Implementations are expected to return stateless, reusable serializer instances.
     * If a serializer maintains internal state, it must guarantee its own thread safety.
     *
     * @param <T>  the type handled by the serializer
     * @param type the class literal representing {@code T}; must not be {@code null}
     * @return the registered serializer for {@code type}, or {@code null} if none is present
     * @since 1.5
     */
    default <T> @Nullable CustomSerializer<T> getCustomSerializer(@NotNull Class<T> type)
    {
        for (CustomSerializer<?> customSerializer : getCustomSerializers()) {
            if (customSerializer != null && customSerializer.type().isAssignableFrom(type))
                //noinspection unchecked
                return (CustomSerializer<T>) customSerializer;
        }
        return null;
    }

    /**
     * The character used for separating nodes on a {@link Configuration}.
     *
     * @return the section separator char
     * @since 1.5
     */
    char getSectionSeparator();
}
