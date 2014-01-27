package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Goal which updates the db to sync it with the model via JPA.
 * 
 * <pre>
 *   <plugin>
 *      <groupId>org.ihtsdo.otf.mapping</groupId>
 *      <artifactId>mapping-admin-mojo</artifactId>
 *      <version>${project.version}</version>
 *      <dependencies>
 *        <dependency>
 *          <groupId>org.ihtsdo.otf.mapping</groupId>
 *          <artifactId>mapping-admin-sampledata-config</artifactId>
 *          <version>${project.version}</version>
 *          <scope>system</scope>
 *          <systemPath>${project.build.directory}/mapping-admin-sampledata-${project.version}.jar</systemPath>
 *        </dependency>
 *      </dependencies>
 *      <executions>
 *        <execution>
 *          <id>sampledata</id>
 *          <phase>package</phase>
 *          <goals>
 *            <goal>sampledata</goal>
 *          </goals>
 *          <configuration>
 *            <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *          </configuration>
 *        </execution>
 *      </executions>
 *    </plugin>
 * </pre>
 * 
 * @goal sampledata
 * 
 * @phase process-resources
 */
public class SampledataMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * Instantiates a {@link SampledataMojo} from the specified parameters.
	 * 
	 */
	public SampledataMojo() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws MojoFailureException {

		EntityManagerFactory factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		

		try {
					
			getLog().info("Load Sample data");

			// ASSUMPTION: Database is unloaded, starting fresh

			List<MapProject> projects = new ArrayList<>();
			List<MapSpecialist> specialists = new ArrayList<>();
			List<MapLead> leads = new ArrayList<>();

			// Add Specialists and Leads
			MapLeadJpa mapLead = new MapLeadJpa();
			mapLead.setName("Kathy Giannangelo");
			mapLead.setUserName("kgi");
			mapLead.setEmail("kgi@ihtsdo.org");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Donna Morgan");
			mapLead.setUserName("dmo");
			mapLead.setEmail("dmo@ihtsdo.org");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Julie O'Halloran");
			mapLead.setUserName("joh");
			mapLead.setEmail("julie.ohalloran@sydney.edu.au");
			leads.add(mapLead);

			MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Krista Lilly");
			mapSpecialist.setUserName("kli");
			mapSpecialist.setEmail("kli@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Nicola Ingram");
			mapSpecialist.setUserName("nin");
			mapSpecialist.setEmail("nin@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Rory Davidson");
			mapSpecialist.setUserName("rda");
			mapSpecialist.setEmail("rda@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Julie O'Halloran");
			mapSpecialist.setUserName("joh");
			mapSpecialist.setEmail("julie.ohalloran@sydney.edu.au");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Graeme Miller");
			mapSpecialist.setUserName("gmi");
			mapSpecialist.setEmail("graeme.miller@sydney.edu.au");
			specialists.add(mapSpecialist);

			tx.begin();
			for (MapSpecialist m : specialists) {
				Logger.getLogger(this.getClass()).info(
						"Adding map specialist " + m.getName());
				manager.persist(m);
			}

			for (MapLead m : leads) {
				Logger.getLogger(this.getClass())
						.info("Adding map lead " + m.getName());
				manager.persist(m);
			}
			tx.commit();

			// Add map advices and principles
			List<MapAdvice> mapAdvices = new ArrayList<MapAdvice>();
			List<MapPrinciple> mapPrinciples = new ArrayList<MapPrinciple>();

			String[] adviceValues =
					new String[] {
							"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
							"Broad to narrow map from SNOMED CT source code to target code",
							"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
							"CONSIDER LATERALITY SPECIFICATION",
							"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
							"CONSIDER TIME OF COMA SCALE SPECIFICATION",
							"CONSIDER TOPHUS SPECIFICATION",
							"CONSIDER TRIMESTER SPECIFICATION",
							"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
							"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
							"EPISODE OF CARE INFORMATION NEEDED",
							"Exact match map from SNOMED CT source code to target code",
							"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
							"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
							"MAP IS CONTEXT DEPENDENT FOR GENDER",
							"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
							"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
							"MAPPED FOLLOWING IHTSDO GUIDANCE",
							"MAPPED FOLLOWING WHO GUIDANCE",
							"MAPPED WITH IHTSDO GUIDANCE",
							"MAPPED WITH NCHS GUIDANCE",
							"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
							"Narrow to broad map from SNOMED CT source code to target code",
							"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
							"Partial overlap between SNOMED CT source code and target code",
							"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
							"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
							"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
							"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
							"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
							"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
							"SNOMED CT source code not mappable to target coding scheme",
							"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
							"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
							"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
							"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
							"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
							"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
							"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)",
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
					};

			String[] icd10AdviceValues =
					new String[] {
							"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
							"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
							"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
							"MAP IS CONTEXT DEPENDENT FOR GENDER",
							"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
							"MAPPED FOLLOWING IHTSDO GUIDANCE",
							"MAPPED FOLLOWING WHO GUIDANCE",
							"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
							"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
							"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
							"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
							"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
							"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
							"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
							"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
							"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
							"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
							"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
							"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
							"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
					};
			String[] icd9cmAdviceValues =
					new String[] {
							"SNOMED CT source code not mappable to target coding scheme",
							"Exact match map from SNOMED CT source code to target code",
							"Narrow to broad map from SNOMED CT source code to target code",
							"Broad to narrow map from SNOMED CT source code to target code",
							"Partial overlap between SNOMED CT source code and target code"
					};

			String[] icpcAdviceValues =
					new String[] {
							"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
							"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
							"CONSIDER LATERALITY SPECIFICATION",
							"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
							"CONSIDER TIME OF COMA SCALE SPECIFICATION",
							"CONSIDER TOPHUS SPECIFICATION",
							"CONSIDER TRIMESTER SPECIFICATION",
							"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
							"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
							"EPISODE OF CARE INFORMATION NEEDED",
							"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
							"MAP IS CONTEXT DEPENDENT FOR GENDER",
							"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
							"MAPPED WITH IHTSDO GUIDANCE",
							"MAPPED WITH NCHS GUIDANCE",
							"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
							"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
							"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
							"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
							"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
							"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
							"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
							"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
							"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
							"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
					};
			
			String[] icd10PrincipleIds = new String[] {
					"01", "02", "03", "04", "05", "06", "07", "08", "09", "010",
					"011", "012", "013", "014", "015", "016", "017", "018", "019", "020",
			        "021", "022", "023", "024", "025", "026", "027", "028", "029", "030",
			        "031", "032", "033", "034" };
			                               
			
			String[] icd10PrincipleTitles = 
					new String[] {
				
					"Mapping of High Level Concepts",
					"Mapping of Low Level Concepts",
					"POSSIBLE REQUIREMENT TO IDENTIFY PLACE OF OCCURRENCE",
					"MAPPED FOLLOWING WHO GUIDANCE",
					"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
					
					"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (BURNS)",
					"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
					"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
					"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
					"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
					
					"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
					"MAP IS CONTEXT DEPENDENT FOR GENDER",
					"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
					"HOW TO MAP PARTIALLY DEFINED SNOMED CT CONCEPTS",
					"ACTIVITY CODE",
					
					"MAPPING CONTEXT: ACQUIRED VERSUS CONGENITAL",
					"JUDGMENTAL ASSIGNMENT OF THE TARGET ICD-10 CODE",
					"USE WHO GUIDANCE, NOT COUNTRY-SPECIFIC",
					"ALLERGIES AND SENSITIVITIES",
					"WHO GUIDANCE FOR MAPPING TO FOURTH CHARACTER ONLY AND  NOT FIFTH CHARACTER FOR SITE SPECIFIC TARGET ICD-10 CODES",
					
					"ADVICE REQUIRED FOR POISONING EXTERNAL CAUSE CODES",
					"MAPPING PERINATAL CONDITIONS, INCLUDING FETAL DEATH, DUE TO MATERNAL FACTORS",
					"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
					"MAPPING CONTEXT: AGE OF ONSET",
					"CURRENT PATIENT AGE",
					
					"MAPPED FOLLOWING IHTSDO GUIDANCE",
					"SEQUENCING OF POISONING CODES",
					"OPEN WOUNDS ACCOMPANYING OTHER INJURIES",
					"REPORTED HEAD INJURIES WITH LOSS OF CONSCIOUSNESS",
					"MULTIPLE FRACTURES",
					
					"MAPPING OF “AND/OR” CONCEPTS",
					"MAPPING OF ANIMAL BITES",
					"SYNDROMES",
					"AMBIGUITY IN SYNONYMS"
					
			};
			
			
			String[] icd10Principles = 
					new String[] {
					
					//01
					"Where a source concept has more than 10 descendants the mapping specialist will create a record for the appropriate default target code or metadata. It is not necessary to add the map advice DESCENDANTS NOT EXHAUSTIVELY MAPPED since this advice is added at the time batches are created to concepts having more than 10 descendants.",
					
					//02
					"Where a source concept has 10 or fewer descendants the mapping specialist will evaluate and map each descendant to a relevant target code or metadata ensuring assignment of map advice where appropriate.",
					
					//03
					"For codes W00-Y34 ICD-10 provides characters to identify the place of occurrence of the external causes, where relevant. These are added as a fourth character to Chapter XX codes to record where the incident happened, with the exceptions of Neglect and abandonment (Y06), Other maltreatment (Y07), and  Legal intervention and Operations of war (Y35, Y36), which already have  a fourth character:\n"
						+ "\n"
						+ "0 -Home\n"
						+ "1-	Residential institution\n"
						+ "2-	School, other institution and public administrative area\n"
						+ "3-	Sports and athletics area\n"
						+ "4-	Street and highway\n"
						+ "5-	Trade and service area\n"
						+ "6-	Industrial and construction area\n"
						+ "7-	Farm\n"
						+ "8-	Other specified places\n"
						+ "9-	Unspecified place\n"
						+ "\n"
						+ "Fourth character place of occurrence codes are not added to the Map but instead the Mapping Specialist selects Map POSSIBLE REQUIREMENT TO IDENTIFY PLACE OF OCCURRENCE."
						+ "\n"	
						+ "Note: The following codes also do not use the Place of Occurrence code, having their own fourth character to be used instead: Transport accidents (V01-V99).",
					
					//04
					"The map advice MAPPED FOLLOWING WHO GUIDANCE is utilized when assigning an ICD-10 target code based on conventions and assumptions in WHO guidance.",
					
					//05
					"All source concepts representing neoplastic disorders will be mapped. Morphology mapping with ICD-O is out of scope for the MAP. An advice note will be recorded by the Mapping Specialist to denote a morphology code may be required for completeness. ",
					
					//06
					"Codes in categories T31 Burns classified according to extent of body surface involved and T32 Corrosions classified according to extent of body surface involved capture information about the percentage of body surface that has been burned or corroded, but codes in these categories should only be used as the main condition if the specific site of the burn is unknown. However, they can be used as an additional code to add more detail to a diagnosis."
						+ "\n\nThe Mapping Specialist selects USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (BURNS) map advice when mapping SNOMED concepts to codes in these categories.",
					
					//07
					"An external cause code from Chapter XX is used with a code from another chapter, to add to the detail captured by the diagnosis code by giving the reason for the condition, especially in situations where the diagnosis code specifies to “use additional external cause code. The external cause code should always be sequenced AFTER the disease chapter code in a secondary position. An exception to this rule is when a SNOMED Concept is described as an event and maps to an external cause code. In this instance the map advice THIS IS AN EXTERNAL CAUSE CODE TO USE IN A SECONDARY POSITION should be added to identify the need to record a diagnosis code and sequencing rules apply.",
					
					//08
					"The map advice of POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE has been changed to POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE." 
						+ "\n"
						+ "Use this map advice when the SNOMED concept maps to an ICD-10 code that has the following Tabular instructions:\n"
						+ "-	Use additional code, if desired, to identify infectious agent or disease\n"
						+ "-	Use additional code (B95-B97), if desired, to identify infectious agent\n"
						+ "-	Use additional code (B95-B96), if desired, to identify bacterial agent\n",
					
					//09
					"(See also Principles 01 and 02) If a source concept has more than 10 descendants, the map specialist will construct a map with a default target only. This will be considered a high level concept. If a source concept has 10 or less descendants, the mapping specialist will construct maps for each of those descendants and this will be considered a low level concept. NC map category cases should not have any other map advice values except 'DESCENDANTS NOT EXHAUSTIVELY  MAPPED' or 'MAP IS CONTEXT DEPENDENT FOR GENDER'",
					
					//010
					"In certain circumstances, for a site-specific SNOMED concept, mapping can be completed to the 4th digit by the Mapping Specialist. To complete the code with a 5th digit requires additional clinical information.  In these cases, it is more useful to allow the user who has access to the clinical information to assign the 5th digit rather than the Mapping Specialist assigning either the .8 Other or .9 Site unspecified. See Example 1 and 2 below. However, this must not be confused with the more usual scenario where a concept that is general and does not specify the site maps to a target code in ICD-10 Chapter XIII (M that is not site-specific and requires a 5th character, and therefore it is correct to add the 5th digit .9 Site unspecified.     See Example 3.",
					
					//011
					"The map advice of THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION is to be added when the SNOMED concept maps to an ICD-10 code that would only be used in a secondary position. This circumstance is different from the current advice THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION.",
					
					//012
					"If the source concept does not assert gender yet only gender restricted target codes are found within ICD-10, the map will be considered context dependent. The Mapping Specialist selects the gender in the Map Rule to add each gender separately. It is not necessary to add the map advice MAP IS CONTEXT DEPENDENT FOR GENDER since this advice is added automatically during QA checks. NC map category cases should not have any other map advice values except “DESCENDANTS NOT EXHAUSTIVELY  MAPPED” or “MAP IS CONTEXT DEPENDENT FOR GENDER”",
					
					//013
					"The map advice of POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION is to be added as a map advice. Use this map advice when the SNOMED concept maps to an ICD-10 code that has the Tabular instruction to “Use additional code, if desired, to identify…..” Note: There is separate advice for requirement to add an external cause code (see Principle 0007) and requirement for causative agent (see Principle 0008).",
					
					//014
					"A SNOMED concept that is not fully defined by its SNOMED relationships, must be fully mapped to the definition in the Fully Specified Name and referred for re-defining to ensure the MAP is Understandable, Useful & Reproducible."
						+ "\n•Tick the check box in the Notes section of the Mapping Tool for Flag for Map Lead."
						+ "\n•In the Notes section, type the following standard phrase (the search will be on the words “Incomplete definition” – so please type carefully):"
						+ "\nIncomplete definition; modeling does not fully express its meaning."
						+ "\nDocument which part of the definition is missing."
						+ "\nFollowing the partial map principle the SNOMED CT concept ID 191802004 Acute Alcoholic Intoxication in Alcoholism, in the example below, is not fully defined as alcoholism and is only defined as alcohol intoxication but the Map Specialist must fully code to the meaning expressed in the Fully Specified Name and flag for Map Lead using the standard phrase, noting down which part of the definition is missing. Therefore the map is two ICD-10 codes following WHO ICD-10 guidance for coding acute-on-chronic conditions (F10.0 and F10.2).",

					//015
					"The Mapping Service Team will not map SNOMED CT concepts to the supplementary Activity code and will not be providing guidance related to or including the Activity code. Assignment of the Activity code is based on country –specific rules with each region either not adopting the code (for example, in the UK) or following local conventions.",
					
					//016
					"A source concept which identifies origination as a congenital or acquired condition will be mapped to a target of congenital or acquired classification should one exist. If a source concept is general (i.e. does not specify congenital or acquired) the ICD-10 index will be searched for guidance of a default Map member, either 'congenital' or 'acquired'. When a default is provided, this context will be employed to create one appropriate map adding map advice MAPPED FOLLOWING WHO GUIDANCE. When the source concept is general and no default is provided in the ICD-10 index, the map specialist will create map rules relevant for all appropriate targets of congenital and acquired or else “not classifiable” when context information is not available. This is a very rare scenario as ICD-10 almost always provides a default ICD-10 code in the alphabetical index.",	
					
					//017
					"If the clinical finding, event, or situation represented by the SNOMED CT concept cannot be located in the ICD-10 Index, Volume 2, then a judgmental assignment of a code(s) must be made. The mapping specialist uses experience, logic, reason, precedent and research to form an opinion allowing a decision to be reached about which is the most appropriate ICD-10 code to assign as the target map.",

					//018
					"Mapping is to be performed according to WHO guidelines, even if those guidelines differ from country-specific guidelines.  WHO guidelines have the ultimate authority in the outcome of codes.  ",
					
					//019
					"This is considered a propensity to rather than an acute allergic reaction. Map to personal history of allergy.",
					
					//020
					"In ICD-10 Chapter XIII (‘M’) 5th characters are provided to add specificity to the 'site' of a particular condition. The wording at the beginning of Chapter XIII states that they are to be used 'With appropriate categories'. ICD-10 categories such as M70.2 Olecranon bursitis and M88.0 Paget's disease of skull do not require the addition of a 5th digit site code as they are site specific at the 4th character code, i.e. the site information is already captured in the 4 character code. The map advice MAPPED FOLLOWING WHO GUIDANCE is unnecessary.",
					
					//021
					"The WHO says to assume that a poisoning is accidental when intent is not specified. Two Map advices are required on an external cause code in a second group for a poisoning assumed to be accidental:"
						+ "\nMAPPED FOLLOWING WHO GUIDANCE"
						+ "\nPOSSIBLE REQUIREMENT TO IDENTIFY PLACE OF OCCURRENCE",
						
					//022
					"When mapping a concept which describes a fetus or newborn that has been affected by a maternal condition, including when this condition results in the death of the fetus or newborn, two codes are required– one for the effect on the fetus or newborn and the other code from ICD-10 block P00–P04 to show the cause. In ICD-10 the block P00–P04 is for Fetus and newborn affected by maternal factors and by complications of pregnancy, labor and delivery and the conditions in this block are the mother’s conditions which are used to show that a problem the mother may have had prior to pregnancy or a problem the mother incurred during the pregnancy, labor or delivery had an effect on the fetus or newborn.",
					
					//023
					"An external cause code from Chapter XX is used in combination with a code from another chapter to add information, especially if the code instructs you to add such an additional code. The external cause code should always be sequenced AFTER the disease chapter code."
						+ "\nAn external cause code from Chapter XX External causes of morbidity and mortality V01-Y98 is designed for the classification of: "
						+ "\n• external events and circumstances which are the cause of injury (includes transport accidents)" 
						+ "\n• poisoning "
						+ "\n• other adverse effects"
						+ "\nIf a map specialist assigns an ICD-10 code which requires an external cause code to provide the reason why it happened, and the information is not available in the description for the concept  to be mapped (for example, to classify the circumstances of an injury) then the map specialist must add the following map advice:"	
						+ "\nPOSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",	

					//024
					"No Map Rule restrictions for age will be applied in cases where there is a properly classified ICD-10 Map target."
					+ "\nExamples:"
					+ "\n•	“Juvenile arthritis” 239796000 will not be flagged for age context. “Juvenile arthritis” 239796000 maps to M08.99, Juvenile arthritis, unspecified."
					+ "\n•	“Juvenile glaucoma” 71111008 will not be flagged for age context. “Juvenile glaucoma” maps to H40.9 Glaucoma, unspecified. "
					+ "\n"
					+ "\n1.	Map Rules for age will not be applied to main SNOMED CT concepts that are high-level (greater than 10 descendants) and have a default code in the ICD-10 alphabetical index. "
					+ "\nThe exception would be for those conditions in ICD-10 that include a decisive age range to be considered even before applying a target ICD-10 code at default level, for example, by adding a current age rule for bronchitis (see principle 0025)."
					+ "\n"
					+ "\n2.	For those main SNOMED CT concepts that are low-level (less than 10 descendants) a condition related to age of onset may be included in SNOMED CT as a descendant.  "
					+ "\nIf an appropriate age-related condition is included as a descendant, a map rule showing that the mapping of the parent concept is dependent on age of onset is not required."
					+ "\nIf an age-related condition is not listed as a descendant, then a map rule showing the mapping of the main concept is dependent on age of onset is required."
					+ "\n"
					+ "\n3. The Map Rule for age context should be placed just above the ELSE rule."
					+ "\n"
					+ "\n4.	Definitions for common phases of life will be employed when SNOMED CT or ICD-10 employ descriptive terms. See Appendix 3 for guidance on age ranges.",

					//025
					"The addition of a map rule of ‘Current patient age’ allows identification in SNOMED CT that mapping to the ICD-10 target code is based on the actual chronological age of the patient.",
					
					//026
					"When mapping a SNOMED CT concept, if the SNOMED Tree View represents a different meaning to ICD 10, the map advice ‘MAPPED FOLLOWING IHTSDO GUIDANCE’ should be assigned.  This is because the map specialist will assign a target code based on the information given within the SNOMED Tree View.  In the given example, SNOMED CT defines a villous adenoma as a benign neoplasm but ICD-10 classifies it as a neoplasm of uncertain behavior.",
					
					//027
					"WHO does not give direct guidance on sequencing of poisoning codes, manifestations, and external causes.  Each country has its own method.  Because external cause codes are optional, their position will be last. So, the Mapping Service Team will map the poisoning code first, any manifestation [if known], and lastly the external cause code. ",
					
					//028
					"WHO handles open wounds that may accompany injuries in various ways.  The index trail may lead to one code only, or the mapping specialist may need to use the index to find two separate codes to appropriately describe a concept.   ",
					
					//029
					"When mapping a SNOMED CT concept which describes an injury or condition which results in loss of consciousness, only the accident or diagnosis code is required.  A target code for the loss of consciousness is not required as the only existing code in ICD-10 describes unconsciousness (coma).",
					
					//030
					"When more than one site of fracture is mentioned within a three character category in Chapter S, code to the specific “multiple fractures” code at fourth character level within that category (usually fourth character .7 in categories S00–S99). See example #1. For multiple injuries of the same fourth character subcategory code, map to the site-specific code.  See example #2. When more than one body region is involved or for bilateral fractures of the same site, coding should be made to the relevant category of Injuries involving multiple body regions (T00–T06) as per explicit WHO Volume 2 instructions.",
		
					//031
					"When SNOMED CT uses the phrase “and/or” within a concept description, the target code for both possibilities will be researched.  In the uncommon instance where the target codes for both possibilities are the same, the map will be completed with that specific code [example #1]. In the instance where the target codes for both possibilities conflict, the concept is not mappable [example #2]. Even if the two possible target codes are within the same ICD-10 category, the map is not mappable (i.e. the unspecified category code cannot be used) [example #2 continued].  ",
					
					//032
					"Bite injuries may or may not break the skin.  SNOMED CT has many concepts described as bite wounds that are not defined as open wounds.  WHO makes the assumption within ICD that all bites of animals and humans are open wounds.  Because WHO makes this assumption, MAPPED FOLLOWING WHO GUIDANCE advice is necessary when mapping these concepts. ",
					
					//033
					"Rare congenital disorders with multiple, various manifestations often do not have an entry within the ICD-10 alphabetical index.  Because patients may exhibit some, but not all, of a particular syndrome’s typical features, the map will not list every ICD-10 code that could be found to comprise that syndrome.  Instead, for syndromes with no index entry in ICD-10, www.orphanet.com will be the code source of reference.  ",
					
					//034
					"One of the definitions of ambiguity is discordance between a SNOMED CT definition and its synonyms.  Discordance is assessed relative to standard medical references.  Although confusing to a Mapping Specialist, the SNOMED CT definition itself is not truly ambiguous and the map for this concept will be completed.  However, the SNOMED CT term which is the source of the confusion will be flagged by the Mapping Team for Editorial Review with the expectation that the confusing term will be marked for demotion as a non-synonymous lexical tag. "
			};
			
			String[] icd10PrincipleReferences = {
					"Technical specification document",
					"",
					"http://apps.who.int/classifications/apps/icd/icd10training/",
					"",
					"ICD-10 Volume 2 page13- 2.4.1 Morphology of neoplasms. The morphology of neoplasms (pp.1177-1204) may be used if desired, as an additional code to classify the morphological type for neoplasms.",
					
					"http://apps.who.int/classifications/apps/icd/icd10training/ICD-10%20training/Start/index.html",
					"http://apps.who.int/classifications/apps/icd/icd10training/ICD-10%20training/Start/index.html",
					"MST Meeting Minutes 8/3/2012",
					"See page 27-30 of User Guidance:  Stand-Alone Mapping Tool at https://csfe.aceworkspace.net/sf/docman/do/downloadDocument/projects.mapping_service_team/docman.root.documentation.education_and_training/doc6264",
					"Mapping Service Team Meeting Minutes, 6th September 2012 , 27th September 2012",
					
					"ICD 10 2010 Volume 1 note at Z37.-",
					"ICD-10 Alphabetical Index ",
					"",
					"CliniClue Xplore:SctIntl-20120731",
					"ICD-10 (2008) Online Training",
					
					"Mapping SNOMED CT to ICD-10 Technical Specifications, 11.5 Mapping Context: Acquired versus Congenital ",
					"",
					"",
					"Agreed at consensus management panel",
					"WHO guidance see Appendix I for copy of email",
					
					"Mapping Service Team Meeting Minutes 20 November 2012",
					"http://apps.who.int/classifications/apps/icd/icd10training/ See appendix 2 for further information.",
					"WHO ICD-10 (2010) Online Training http://apps.who.int/classifications/apps/icd/icd10training/",
					"Mapping SNOMED CT to ICD-10 Technical Specifications Version 2.0, 11.4 Mapping Context: Patient Age at Onset ",
					"MST Minutes 09/11/2012",
					
					"",
					"",
					"",
					"Mapping Service Team Meeting discrepancy review 6 August 2013",
					"ICD-10 Online Training (version 2010)",
					
					"MST Meeting Minutes 20130730",
					"MST Meeting Minutes 20131204",
					"MST Meeting Minutes 20131211",
					"MST Meeting Minutes 20131211. Technical Implementation Guide sections 10 and 11"					
			};

			for (String value : adviceValues) {
				MapAdvice advice = new MapAdviceJpa();
				advice.setName(value);
				advice.setDetail(value);
				mapAdvices.add(advice);
			}
			tx.begin();
			Map<String, MapAdvice> mapAdviceValueMap = new HashMap<>();
			for (MapAdvice m : mapAdvices) {
				Logger.getLogger(this.getClass()).info(
						"Adding map advice " + m.getName());
				manager.persist(m);
				mapAdviceValueMap.put(m.getName(), m);
			}
			tx.commit();
			
			tx.begin();
			for (int i = 0; i < 34.; i++) {
				MapPrinciple principle = new MapPrincipleJpa();
				principle.setPrincipleId(icd10PrincipleIds[i]);
				principle.setName(icd10PrincipleTitles[i]);
				principle.setDetail(icd10Principles[i]);
				principle.setSectionRef(icd10PrincipleReferences[i]);
				Logger.getLogger(this.getClass()).info(
						"Adding map principle " + principle.getName());
				mapPrinciples.add(principle);
				manager.persist(principle);
			}
			tx.commit();

			// Add map projects
			Map<String, Long> refSetIdToMapProjectIdMap = new HashMap<String, Long>();
			MapProject mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICD10");
			mapProject.setRefSetId("447562003");
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20140131");
			mapProject.setDestinationTerminology("ICD10");
			mapProject.setDestinationTerminologyVersion("2010");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(true);
			mapProject.setPublished(true);
			mapProject.addMapLead(leads.get(0));
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			mapProject.addMapSpecialist(specialists.get(2));
			for (String s : icd10AdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			for (MapPrinciple m : mapPrinciples) {
				mapProject.addMapPrinciple(m);
			}
			
			
			Long mapProjectId = new Long("1");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
			projects.add(mapProject);

			mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICD9CM");
			mapProject.setRefSetId("5781347179");
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20140131");
			mapProject.setDestinationTerminology("ICD9CM");
			mapProject.setDestinationTerminologyVersion("2013");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(true);
			mapProject.setPublished(true);
			mapProject.addMapLead(leads.get(0));
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			for (String s : icd9cmAdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			
			mapProjectId = new Long("2");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
			projects.add(mapProject);

			mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICPC - Family Practice/GPF Refset");
			mapProject.setRefSetId("5235669");
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20130731");
			mapProject.setDestinationTerminology("ICPC");
			mapProject.setDestinationTerminologyVersion("2");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(false);
			mapProject.setPublished(false);
			mapProject.addMapLead(leads.get(2));
			mapProject.addMapSpecialist(specialists.get(3));
			for (String s : icpcAdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			mapProjectId = new Long("3");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
			projects.add(mapProject);

			// Set to assign map records to a project
			Map<String, Long> projectRefSetIdMap = new HashMap<String, Long>();
			
			tx.begin();
			javax.persistence.Query query; 
			for (MapProject m : projects) {
				
				Logger.getLogger(this.getClass()).info(
						"Adding map project " + m.getName());
				
				// get the refset id name
				query = manager.createQuery("select r from ConceptJpa r where r.terminologyId = " + m.getRefSetId() );
				
				try {
					m.setRefSetName( ((Concept) query.getSingleResult()).getDefaultPreferredName());
				} catch (Exception e) {
					getLog().info("No concept in database for this project");
					m.setRefSetName("Concept not in database");
				}
				manager.merge(m);
			}
			tx.commit();
			
			query = manager.createQuery("select r from MapProjectJpa r");
			
			projects = query.getResultList();			
			
			for (MapProject m : projects) {

				// <RefSetId, ProjectId>
				projectRefSetIdMap.put(m.getRefSetId(), m.getId());
				getLog().debug("    Add entry to map " + m.getRefSetId() + ", " + m.getId().toString());
			}
			
			// load map specialist Patrick Granvold for use in note setting

			MapUser mapUser = new MapUserJpa();
			mapUser.setName("Patrick Granvold");
			mapUser.setEmail("pranvold@westcoastinformatics.com");
			mapUser.setUserName("pgranvold");
			
			tx.begin();
			manager.persist(mapUser);
			tx.commit();



			// Load map records from complex map refset members
			long prevConceptId = -1;
			MapRecord mapRecord = null;

		  query =
					manager
							.createQuery("select r from ComplexMapRefSetMemberJpa r order by r.concept.id, " +
									"r.mapBlock, r.mapGroup, r.mapPriority");
			getLog().debug("    complex refset member size "
					+ query.getResultList().size());
			
			// instantiate MappingService for getting descendant concepts
			ContentService contentService = new ContentServiceJpa();
			
			// Added to speed up process
			tx.begin();
			int i = 0;// for progress tracking

			for (Object member : query.getResultList()) {
				
				ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
				
				if(refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|.*\\s\\|") &&
			    !(refSetMember.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")) &&
			    !(refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]"))){
				  getLog().debug("    skipping refSetMember: " + refSetMember.getConcept().getTerminologyId() + " : " + 
				    refSetMember.getMapRule() + " : " + refSetMember.getMapAdvice()); 
				  continue; 
				}
				
				// retrieve the concept
				Concept concept = refSetMember.getConcept();
				
				// if no concept for this ref set member, skip
				if (concept == null)
					continue;
				
				// if different concept than previous ref set member, create new mapRecord
				if (!concept.getTerminologyId()
						.equals(new Long(prevConceptId).toString())) {
					
					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(concept.getTerminologyId());
					mapRecord.setConceptName(concept.getDefaultPreferredName());
					
					// add a test note to this mapRecord
					MapNote note = new MapNoteJpa();
					note.setNote("Test MapRecord note");
					note.setUser(mapUser);
					note.setTimestamp(Calendar.getInstance().getTime());
					
					mapRecord.addMapNote(note);
				
					
					// if this refSet terminology id in project map, set the project id
					if (projectRefSetIdMap.containsKey(refSetMember.getRefSetId())) {
						mapRecord.setMapProjectId(projectRefSetIdMap.get(refSetMember.getRefSetId()));
					} 
				
					// get the number of descendants
					mapRecord.setCountDescendantConcepts( new Long(
							contentService.getDescendants(
								concept.getTerminologyId(),
								concept.getTerminology(),
								concept.getTerminologyVersion(),
								new Long("116680003")).size()));
			
					// set the previous concept to this concept
					prevConceptId = new Long(refSetMember.getConcept().getTerminologyId());
					
					// persist the record
					manager.persist(mapRecord);
				}

				// add map entry to record
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTargetId(refSetMember.getMapTarget());
				mapEntry.setTargetName(null); // TODO: Change this once we have other terminologies loaded
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
				mapEntry.setRule(refSetMember.getMapRule());
				mapEntry.setMapGroup(1);
				mapEntry.setMapBlock(1);
				
				// add a test note to MapEntry
				MapNote note = new MapNoteJpa();
				note.setNote("Test MapEntry note");
				note.setUser(mapUser);
				note.setTimestamp(Calendar.getInstance().getTime());
				mapEntry.addMapNote(note);
				
				// find the correct advice and add it
				if (mapAdviceValueMap.containsKey(refSetMember.getMapAdvice())) {
					mapEntry
							.addMapAdvice(mapAdviceValueMap.get(refSetMember.getMapAdvice()));
				}
				mapRecord.addMapEntry(mapEntry);
				
				manager.merge(mapRecord);
				
				if (++i % 1000 == 0) {getLog().info(Integer.toString(i) + " map records processed");}

			}
			
			// Commit all map records
			getLog().info("     Committing...");
			tx.commit();
			getLog().info("...done");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
