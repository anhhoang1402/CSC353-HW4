/*
 * Author: Hammurabi Mendes
 * License: BSD-3-Clause
 * 
 * Implemented for CSC353 (Database Systems) at Davidson College.
 */
package edu.davidson.csc353.microdb.examples.indexes;

import edu.davidson.csc353.microdb.files.Block;
import edu.davidson.csc353.microdb.files.Relation;

import edu.davidson.csc353.microdb.indexes.*;
import edu.davidson.csc353.microdb.indexes.bptree.*;
import edu.davidson.csc353.microdb.examples.files.college.*;

import edu.davidson.csc353.microdb.files.Queriable;
import edu.davidson.csc353.microdb.files.Record;

// NOTE: WILL ONLY WORK AFTER YOUR BPTreeIndex.java is implemented correctly
public class TestBP {
	public static void main(String[] args) {
		Block.SIZE = 512;

		Queriable<Student> student = new Relation<>("student", () -> new Student());

		System.out.println("---------------");

		SecondaryIndex<Student, Integer> studentIndex1 = new SecondaryIndexMaterialized<Student, Integer>(student, (tuple) -> tuple.id, i -> i.toString(), s -> Integer.valueOf(s));
		SecondaryIndex<Student, Integer> studentIndex2 = new BPTreeIndex<Student, Integer>(student, (tuple) -> tuple.id, s -> Integer.valueOf(s));

		int[] queries = {36052, 66054, 108, 107, 99730, 100000};

		// Should print [36052,Guerra,Elec. Eng.,59]
		for(int i = 0; i < queries.length; i++) {
			for(RecordLocation location: studentIndex1.get(queries[i])) {
				if(location == null) {
					System.out.println("NULL result");
					break;
				}

				System.out.println("In-memory index: " + location);

				Record<Student> record = student.get(location.getBlockNumber(), location.getRecordNumber());

				System.out.println(record.getTuple());
				break; // prints only the first result
			}

			// TODO: Uncomment this to test the BPIndex
			/*
			for(RecordLocation location: studentIndex2.get(queries[i])) {
				if(location == null) {
					System.out.println("NULL result");
					break;
				}

				System.out.println("BPTree    index: " + location);

				Record<Student> record = student.get(location.getBlockNumber(), location.getRecordNumber());

				System.out.println(record.getTuple());
				break; // prints only the first result
			}
			*/
		}
	}
}
