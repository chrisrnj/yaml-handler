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

import java.util.LinkedHashMap;
import java.util.Map;

class YamlHandlerUtil
{
    static void convertToConfigurationSectionNodes(char sectionSeparator, ConfigurationSection input, Map<?, ?> nodes, Map<String, Object> output)
    {
        for (Map.Entry<?, ?> entry : nodes.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                String path;

                if (input instanceof Configuration)
                    path = key;
                else
                    path = input.getPath() + sectionSeparator + key;

                value = new ConfigurationSection(key, path, input, (Map<?, ?>) value, sectionSeparator);
            }

            output.put(key, value);
        }
    }

    static void getAbsoluteNodes(char sectionSeparator, ConfigurationSection input, Map<String, Object> output)
    {
        for (Map.Entry<String, Object> entry : input.getNodes().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                getAbsoluteNodes(sectionSeparator, (ConfigurationSection) value, output);
            } else {
                String path;

                if (input instanceof Configuration)
                    path = key;
                else
                    path = input.getPath() + sectionSeparator + key;

                output.put(path, value);
            }
        }
    }

    static void convertToMapNodes(ConfigurationSection input, Map<String, Object> output)
    {
        for (Map.Entry<String, Object> entry : input.getNodes().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();

                convertToMapNodes((ConfigurationSection) value, map);
                value = map;
            }

            output.put(key, value);
        }
    }
}
