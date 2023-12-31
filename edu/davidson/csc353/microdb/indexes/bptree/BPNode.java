package edu.davidson.csc353.microdb.indexes.bptree;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Function;

import static edu.davidson.csc353.microdb.indexes.bptree.BPNodeFactory.DISK_SIZE;

// Katherine Hu, Anh Hoang

/**
 * B+Tree node (internal or leaf).
 *
 * @param <K> Type of the keys in the B+Tree node.
 * @param <V> type of the values associated with the keys in the B+Tree node.
 */
public class BPNode<K extends Comparable<K>, V> {
    // Maximum number of children in an internal node (fanout)
    // A leaf node can have at most SIZE - 1 key-value pairs
    public static int SIZE = 5;
    // Reference to the parent
    public int parent;
    // Keys:
    // -  in the leaf node, they are the associated with values
    // -  in internal nodes, they separate the pointers to the children
    public ArrayList<K> keys;
    // (Leaf node only) Values associated with each key
    public ArrayList<V> values;
    // (Leaf node only) Next leaf node
    public int next;
    // (Internal node only) Children of the internal node
    public ArrayList<Integer> children;
    // Node number
    public int number;
    // Flag indicating if a leaf or not
    boolean leaf;

    /**
     * Constructor.
     *
     * @param leaf Flag indicating whether the node is a leaf (true) or internal node (false)
     */
    public BPNode(boolean leaf) {
        this.leaf = leaf;

        this.parent = -1;
        this.next = -1;

        // Contains one more than you allow, because you may want to add, and then split
        this.keys = new ArrayList<>(SIZE);

        // Contains one more than you allow, because you may want to add, and then split
        this.values = new ArrayList<>(SIZE);

        // Contains one more than you allow, because you may want to add, and then split
        this.children = new ArrayList<>(SIZE + 1);
    }

    /**
     * Returns true if the B+Tree node is a leaf node.
     *
     * @return True if the B+Tree node is a leaf node.
     */
    public boolean isLeaf() {
        return leaf;
    }

    /**
     * Returns the key in a given position.
     *
     * @param index Position of the key.
     * @return Key in the specified position.
     */
    public K getKey(int index) {
        return keys.get(index);
    }

    /**
     * Returns the child in a given position.
     * This can only be called for internal nodes.
     *
     * @param index Position of the child.
     * @return Child in the specified position.
     */
    public int getChild(int index) {
        return children.get(index);
    }

    /**
     * Returns the value in a given position.
     * This can only be called for leaf nodes.
     *
     * @param index Position of the value.
     * @return Value in the specified position.
     */
    public V getValue(int index) {
        return values.get(index);
    }

    /**
     * Returns the number of keys in this node.
     *
     * @return The number of keys.
     */
    public int getNumberOfKeys() {
        return keys.size();
    }

    /**
     * (Leaf node only) Inserts a (K,P) pair in the appropriate position in the node.
     *
     * @param key   The key.
     * @param value The value associated with the key.
     */
    public void insertValue(K key, V value) {
        int i;

        for (i = 0; i < keys.size(); i++) {
            if (BPTree.more(keys.get(i), key)) {
                break;
            }
        }

        keys.add(i, key);
        values.add(i, value);
    }

    /**
     * (Internal node only) Inserts a (K,P) pair in the appropriate position in the node.
     * <p>
     * NOTE: The pointer associated with key at position i is actually located at position i+1
     * due to the first pointer.
     *
     * @param key         The key.
     * @param childNumber The child node reference that should FOLLOW the key.
     * @param nodeFactory Factory that generates new nodes.
     */
    public void insertChild(K key, int childNumber, BPNodeFactory<K, V> nodeFactory) {
        int i;

        for (i = 0; i < keys.size(); i++) {
            if (BPTree.more(keys.get(i), key)) {
                break;
            }
        }

        keys.add(i, key);
        children.add(i + 1, childNumber);

        BPNode<K, V> child = nodeFactory.getNode(childNumber);

        child.parent = this.number;
        nodeFactory.save(child);
    }

    /**
     * Splits an overflowed leaf node into a {@link SplitResult} object,
     * that contains:
     * - A reference to the left B+Tree node;
     * - A reference to the right B+Tree node;
     * - The first key in the right B+Tree node (the "divider Key" in the {@link SplitResult} object).
     * <p>
     * The method also makes the left node point to the right node in the "next" attribute.
     *
     * @param nodeFactory Factory that generates new nodes.
     * @return A new {@link SplitResult} following the specifications above.
     */
    public SplitResult<K, V> splitLeaf(BPNodeFactory<K, V> nodeFactory) {
        SplitResult<K, V> result = new SplitResult<>();

        result.left = this;
        result.right = nodeFactory.create(true);

        int mid = keys.size() / 2;

        // Transfer the second half of the keys/values to the right node
        for (int i = mid; i < keys.size(); i++) {
            result.right.keys.add(keys.get(i));
            result.right.values.add(values.get(i));
        }

        // Remove the transferred keys/values from the left node
        // Do this in reverse order to avoid index issues
        for (int i = keys.size() - 1; i >= mid; i--) {
            keys.remove(i);
            values.remove(i);
        }

        // Set next of left to right
        result.left.next = result.right.number;

        // Set the divider key
        result.dividerKey = result.right.getKey(0);

        return result;
    }


