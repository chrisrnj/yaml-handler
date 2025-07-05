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

package com.epicnicity322.yamlhandler.loaders;

import com.epicnicity322.yamlhandler.Comment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class YamlUtil
{
    private YamlUtil()
    {
    }

    /**
     * Recursively walks through the parent node, grabbing the comments.
     *
     * @param topNode          the node to get the comments from
     * @param sectionSeparator the character used for separating the path of child nodes
     * @return a map with all the comments on this node and inner nodes.
     */
    static @Nullable Map<String, Comment> retrieveComments(@NotNull Node topNode, char sectionSeparator)
    {
        Map<String, Comment> comments = new HashMap<>();
        retrieveComments(topNode, null, sectionSeparator, comments);
        return comments.isEmpty() ? null : comments;
    }

    /**
     * Recursively walks through a node setting the comments to the output map.
     * <p>
     * If the node is a {@link MappingNode}, its child nodes are added with a path separated by the specified section
     * separator char.
     *
     * @param node             the node to get the comments from
     * @param path             the current path of this node
     * @param sectionSeparator the character used for separating the path of child nodes
     * @param output           the map where the comments will be put on
     */
    private static void retrieveComments(Node node, @Nullable String path, char sectionSeparator, @NotNull Map<String, Comment> output)
    {
        if (node == null) return;

        if (path != null) {
            String inlineComment = collectComment(node.getInLineComments());
            if (inlineComment != null) {
                output.compute(path, (k, v) -> Comment.of(v == null ? null : v.blockComment(), inlineComment));
            }
        }

        if (node instanceof MappingNode) {
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
                ScalarNode keyNode = tuple.getKeyNode() instanceof ScalarNode ? (ScalarNode) tuple.getKeyNode() : null;
                if (keyNode == null) continue;

                String nodePath = path == null ? keyNode.getValue() : path + sectionSeparator + keyNode.getValue();

                String blockComment = collectComment(keyNode.getBlockComments());
                if (blockComment != null) output.put(nodePath, Comment.of(blockComment, null));

                retrieveComments(tuple.getValueNode(), nodePath, sectionSeparator, output);
            }
        }
    }

    /**
     * Recursively walks through the node, iterating through each {@link MappingNode} in order to find the path of the
     * comments, then sets the comment of the node as it's specified on the map.
     *
     * @param node             the node to recursively walk through, must be a {@link MappingNode}
     * @param path             the current path of the node
     * @param sectionSeparator the character used for separating sections and nodes on the comments map
     *                         //* @param blockOnly            whether to set only block comments for this node, false for only inline
     * @param comments         the map of comments with the key
     */
    static void assignComments(@NotNull Node node, @Nullable String path, char sectionSeparator, @NotNull Map<String, Comment> comments)
    {
        if (path != null && node instanceof ScalarNode) setComment(node, path, false, comments);

        if (node instanceof MappingNode) {
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
                ScalarNode keyNode = tuple.getKeyNode() instanceof ScalarNode ? (ScalarNode) tuple.getKeyNode() : null;
                if (keyNode == null) continue;

                String nodePath = path == null ? keyNode.getValue() : path + sectionSeparator + keyNode.getValue();

                setComment(keyNode, nodePath, true, comments);

                assignComments(tuple.getValueNode(), nodePath, sectionSeparator, comments);
            }
        }
    }

    private static void setComment(@NotNull Node node, @NotNull String path, boolean block, @NotNull Map<String, Comment> comments)
    {
        Comment comment = comments.getOrDefault(path, Comment.NULL_COMMENT);
        String value = block ? comment.blockComment() : comment.inlineComment();

        if (value == null) return;

        CommentType type = block ? CommentType.BLOCK : CommentType.IN_LINE;
        List<CommentLine> commentLines = Arrays.stream(value.split("\n")).map(line -> new CommentLine(node.getStartMark(), node.getEndMark(), line, type)).collect(Collectors.toList());

        if (block) node.setBlockComments(commentLines);
        else node.setInLineComments(commentLines);
    }

    private static @Nullable String collectComment(@Nullable List<CommentLine> comment)
    {
        return comment == null ? null : comment.stream().filter(line -> {
            CommentType type = line.getCommentType();
            return type == CommentType.IN_LINE || type == CommentType.BLOCK;
        }).map(CommentLine::getValue).collect(Collectors.joining("\n"));
    }
}
