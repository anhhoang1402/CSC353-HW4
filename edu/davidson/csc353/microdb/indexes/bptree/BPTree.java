package edu.davidson.csc353.microdb.indexes.bptree;

import java.nio.ByteBuffer;
import java.util.function.Function;

// Katherine Hu, Anh Hoang

/**
 * B+Tree implementation.
 *
 * @param <K> Type of the keys in the B+Tree node.
 * @param <V> type of the values associated with the keys in the B+Tree node.
 */
public class BPTree<K extends Comparable<K>, V> {
    private final BPNodeFactory<K, V> nodeFactory;

    private int rootNumber;

    public BPTree(Function<String, K> loadKey, Function<String, V> loadValue) {
        nodeFactory = new BPNodeFactory<>("test-index", loadKey, loadValue);

        // The node will be garbage-collected, but retrievable from disk
        BPNode<K, V> rootNode = nodeFactory.create(true);
        //nodeFactory.save(rootNode);

        rootNumber = rootNode.number;
    }

    /**
     * Helper method: returns true if k1 < k2.
     *
     * @param k1 The first key k1.
     * @param k2 The second key k2.
     * @return True iff k1 < k2.
     */
    public static <K extends Comparable<K>> boolean less(K k1, K k2) {
        return k1.compareTo(k2) < 0;
    }

    /**
     * Helper method: returns true if k1 == k2.
     *
     * @param k1 The first key k1.
     * @param k2 The second key k2.
     * @return True iff k1 == k2.
     */
    public static <K extends Comparable<K>> boolean equal(K k1, K k2) {
        return k1.compareTo(k2) == 0;
    }

    /**
     * Helper method: returns true if k1 > k2.
     *
     * @param k1 The first key k1.
     * @param k2 The second key k2.
     * @return True iff k1 > k2.
     */
    public static <K extends Comparable<K>> boolean more(K k1, K k2) {
        return k1.compareTo(k2) > 0;
    }

    /**
     * Main method. Creates a test index and add some key-value pairs.
     *
     * @param arguments None.
     */
    public static void main(String[] arguments) {


        BPTree<String, Integer> testIndex = new BPTree<>(k -> k, s -> Integer.parseInt(s));
        testIndex.insert("i", 9);
        testIndex.insert("l", 12);
        testIndex.insert("g", 7);
        testIndex.insert("o", 15);
        testIndex.insert("d", 4); // Generates leaf node split
        testIndex.insert("f", 6);
        testIndex.insert("a", 1);
        testIndex.insert("c", 3); // Generates leaf node split
        testIndex.insert("p", 16);
        testIndex.insert("k", 11); // Generates leaf node split
        testIndex.insert("b", 2);
        testIndex.insert("e", 5);
        testIndex.insert("n", 14);
        testIndex.insert("h", 8); // Generates leaf node split
        testIndex.insert("j", 10);
        testIndex.insert("m", 13); // Generates an internal node split

        System.out.println(testIndex.get("g")); // Should print 7
        System.out.println(testIndex.get("o")); // Should print 15
        System.out.println(testIndex.get("a")); // Should print 1
        System.out.println(testIndex.get("c")); // Should print 3
        System.out.println(testIndex.get("x")); // Should print null


        BPNode<String, Integer> node1 = testIndex.nodeFactory.getNode(testIndex.rootNumber);
        BPNode<String, Integer> node2 = testIndex.find(node1, "a");

        ByteBuffer buffer1 = ByteBuffer.allocate(512);
        ByteBuffer buffer2 = ByteBuffer.allocate(512);

        node1.save(buffer1, testIndex.nodeFactory);
        node2.save(buffer2, testIndex.nodeFactory);

        BPNode<String, Integer> newNode1 = new BPNode<>(false);
        BPNode<String, Integer> newNode2 = new BPNode<>(true);

        newNode1.load(buffer1, k -> k, s -> Integer.parseInt(s));
        newNode2.load(buffer2, k -> k, s -> Integer.parseInt(s));

        System.out.println("Original root: " + node1 + ", parent = " + node1.parent + ", next = " + node1.next + ", number = " + newNode1.number);
        System.out.println("Loaded root:   " + newNode1 + ", parent = " + newNode1.parent + ", next = " + newNode1.next + ", number = " + newNode1.number);

        System.out.println("Original leaf: " + node2 + ", parent = " + node2.parent + ", next = " + node2.next + ", number = " + newNode2.number);
        System.out.println("Loaded leaf:   " + newNode2 + ", parent = " + newNode2.parent + ", next = " + newNode2.next + ", number = " + newNode2.number);


        // Creating a simple leaf node
        BPNode<String, Integer> leafNode = new BPNode<>(true);
        leafNode.insertValue("apple", 1);
        leafNode.insertValue("banana", 2);
        leafNode.insertValue("cherry", 3);

        // Saving the leaf node
        ByteBuffer leafBuffer = ByteBuffer.allocate(512);
        leafNode.save(leafBuffer, testIndex.nodeFactory);

        // Loading into a new leaf node
        BPNode<String, Integer> newLeafNode = new BPNode<>(true);
        newLeafNode.load(leafBuffer, k -> k, s -> Integer.parseInt(s));

        // Testing if the new node has the same content
        System.out.println("Original Leaf Node: " + leafNode);
        System.out.println("Loaded Leaf Node: " + newLeafNode);

        // Creating a simple internal node
        BPNode<String, Integer> internalNode = new BPNode<>(false);
        internalNode.children.add(-1); // Using -1 or any placeholder value

        internalNode.insertChild("mango", 1, testIndex.nodeFactory);
        internalNode.insertChild("orange", 2, testIndex.nodeFactory);

        // Creating a node with parent and next references
        BPNode<String, Integer> parentNode = new BPNode<>(false);
        parentNode.number = 5; // Assigning a unique number

        BPNode<String, Integer> nextNode = new BPNode<>(true);
        nextNode.number = 6; // Assigning a unique number

        BPNode<String, Integer> nodeWithRefs = new BPNode<>(true);
        nodeWithRefs.parent = parentNode.number;
        nodeWithRefs.next = nextNode.number;
        nodeWithRefs.insertValue("pear", 4);

        // Saving the node with references
        ByteBuffer refBuffer = ByteBuffer.allocate(512);
        nodeWithRefs.save(refBuffer, testIndex.nodeFactory);

        // Loading into a new node
        BPNode<String, Integer> newNodeWithRefs = new BPNode<>(true);
        newNodeWithRefs.load(refBuffer, k -> k, s -> Integer.parseInt(s));

        // Testing if the new node has the same content and references
        System.out.println("Original Node with Refs: " + nodeWithRefs + ", Parent: " + nodeWithRefs.parent + ", Next: " + nodeWithRefs.next);
        System.out.println("Loaded Node with Refs: " + newNodeWithRefs + ", Parent: " + newNodeWithRefs.parent + ", Next: " + newNodeWithRefs.next);

    }