    /**
     * Splits an overflowed leaf internal into a {@link SplitResult} object,
     * that contains:
     * - A reference to the left B+Tree node;
     * - A reference to the right B+Tree node;
     * - The divider key that will be subsequently be inserted in the parent node
     * (stored in the "divider Key" in the {@link SplitResult} object).
     *
     * @param nodeFactory Factory that generates new nodes.
     * @return A new {@link SplitResult} following the specifications above.
     */
    public SplitResult<K, V> splitInternal(BPNodeFactory<K, V> nodeFactory) {
        SplitResult<K, V> result = new SplitResult<>();

        result.left = this;
        result.right = nodeFactory.create(false);

        int mid = keys.size() / 2;
        result.dividerKey = keys.get(mid);

        // Transfer the keys and children to the right of the median key to the new right node
        for (int i = mid + 1; i < keys.size(); i++) {
            result.right.keys.add(keys.get(i));
        }
        for (int i = mid + 1; i < children.size(); i++) {
            result.right.children.add(children.get(i));
        }

        for (int i = keys.size() - 1; i >= mid; i--) {
            keys.remove(i);
        }

        for (int i = children.size() - 1; i >= mid + 1; i--) {
            children.remove(i);
        }


        return result;
    }


    /**
     * Returns a string representation of the B+Tree node.
     */
    public String toString() {
        String result = "[(#: " + number + ") ";

        // If leaf, print [k1 v1 k2 v2 ... kn vn]
        if (isLeaf()) {
            for (int i = 0; i < keys.size() - 1; i++) {
                result += keys.get(i);
                result += " ";
                result += values.get(i);
                result += " ";
            }

            if (keys.size() > 0) {
                result += keys.get(keys.size() - 1);
                result += " ";
                result += values.get(keys.size() - 1);
            }
        }
        // If internal, print [<c0> k1 <c1> k2 <c2> ... kn <cn>], where
        else {
            for (int i = 0; i < keys.size(); i++) {
                result += "<" + children.get(i) + ">";
                result += " ";
                result += keys.get(i);
                result += " ";
            }

            if (keys.size() > 0) {
                result += "<" + children.get(keys.size()) + ">";
            }
        }

        result += "]";

        return result;
    }

    /**
     * Loads information from a buffer of 512 bytes (loaded from disk)
     * into the memory representation of a B+Tree node.
     *
     * @param buffer    A byte buffer of 512 bytes containing the node representation from disk.
     * @param loadKey   A function that converts string representations into keys of type K
     * @param loadValue A function that converts string representations into values of type V
     */
    public void load(ByteBuffer buffer, Function<String, K> loadKey, Function<String, V> loadValue) {
        // TODO: Load from disk (that is, from the buffer), create your own file format
        //       The getInt() and putInt() functions should be very helpful
        buffer.rewind();

        leaf = buffer.getInt() == 1;
        parent = buffer.getInt();
        next = buffer.getInt();
        number = buffer.getInt();

        int numKeys = buffer.getInt();

        keys.clear();
        for (int i = 0; i < numKeys; i++) {
            int keyLength = buffer.getInt();
            byte[] keyBytes = new byte[keyLength];
            buffer.get(keyBytes);
            keys.add(loadKey.apply(new String(keyBytes)));
        }

        if (leaf) {
            values.clear();
            for (int i = 0; i < numKeys; i++) {
                int valueLength = buffer.getInt();
                byte[] valueBytes = new byte[valueLength];
                buffer.get(valueBytes);
                values.add(loadValue.apply(new String(valueBytes)));
            }
        } else {
            children.clear();
            for (int i = 0; i < numKeys + 1; i++) {
                children.add(buffer.getInt());
            }
        }
    }


    /**
     * Save the memory representation of the B+Tree node
     * into a buffer of 512 bytes (to be stored on disk).
     *
     * @param buffer
     */
    public void save(ByteBuffer buffer, BPNodeFactory<K, V> nodeFactory) {
        int totalSize = 0;
        buffer.clear();
        // Write leaf flag
        buffer.putInt(leaf ? 1 : 0);
        totalSize += Integer.BYTES;

        // Write parent
        buffer.putInt(parent);
        totalSize += Integer.BYTES;

        // Write next
        buffer.putInt(next);
        totalSize += Integer.BYTES;

        // Write number
        buffer.putInt(number);
        totalSize += Integer.BYTES;

        // Write number of keys
        buffer.putInt(keys.size());
        totalSize += Integer.BYTES;

        // Save the keys
        for (K key : keys) {
            String keyStr = key.toString();
            byte[] keyBytes = keyStr.getBytes();
            buffer.putInt(keyBytes.length);
            buffer.put(keyBytes);
            int keySize = Integer.BYTES + keyBytes.length;
            totalSize += keySize;
        }

        if (leaf) {
            // For leaf nodes, save the values
            for (V value : values) {
                String valueStr = value.toString();
                byte[] valueBytes = valueStr.getBytes();
                buffer.putInt(valueBytes.length);
                buffer.put(valueBytes);
                int valueSize = Integer.BYTES + valueBytes.length;
                totalSize += valueSize;
            }
        } else {
            // For internal nodes, save the children's node numbers
            for (Integer childNumber : children) {
                buffer.putInt(childNumber);
                totalSize += Integer.BYTES;
            }
        }

        if (totalSize > DISK_SIZE) {
            throw new IllegalStateException("Data size exceeds buffer capacity before padding.");
        }

        // Padding the buffer
        while (totalSize < DISK_SIZE) {
            buffer.put((byte) 0);
            totalSize++;
        }
    }
}



