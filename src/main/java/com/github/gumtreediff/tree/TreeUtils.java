/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import com.github.gumtreediff.utils.Pair;

import java.util.*;

public final class TreeUtils {
    private TreeUtils() {
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a pre-order.
     * @param tree a Tree.
     */
    public static List<ITree> preOrder(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        preOrder(tree, trees);
        return trees;
    }

    private static void preOrder(ITree tree, List<ITree> trees) {
        if (tree != null){
            trees.add(tree);
            if (!tree.isLeaf())
                for (ITree c: tree.getChildren())
                    preOrder(c, trees);
        }
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a breadth-first order.
     * @param tree a Tree.
     */
    public static List<ITree> breadthFirst(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        List<ITree> currents = new ArrayList<>();
        currents.add(tree);
        while (currents.size() > 0) {
            ITree c = currents.remove(0);
            trees.add(c);
            currents.addAll(c.getChildren());
        }
        return trees;
    }

    public static Iterator<ITree> breadthFirstIterator(final ITree tree) {
        return new Iterator<ITree>() {
            Deque<Iterator<ITree>> fifo = new ArrayDeque<>();

            {
                addLasts(new FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return !fifo.isEmpty();
            }

            @Override
            public ITree next() {
                while (!fifo.isEmpty()) {
                    Iterator<ITree> it = fifo.getFirst();
                    if (it.hasNext()) {
                        ITree item = it.next();
                        if (!it.hasNext())
                            fifo.removeFirst();
                        addLasts(item);
                        return item;
                    }
                }
                throw new NoSuchElementException();
            }

            private void addLasts(ITree item) {
                List<ITree> children = item.getChildren();
                if (!children.isEmpty())
                    fifo.addLast(children.iterator());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a post-order.
     * @param tree a Tree.
     */
    public static List<ITree> postOrder(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        postOrder(tree, trees);
        return trees;
    }

    private static void postOrder(ITree tree, List<ITree> trees) {
        if (tree != null){
            if (!tree.isLeaf())
                for (ITree c: tree.getChildren())
                    postOrder(c, trees);
        }
        trees.add(tree);
    }

    public static Iterator<ITree> postOrderIterator(final ITree tree) {
        return new Iterator<ITree>() {
            Deque<Pair<ITree, Iterator<ITree>>> stack = new ArrayDeque<>();
            {
                push(tree);
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public ITree next() {
                if (stack.isEmpty())
                    throw new NoSuchElementException();
                return selectNextChild(stack.peek().second);
            }

            ITree selectNextChild(Iterator<ITree> it) {
                if (!it.hasNext())
                    return stack.pop().first;
                ITree item = it.next();
                if (item.isLeaf())
                    return item;
                return selectNextChild(push(item));
            }

            private Iterator<ITree> push(ITree item) {
                Iterator<ITree> it = item.getChildren().iterator();
                stack.push(new Pair<>(item, it));
                return it;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<ITree> preOrderIterator(ITree tree) {
        return new Iterator<ITree>() {
            Deque<Iterator<ITree>> stack = new ArrayDeque<>();
            {
                push(new FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public ITree next() {
                Iterator<ITree> it = stack.peek();
                if (it == null)
                    throw new NoSuchElementException();
                ITree t = it.next();
                while (it != null && !it.hasNext()) {
                    stack.pop();
                    it = stack.peek();
                }
                push(t);
                return t;
            }

            private void push(ITree tree) {
                if (tree != null){
                    if (!tree.isLeaf())
                        stack.push(tree.getChildren().iterator());
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<ITree> leafIterator(final Iterator<ITree> it) {
        return new Iterator<ITree>() {
            ITree current = it.hasNext() ? it.next() : null;
            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public ITree next() {
                ITree val = current;
                while (it.hasNext()) {
                    current = it.next();
                    if (current.isLeaf())
                        break;
                }
                if (!it.hasNext()) {
                    current = null;
                }
                return val;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
