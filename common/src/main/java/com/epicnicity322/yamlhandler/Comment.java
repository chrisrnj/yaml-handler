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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A record for storing comments internally in the configuration.
 *
 * @since 1.5
 */
public class Comment
{
    private final @NotNull String comment;
    private final boolean inline;

    public Comment(@NotNull String comment, boolean inline)
    {
        this.comment = comment;
        this.inline = inline;
    }

    public @NotNull String comment()
    {
        return comment;
    }

    public boolean inline()
    {
        return inline;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment1 = (Comment) o;
        return inline == comment1.inline && Objects.equals(comment, comment1.comment);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(comment, inline);
    }

    @Override
    public String toString()
    {
        return "Comment[" +
                "comment='" + comment + '\'' +
                ", inline=" + inline +
                ']';
    }
}
