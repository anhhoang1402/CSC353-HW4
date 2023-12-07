package edu.davidson.csc353.microdb.indexes.bptree;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.function.Function;

import edu.davidson.csc353.microdb.files.Block;
import edu.davidson.csc353.microdb.utils.DecentPQ;

public class BPNodeFactory<K extends Comparable<K>, V> {
	public static final int DISK_SIZE = 512;

	public static final int CAPACITY = 15;

	private String indexName;

	private Function<String, K> loadKey;
	private Function<String, V> loadValue;

	private int numberNodes;

	private RandomAccessFile relationFile;
	private FileChannel relationChannel;

	private DecentPQ<NodeTimestamp> nodePQ;

	private class NodeTimestamp implements Comparable<NodeTimestamp> {
		public BPNode<K,V> node;
		public long lastUsed;

		public NodeTimestamp(BPNode<K,V> node, long lastUsed) {
			this.node = node;
			this.lastUsed = lastUsed;
		}

		public int compareTo(NodeTimestamp other) {
			return (int) (lastUsed - other.lastUsed);
		}
	}

	// You should change the type of this nodeMap
	private HashMap<Integer, NodeTimestamp> nodeMap;

	/**
	 * Creates a new NodeFactory object, which will operate a buffer manager for
	 * the nodes of a B+Tree.
	 * 
	 * @param indexName The name of the index stored on disk.
	 */
	public BPNodeFactory(String indexName, Function<String, K> loadKey, Function<String, V> loadValue) {
		try {
			this.indexName = indexName;
			this.loadKey = loadKey;
			this.loadValue = loadValue;

			Files.delete(Paths.get(indexName + ".db"));

			relationFile = new RandomAccessFile(indexName + ".db", "rws");
			relationChannel = relationFile.getChannel();

			numberNodes = 0;

			nodeMap = new HashMap<>();
			nodePQ = new DecentPQ<>();
		}
		catch (FileNotFoundException exception) {
			// Ignore: a new file has been created
		}
		catch(IOException exception) {
			throw new RuntimeException("Error accessing " + indexName);
		}
	}

	/**
	 * Creates a B+Tree node.
	 * 
	 * @param leaf Flag indicating whether the node is a leaf (true) or internal node (false)
	 * 
	 * @return A new B+Tree node.
	 */
	public BPNode<K,V> create(boolean leaf) {
		BPNode<K,V> created = new BPNode<K,V>(leaf);
		created.number = numberNodes;
		NodeTimestamp newest = new NodeTimestamp(created,System.nanoTime());
		nodeMap.put(created.number, newest);
		numberNodes++;
		nodePQ.add(newest);
		return created;
	}

	/**
	 * Saves a node into disk.
	 * 
	 * @param node Node to be saved into disk.
	 */
	public void save(BPNode<K,V> node) {
		writeNode(node);
	}

	/**
	 * Reads a node from the disk.
	 * 
	 * @param nodeNumber Number of the node read.
	 * 
	 * @return Node read from the disk that has the provided number.
	 */
	private BPNode<K, V> readNode(int nodeNumber) {
		if (nodeNumber < 0) {
			throw new IllegalArgumentException("Node number cannot be negative. Given: " + nodeNumber);
		}

		try {
			ByteBuffer buffer = ByteBuffer.allocate(DISK_SIZE);
			long position = (long) nodeNumber * DISK_SIZE;
			relationChannel.position(position);
			relationChannel.read(buffer);
			buffer.flip();

			// Read leaf status
			boolean isLeaf = buffer.getInt() == 1;

			// Create the node with the correct leaf status
			BPNode<K, V> node = new BPNode<>(isLeaf);
			node.load(buffer, loadKey, loadValue);
			return node;
		} catch (IOException e) {
			throw new RuntimeException("Error reading node from disk", e);
		}
	}



	/**
	 * Writes a node into disk.
	 * 
	 * @param node Node to be saved into disk.
	 */
	private void writeNode(BPNode<K, V> node) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(DISK_SIZE);
			node.save(buffer, this);
			buffer.flip();

			long position = (long) node.number * DISK_SIZE;
			relationChannel.position(position);

			int bytesWritten = relationChannel.write(buffer);
			if (bytesWritten != DISK_SIZE) {
				throw new IOException("Incomplete write, expected " + DISK_SIZE + " bytes, but wrote " + bytesWritten);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error writing node to disk", e);
		}
	}


	/**
	 * Evicts the last recently used node back into disk.
	 */
	private void evict() {
		NodeTimestamp least = nodePQ.removeMin();
		Integer nodeNumber = least.node.number;
		nodeMap.remove(nodeNumber);

		writeNode(least.node);
	}

	/**
	 * Returns the node associated with a particular number.
	 * 
	 * @param number The number to be converted to node (loading it from disk, if necessary).
	 * 
	 * @return The node associated with the provided number.
	 */
	public BPNode<K,V> getNode(int number) {

		if (nodeMap.containsKey(number)) {
			NodeTimestamp nodeTime = nodeMap.get(number);
			nodeTime.lastUsed = System.nanoTime();
			return nodeTime.node;
		}

		BPNode<K,V> node = readNode(number);
		NodeTimestamp newest = new NodeTimestamp(node, System.nanoTime());
		nodeMap.put(node.number, newest);

		nodePQ.add(newest);
		return node;
	}
}
