package org.ihtsdo.otf.mapping.jpa.services;

import static org.junit.Assert.fail;

import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompareMapRecordJpaTest {

	private static DefaultProjectSpecificAlgorithmHandler handler;

	
	@BeforeClass
	public static void init() {
		handler = new DefaultProjectSpecificAlgorithmHandler();
	  
	}
	
	@Test
	public void testSingleGroupSingleEntryMatching() throws Exception {
		
		MapRecord record1 = new MapRecordJpa();
		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setAbbreviation("NC");
		entry1.setMapRelation(relation1);
		record1.addMapEntry(entry1);
		
		MapRecord record2 = new MapRecordJpa();
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T1");
		entry2.setRule("TRUE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setAbbreviation("NC");
		entry2.setMapRelation(relation2);
		record2.addMapEntry(entry2);
		
		if (handler.compareMapRecords(record1, record2).isValid() == false) {
			fail("testSingleGroupSingleEntryMatching failed!");
		};
	}
	
	@Test
	public void testSingleGroupSingleEntryDifferentRules() throws Exception {
		
		MapRecord record1 = new MapRecordJpa();
		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setAbbreviation("NC");
		entry1.setMapRelation(relation1);
		record1.addMapEntry(entry1);
		
		MapRecord record2 = new MapRecordJpa();
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T1");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setAbbreviation("NC");
		entry2.setMapRelation(relation2);
		record2.addMapEntry(entry2);
		
		if (handler.compareMapRecords(record1, record2).isValid() == true) {
			fail("testSingleGroupSingleEntryDifferentRules failed!");
		};
	}
	
	@Test
	public void testSingleGroupSingleEntryDifferentRelationId() throws Exception {
		
		MapRecord record1 = new MapRecordJpa();
		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		record1.addMapEntry(entry1);
		
		MapRecord record2 = new MapRecordJpa();
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T1");
		entry2.setRule("TRUE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(2L);
		entry2.setMapRelation(relation2);
		record2.addMapEntry(entry2);
		
		if (handler.compareMapRecords(record1, record2).isValid() == true) {
			fail("testSingleGroupSingleEntryDifferentRelationId failed!");
		};
	}
	
	
	@Test
	public void testSingleGroupSingleEntryDifferentAdviceLists() throws Exception {
		
		MapRecord record1 = new MapRecordJpa();
		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
		record1.addMapEntry(entry1);
		
		MapRecord record2 = new MapRecordJpa();
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T1");
		entry2.setRule("TRUE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);
		record2.addMapEntry(entry2);
		
		if (handler.compareMapRecords(record1, record2).isValid() == true) {
			fail("testSingleGroupSingleEntryDifferentAdviceLists failed!");
		};
	}
		
	@Test
	public void testSingleGroupMultipleEntriesSameOrder() throws Exception {
		

		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
			
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T2");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);

		MapRecord record1 = new MapRecordJpa();
		record1.addMapEntry(entry1);
		record1.addMapEntry(entry2);
		
		MapRecord record2 = new MapRecordJpa();
		record2.addMapEntry(entry1);
		record2.addMapEntry(entry2);
		
		if (handler.compareMapRecords(record1, record2).isValid() == false) {
			fail("testSingleGroupMultipleEntriesSameOrder failed!");
		};
	}
	
	@Test
	public void testSingleGroupMultipleEntriesDifferentOrder() throws Exception {
		

		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
			
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T2");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);

		MapRecord record1 = new MapRecordJpa();
		record1.addMapEntry(entry1);
		record1.addMapEntry(entry2);
		
		MapRecord record2 = new MapRecordJpa();
		record2.addMapEntry(entry2);
		record2.addMapEntry(entry1);
		
		if (handler.compareMapRecords(record1, record2).isValid() == false) {
			fail("testSingleGroupMultipleEntriesDifferentOrder failed!");
		};
	}
	
	@Test
	public void testMultipleGroupsEntriesMatching() throws Exception {
		

		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
			
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T2");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);
		
		MapEntry entry3 = new MapEntryJpa();
		entry3.setMapGroup(2);
		entry3.setTargetId("T3");
		entry3.setRule("FALSE");
		entry3.setMapRelation(relation2);
		entry3.addMapAdvice(advice1);
		entry3.addMapAdvice(advice3);

		MapRecord record1 = new MapRecordJpa();
		record1.addMapEntry(entry1);
		record1.addMapEntry(entry2);
		record1.addMapEntry(entry3);
		
		MapRecord record2 = new MapRecordJpa();
		record2.addMapEntry(entry1);
		record2.addMapEntry(entry2);
		record2.addMapEntry(entry3);
		
		if (handler.compareMapRecords(record1, record2).isValid() == false) {
			fail("testMultipleGroupsEntriesMatching failed!");
		};
	}
	
	@Test
	public void testMultipleGroupsFirstGroupMatching() throws Exception {
		

		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
			
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T2");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);
		
		MapEntry entry3 = new MapEntryJpa();
		entry3.setMapGroup(2);
		entry3.setTargetId("T3");
		entry3.setRule("FALSE");
		entry3.setMapRelation(relation2);
		entry3.addMapAdvice(advice1);
		entry3.addMapAdvice(advice3);
		
		MapEntry entry4 = new MapEntryJpa();
		entry4.setMapGroup(2);
		entry4.setTargetId("T4");
		entry4.setRule("TRUE");
		entry4.setMapRelation(relation2);
		entry4.addMapAdvice(advice1);
		entry4.addMapAdvice(advice3);

		MapRecord record1 = new MapRecordJpa();
		record1.addMapEntry(entry1);
		record1.addMapEntry(entry2);
		record1.addMapEntry(entry3);
		
		MapRecord record2 = new MapRecordJpa();
		record2.addMapEntry(entry1);
		record2.addMapEntry(entry2);
		record2.addMapEntry(entry4);
		
		if (handler.compareMapRecords(record1, record2).isValid() == true) {
			fail("testMultipleGroupsFirstGroupMatching failed!");
		};
	}
	
	@Test
	public void testMultipleGroupsDifferentNumbersOfGroups() throws Exception {
		

		MapEntry entry1 = new MapEntryJpa();
		entry1.setMapGroup(1);
		entry1.setTargetId("T1");
		entry1.setRule("TRUE");
		MapRelation relation1 = new MapRelationJpa();
		relation1.setId(1L);
		entry1.setMapRelation(relation1);
		MapAdvice advice1 = new MapAdviceJpa();
		advice1.setId(1L);
		advice1.setDetail("detail1");
		advice1.setName("name1");
		MapAdvice advice2 = new MapAdviceJpa();
		advice2.setId(2L);
		advice2.setDetail("detail2");
		advice2.setName("name2");
		entry1.addMapAdvice(advice1);
		entry1.addMapAdvice(advice2);
			
		MapEntry entry2 = new MapEntryJpa();
		entry2.setMapGroup(1);
		entry2.setTargetId("T2");
		entry2.setRule("FALSE");
		MapRelation relation2 = new MapRelationJpa();
		relation2.setId(1L);
		entry2.setMapRelation(relation2);
		MapAdvice advice3 = new MapAdviceJpa();
		advice3.setId(3L);
		advice3.setDetail("detail3");
		advice3.setName("name3");
		entry2.addMapAdvice(advice1);
		entry2.addMapAdvice(advice3);
				
		MapEntry entry4 = new MapEntryJpa();
		entry4.setMapGroup(2);
		entry4.setTargetId("T4");
		entry4.setRule("TRUE");
		entry4.setMapRelation(relation2);
		entry4.addMapAdvice(advice1);
		entry4.addMapAdvice(advice3);

		MapRecord record1 = new MapRecordJpa();
		record1.addMapEntry(entry1);
		record1.addMapEntry(entry2);
		
		MapRecord record2 = new MapRecordJpa();
		record2.addMapEntry(entry1);
		record2.addMapEntry(entry2);
		record2.addMapEntry(entry4);
		
		if (handler.compareMapRecords(record1, record2).isValid() == true) {
			fail("testMultipleGroupsDifferentNumbersOfGroups failed!");
		};
	}

	
	@AfterClass
	public static void cleanup() {
		
	}
}
