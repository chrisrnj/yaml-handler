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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A record for storing comments internally in the configuration.
 *
 * @since 1.5
 */
public class Comment
{
    public static final @NotNull Comment NULL_COMMENT = new Comment(null, null);
    private final @Nullable String blockComment;
    private final @Nullable String inlineComment;

    private Comment(@Nullable String blockComment, @Nullable String inlineComment)
    {
        this.blockComment = blockComment;
        this.inlineComment = inlineComment;
    }

    public static @NotNull Comment of(@Nullable String blockComment, @Nullable String inlineComment)
    {
        if (blockComment != null && blockComment.isEmpty()) blockComment = null;
        if (inlineComment != null && inlineComment.isEmpty()) inlineComment = null;

        return blockComment == null && inlineComment == null ? NULL_COMMENT : new Comment(blockComment, inlineComment);
    }

    public @Nullable String blockComment()
    {
        return blockComment;
    }

    public @Nullable String inlineComment()
    {
        return inlineComment;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(blockComment, comment.blockComment) && Objects.equals(inlineComment, comment.inlineComment);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(blockComment, inlineComment);
    }

    @Override
    public String toString()
    {
        return "Comment[" +
                "block='" + blockComment + '\'' +
                ", inline='" + inlineComment + '\'' +
                ']';
    }
}
