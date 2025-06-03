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

package com.epicnicity322.yamlhandler;

import com.epicnicity322.yamlhandler.serializers.CustomSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class YamlHandlerUtil
{
    static void getAbsoluteNodes(ConfigurationSection input, Map<String, Object> output)
    {
        Map<String, Object> nodes = input.getNodes();

        if (nodes.isEmpty()) output.put(input.getPath(), input);
        else {
            for (Map.Entry<String, Object> node : nodes.entrySet()) {
                String key = node.getKey();
                Object object = node.getValue();

                if (object instanceof ConfigurationSection) {
                    getAbsoluteNodes((ConfigurationSection) object, output);
                } else {
                    String keyPath = input instanceof Configuration ? key : input.getPath() + input.getSectionSeparator() + key;
                    output.put(keyPath, object);
                }
            }
        }
    }

    static void convertToConfigurationSectionNodes(ConfigurationLoader loader, ConfigurationSection holder, Map<?, ?> nodes, Map<String, Object> output)
    {
        String separator = Character.toString(holder.getSectionSeparator());

        for (Map.Entry<?, ?> node : nodes.entrySet()) {
            Object[] newNode = createMapsFromSeparators(separator, node.getKey().toString(), node.getValue());
            String key = newNode[0].toString();
            Object value = newNode[1];

            if (value == ConfigurationSection.NULL_VALUE) value = null;
            else {
                if (value instanceof ConfigurationSection) value = ((ConfigurationSection) value).getNodes();
                if (value instanceof Map) {
                    ConfigurationSection section = new ConfigurationSection(key, holder, (Map<?, ?>) value, loader);
                    Object deserialized = tryDeserialize(section, loader.getCustomSerializers());
                    value = deserialized != null ? deserialized : section;
                }
            }

            output.put(key, value);
        }
    }

    static Object[] createMapsFromSeparators(String separator, String key, Object value)
    {
        StringTokenizer tokens = new StringTokenizer(key, separator);
        int tokenCount = tokens.countTokens();
        if (tokenCount == 0) throw new IllegalArgumentException("The key '" + key + "' has no tokens!");
        if (tokenCount == 1) return new Object[]{tokens.nextToken(), value};

        Deque<String> parts = new ArrayDeque<>(tokenCount);
        while (tokens.hasMoreTokens()) parts.push(tokens.nextToken());

        key = parts.getLast();
        parts.removeLast();

        for (String part : parts) value = Collections.singletonMap(part, value);
        return new Object[]{key, value};
    }

    static @Nullable Object tryDeserialize(ConfigurationSection section, CustomSerializer<?>[] serializers)
    {
        Map<String, Object> nodes = section.getNodes();

        for (CustomSerializer<?> serializer : serializers) {
            if (serializer == null) continue;
            if (serializer.isDeserializable(nodes)) {
                return serializer.deserialize(serializer.usesConfigurationSectionNodes() ? nodes : convertToMapNodes(section, false));
            }
        }
        return null;
    }

    static Map<String, Object> convertToMapNodes(ConfigurationSection section, boolean serialize)
    {
        Map<String, Object> sectionNodes = section.getNodes();
        LinkedHashMap<String, Object> mapNodes = new LinkedHashMap<>((int) Math.ceil(sectionNodes.size() / .75f));

        for (Map.Entry<String, Object> entry : sectionNodes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                value = convertToMapNodes((ConfigurationSection) value, serialize);
            } else if (serialize) {
                Map<String, Object> serialized = trySerialize(value, section);
                if (serialized != null) value = serialized;
            }

            mapNodes.put(key, value);
        }

        return mapNodes;
    }

    @SuppressWarnings("unchecked")
    static <T> @Nullable Map<String, Object> trySerialize(T object, ConfigurationSection section)
    {
        if (object == null) return null;
        ConfigurationLoader loader = section.getRoot().getLoader();
        CustomSerializer<T> customSerializer = (CustomSerializer<T>) loader.getCustomSerializer(object.getClass());
        if (customSerializer != null) return fullySerializeNodes(customSerializer.serialize(object), section);

        return null;
    }

    static Map<String, Object> fullySerializeNodes(Map<String, Object> nodes, ConfigurationSection section)
    {
        Map<String, Object> finalNodes = new LinkedHashMap<>(nodes);

        for (Map.Entry<String, Object> node : nodes.entrySet()) {
            String key = node.getKey();
            Object value = node.getValue();

            if (value instanceof ConfigurationSection) {
                finalNodes.put(key, convertToMapNodes((ConfigurationSection) value, true));
            } else {
                Map<String, Object> serialized = trySerialize(value, section);
                if (serialized != null) finalNodes.put(key, serialized);
            }
        }

        return finalNodes;
    }
}
