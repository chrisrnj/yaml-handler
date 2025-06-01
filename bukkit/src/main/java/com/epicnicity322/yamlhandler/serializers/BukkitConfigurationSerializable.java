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

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class BukkitConfigurationSerializable implements CustomSerializer<ConfigurationSerializable>
{
    private static final @NotNull BukkitConfigurationSerializable instance = new BukkitConfigurationSerializable();

    private BukkitConfigurationSerializable()
    {
    }

    public static @NotNull BukkitConfigurationSerializable get()
    {
        return instance;
    }

    @Override
    public @NotNull Class<ConfigurationSerializable> type()
    {
        return ConfigurationSerializable.class;
    }

    @Override
    public boolean usesConfigurationSectionNodes()
    {
        return false;
    }

    @Override
    public @NotNull Map<String, Object> serialize(@NotNull ConfigurationSerializable obj)
    {
        return obj.serialize();
    }

    @Override
    public boolean isDeserializable(@NotNull Map<String, Object> nodes)
    {
        Object classPath = nodes.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY);
        if (classPath == null) return false;
        return ConfigurationSerialization.getClassByAlias(classPath.toString()) != null;
    }

    @Override
    public @NotNull ConfigurationSerializable deserialize(@NotNull Map<String, Object> nodes)
    {
        return Objects.requireNonNull(ConfigurationSerialization.deserializeObject(nodes), "Could not deserialize from map.");
    }
}