    /**
     * Inserts the key-value pair into the B+Tree.
     *
     * @param key   The key.
     * @param value The value associated with the key.
     */
    public void insert(K key, V value) {
        System.out.println("Inserting " + key);
        BPNode<K, V> insertPlace = find(nodeFactory.getNode(rootNumber), key);
        insertPlace.insertValue(key, value);
        // check for overflown
        if (insertPlace.getNumberOfKeys() >= BPNode.SIZE) {
            SplitResult<K, V> result = insertPlace.splitLeaf(nodeFactory);
            insertOnParent(result.left, result.dividerKey, result.right);
        }
    }

    /**
     * Insert on parent node a divider key after a child has been splitted.
     *
     * @param left  Left B+Tree node after a split has been made.
     * @param key   Divider key (according to the description in {@link SplitResult}.
     * @param right Right B+Tree node after a split has been made.
     */
    private void insertOnParent(BPNode<K, V> left, K key, BPNode<K, V> right) {
        BPNode<K, V> parent = nodeFactory.getNode(left.parent);

        //base case: if the parent is the root
        if (parent == null) {
            BPNode<K, V> newRoot = nodeFactory.create(false);
            newRoot.keys.add(key);
            newRoot.children.add(left.number);
            left.parent = newRoot.number;

            newRoot.children.add(right.number);
            right.parent = newRoot.number;

            rootNumber = newRoot.number;

            return;
        }

        //insert the divider key to the parent
        parent.insertChild(key, right.number, nodeFactory);

        if (parent.keys.size() >= BPNode.SIZE) {
            SplitResult<K, V> result = parent.splitInternal(nodeFactory);
            // Need to keep calling insertOnParent after performing an internal node split
            insertOnParent(result.left, result.dividerKey, result.right);
        }
    }

    /**
     * Returns a value associated with a particular key.
     *
     * @param key The key.
     * @return The value associated with the provided key.
     */
    public V get(K key) {
        BPNode<K, V> node = find(nodeFactory.getNode(rootNumber), key);
        for (int i = 0; i < node.getNumberOfKeys(); i++) {
            if (equal(node.getKey(i), key)) {
                return node.getValue(i);
            }
        }
        return null;
    }

    /**
     * Returns the leaf node where we should look for the provided key.
     *
     * @param node The current node in the recursive search procedure.
     * @param key  The key being searched.
     * @return The leaf node where we should look for the provided key.
     */
    private BPNode<K, V> find(BPNode<K, V> node, K key) {

        if (node.isLeaf()) {
            return node;
        }
        // Find the child to descend into
        for (int i = 0; i < node.getNumberOfKeys(); i++) {
            if (more(key, node.getKey(i))) {
                continue;
            } else if (equal(key, node.getKey(i))) {
                return find(nodeFactory.getNode(node.getChild(i + 1)), key);
            } else {
                return find(nodeFactory.getNode(node.getChild(i)), key);
            }
        }
        // If key is greater than all keys in the node, go to the last child
        return find(nodeFactory.getNode(node.getChild(node.getNumberOfKeys())), key);
    }

    private void printTreeStructure(BPNode<K, V> node, int level) {
        // Print the current node's details
        System.out.println("Level " + level + " Node " + node.number + ": " + node.keys);

        // If it's not a leaf, recursively print its children
        if (!node.isLeaf()) {
            for (int childNum : node.children) {
                BPNode<K, V> childNode = nodeFactory.getNode(childNum);
                printTreeStructure(childNode, level + 1);
            }
        }
    }
}
