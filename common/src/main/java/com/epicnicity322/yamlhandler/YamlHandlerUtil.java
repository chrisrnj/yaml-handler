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

import java.util.LinkedHashMap;
import java.util.Map;

class YamlHandlerUtil
{
    static void convertToConfigurationSectionNodes(ConfigurationLoader loader, ConfigurationSection holder, Map<?, ?> nodes, Map<String, Object> output)
    {
        for (Map.Entry<?, ?> entry : nodes.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

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

    static Map<String, Object> convertToMapNodes(ConfigurationSection section)
    {
        LinkedHashMap<String, Object> mapNodes = new LinkedHashMap<>((int) Math.ceil(section.getNodes().size() / .75f));

        for (Map.Entry<String, Object> entry : section.getNodes().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) value = convertToMapNodes((ConfigurationSection) value);

            mapNodes.put(key, value);
        }

        return mapNodes;
    }

    static @Nullable Object tryDeserialize(ConfigurationSection section, CustomSerializer<?>[] serializers)
    {
        Map<String, Object> nodes = section.getNodes();

        for (CustomSerializer<?> serializer : serializers) {
            if (serializer == null) continue;
            if (serializer.isDeserializable(nodes)) {
                return serializer.deserialize(serializer.usesConfigurationSectionNodes() ? nodes : convertToMapNodes(section));
            }
        }
        return null;
    }
}
