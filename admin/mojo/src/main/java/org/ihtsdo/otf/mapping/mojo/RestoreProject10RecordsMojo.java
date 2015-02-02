package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal restore-project-10-records
 * @phase package
 */
public class RestoreProject10RecordsMojo extends AbstractMojo {

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Restore project 10 records");

    try {

      MappingService service = new MappingServiceJpa();
      OUTER: for (String id : getIds()) {
        MapRecordList list = service.getMapRecordRevisionsForConcept(id, 10L);
        // iterate from top of list until we find a non-"wci" owned entry
        for (MapRecord record : list.getMapRecords()) {
          getLog().info(
              "Record Info: " + record.getConceptId() + ", "
                  + record.getLastModifiedBy().getUserName() + ", "
                  + record.getWorkflowStatus());
          // Skip ones that need review
          if (record.getLastModifiedBy().getUserName().equals("wci")
              && record.getWorkflowStatus() == WorkflowStatus.REVIEW_NEEDED) {
            getLog().info("  SKIPPED");
            continue OUTER;
          }

          if (!record.getLastModifiedBy().getUserName().equals("wci")
              && record.getWorkflowStatus() == WorkflowStatus.READY_FOR_PUBLICATION) {
            // found record, restore this one.
            getLog().info("  FOUND");

            MapRecordList list2 =
                service.getMapRecordsForProjectAndConcept(10L,
                    record.getConceptId());
            if (list2.getCount() == 0) {
              getLog().debug("  SKIP");
              continue OUTER;
            }
            if (list2.getCount() != 1) {
              throw new Exception("Expected only a single record for "
                  + record.getConceptId());
            }
            MapRecord toremove = list2.getMapRecords().iterator().next();
            if (!toremove.getLastModifiedBy().getUserName().equals("wci")) {
              throw new Exception("Expected a wci record");
            }
            if (toremove.getWorkflowStatus() != WorkflowStatus.PUBLISHED) {
              throw new Exception("Expected a PUBLISHED record");
            }
            getLog().debug("REMOVE " + toremove);
            for (MapEntry entry : toremove.getMapEntries()) {
              getLog().debug("  ENTRY = " + entry);
            }
            handleMapRecordLazyInitialization(record);
            MapRecord toinsert = new MapRecordJpa(record, false);
            getLog().debug("ADD " + toinsert);
            for (MapEntry entry : toinsert.getMapEntries()) {
              getLog().debug("  ENTRY = " + entry);
            }
            getLog().debug("\n");

            service.removeMapRecord(toremove.getId());
            service.addMapRecord(toinsert);
            continue OUTER;
          }

        }
      }
      service.close();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Loading of Unpublished RF2 Complex Maps failed.", e);
    }

  }

  /**
   * Handle map record lazy initialization.
   *
   * @param mapRecord the map record
   */
  private void handleMapRecordLazyInitialization(MapRecord mapRecord) {
    // handle all lazy initializations
    mapRecord.getOwner().getEmail();
    mapRecord.getLastModifiedBy().getEmail();
    mapRecord.getMapNotes().size();
    mapRecord.getMapPrinciples().size();
    mapRecord.getOriginIds().size();
    mapRecord.getLabels().size();
    mapRecord.getReasonsForConflict().size();
    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      if (mapEntry.getMapRelation() != null)
        mapEntry.getMapRelation().getName();
      mapEntry.getMapAdvices().size();
    }

  }

  /**
   * Returns the ids.
   *
   * @return the ids
   */
  private Set<String> getIds() {
    Set<String> set = new HashSet<>();
    set.add("279001004");
    set.add("50415004");
    set.add("427304006");
    set.add("162504001");
    set.add("301371003");
    set.add("37226004");
    set.add("10337008");
    set.add("117130002");
    set.add("80547003");
    set.add("246563002");
    set.add("246564008");
    set.add("438113009");
    set.add("21165006");
    set.add("424600001");
    set.add("12138000");
    set.add("13217005");
    set.add("67741005");
    set.add("427044009");
    set.add("427555000");
    set.add("7640004");
    set.add("93182006");
    set.add("413656006");
    set.add("413843002");
    set.add("93501005");
    set.add("92514004");
    set.add("92511007");
    set.add("188511002");
    set.add("404144008");
    set.add("188562004");
    set.add("188580008");
    set.add("188568000");
    set.add("93151007");
    set.add("404118008");
    set.add("94712003");
    set.add("188635004");
    set.add("419868009");
    set.add("16442005");
    set.add("234241007");
    set.add("44622003");
    set.add("387759001");
    set.add("79099006");
    set.add("36622002");
    set.add("41832009");
    set.add("247804008");
    set.add("296355001");
    set.add("238061001");
    set.add("296830004");
    set.add("160464009");
    set.add("160429000");
    set.add("433492005");
    set.add("257494002");
    set.add("401270003");
    set.add("277841004");
    set.add("106134006");
    set.add("443999008");
    set.add("248608006");
    set.add("443429007");
    set.add("163772009");
    set.add("160267000");
    set.add("160270001");
    set.add("415036007");
    set.add("431330007");
    set.add("266901001");
    set.add("430088005");
    set.add("430815001");
    set.add("315626001");
    set.add("401065001");
    set.add("160378006");
    set.add("275940001");
    set.add("433278001");
    set.add("164648004");
    set.add("427668002");
    set.add("162505000");
    set.add("426555006");
    set.add("39565009");
    set.add("49575005");
    set.add("405496006");
    set.add("417228009");
    set.add("391118006");
    set.add("248689000");
    set.add("59495006");
    set.add("15157000");
    set.add("117122003");
    set.add("43889001");
    set.add("275958007");
    set.add("164615004");
    set.add("165389001");
    set.add("163153004");
    set.add("224459001");
    set.add("224461005");
    set.add("224440003");
    set.add("225861007");
    set.add("249994007");
    set.add("164641005");
    set.add("248467005");
    set.add("102507004");
    set.add("267044007");
    set.add("44170005");
    set.add("117120006");
    set.add("117123008");
    set.add("59685007");
    set.add("123893002");
    set.add("123881006");
    set.add("123921008");
    set.add("123909009");
    set.add("79940008");
    set.add("123887005");
    set.add("123875003");
    set.add("24713001");
    set.add("60622002");
    set.add("58744002");
    set.add("30844003");
    set.add("14113009");
    set.add("83870002");
    set.add("45567009");
    set.add("70896009");
    set.add("91536003");
    set.add("30312008");
    set.add("68258002");
    set.add("68814008");
    set.add("4075006");
    set.add("30112004");
    set.add("32171005");
    set.add("123938007");
    set.add("123929005");
    set.add("123942005");
    set.add("123914008");
    set.add("22957009");
    set.add("27759007");
    set.add("44137000");
    set.add("5877009");
    set.add("82958007");
    set.add("88126006");
    set.add("21019005");
    set.add("68102003");
    set.add("53819003");
    set.add("32271000");
    set.add("12306004");
    set.add("31199003");
    set.add("1387009");
    set.add("50117000");
    set.add("8018005");
    set.add("34586006");
    set.add("5859004");
    set.add("84469002");
    set.add("75708007");
    set.add("71843006");
    set.add("14508001");
    set.add("38173001");
    set.add("4367009");
    set.add("35508004");
    set.add("68009005");
    set.add("80525003");
    set.add("82668000");
    set.add("70651004");
    set.add("74437002");
    set.add("85905009");
    set.add("31265008");
    set.add("81694006");
    set.add("163842007");
    set.add("365877008");
    set.add("271349002");
    set.add("299836006");
    set.add("165945006");
    set.add("168350006");
    set.add("168293008");
    set.add("168143002");
    set.add("299313006");
    set.add("299403002");
    set.add("299410008");
    set.add("274604009");
    set.add("250264008");
    set.add("302785009");
    set.add("310262007");
    set.add("314137006");
    set.add("42915007");
    set.add("250064001");
    set.add("28328005");
    set.add("167855007");
    set.add("165143003");
    set.add("102845008");
    set.add("102854006");
    set.add("102856008");
    set.add("170156009");
    set.add("170307001");
    set.add("170259009");
    set.add("170155008");
    set.add("251351004");
    set.add("251564002");
    set.add("41835006");
    set.add("251523007");
    set.add("167601005");
    set.add("7667005");
    set.add("167992006");
    set.add("277910004");
    set.add("249621006");
    set.add("7766007");
    set.add("109978004");
    set.add("277654008");
    set.add("305907000");
    set.add("305908005");
    set.add("313340009");
    set.add("307062006");
    set.add("295403005");
    set.add("297085009");
    set.add("295407006");
    set.add("440565004");
    set.add("23954006");
    set.add("38212005");
    set.add("408691009");
    set.add("401292004");
    set.add("129898001");
    set.add("33005006");
    set.add("248659007");
    set.add("31038006");
    set.add("22499001");
    set.add("445413001");
    set.add("391100008");
    set.add("251988007");
    set.add("248888005");
    set.add("249544004");
    set.add("249560000");
    set.add("67672003");
    set.add("71405002");
    set.add("413235002");
    set.add("413236001");
    set.add("20036005");
    set.add("248914002");
    set.add("2170000");
    set.add("248696003");
    set.add("248692001");
    set.add("76398007");
    set.add("82048005");
    set.add("248671001");
    set.add("248614004");
    set.add("163067006");
    set.add("163070005");
    set.add("163081008");
    set.add("163075000");
    set.add("224449002");
    set.add("28030000");
    set.add("424283009");
    set.add("425383001");
    set.add("53244004");
    set.add("405641009");
    set.add("43912002");
    set.add("8779001");
    set.add("11593000");
    set.add("46833000");
    set.add("20731008");
    set.add("111790004");
    set.add("123932008");
    set.add("188589009");
    set.add("54267006");
    set.add("217936008");
    set.add("242724008");
    set.add("217954007");
    set.add("217961006");
    set.add("276374003");
    set.add("164301009");
    set.add("14041005");
    set.add("274286000");
    set.add("38867000");
    set.add("284465006");
    set.add("295879004");
    set.add("295862005");
    set.add("242830002");
    set.add("295828005");
    set.add("295933005");
    set.add("295518009");
    set.add("242826000");
    set.add("295183001");
    set.add("295725000");
    set.add("295707007");
    set.add("295686005");
    set.add("296223003");
    set.add("295417001");
    set.add("295299004");
    set.add("295262004");
    set.add("296478006");
    set.add("33300005");
    set.add("19391002");
    set.add("425274008");
    set.add("37298006");
    set.add("48433002");
    set.add("59919008");
    set.add("405358009");
    set.add("293416008");
    set.add("293032001");
    set.add("292712000");
    set.add("292125003");
    set.add("292225007");
    set.add("292209002");
    set.add("218794000");
    set.add("292264005");
    set.add("292303004");
    set.add("292354004");
    set.add("292352000");
    set.add("292356002");
    set.add("292407002");
    set.add("292378009");
    set.add("292324009");
    set.add("292149000");
    set.add("292421000");
    set.add("418370000");
    set.add("292472009");
    set.add("292553006");
    set.add("292606000");
    set.add("218610002");
    set.add("28631002");
    set.add("422485001");
    set.add("296068003");
    set.add("296108003");
    set.add("296171008");
    set.add("402486005");
    set.add("246949000");
    set.add("246936005");
    set.add("125128004");
    set.add("102483000");
    set.add("53388009");
    set.add("422376000");
    set.add("163843002");
    set.add("299829005");
    set.add("163837004");
    set.add("163792002");
    set.add("299891004");
    set.add("418450005");
    set.add("405897009");
    set.add("412734009");
    set.add("25194005");
    set.add("33054004");
    set.add("442571003");
    set.add("166940007");
    set.add("166935004");
    set.add("166479009");
    set.add("131026001");
    set.add("168074003");
    set.add("442485006");
    set.add("441793007");
    set.add("444138006");
    set.add("315333006");
    set.add("310406009");
    set.add("373679006");
    set.add("167958008");
    set.add("129749001");
    set.add("129764001");
    set.add("168015009");
    set.add("128605003");
    set.add("31841001");
    set.add("11578004");
    set.add("23740006");
    set.add("81445002");
    set.add("402157004");
    set.add("201162002");
    set.add("403261006");
    set.add("238994000");
    set.add("24152003");
    set.add("238549003");
    set.add("30873000");
    set.add("235629003");
    set.add("197271008");
    set.add("410481000");
    set.add("235659008");
    set.add("405589007");
    set.add("165928001");
    set.add("206262006");
    set.add("289652008");
    set.add("289654009");
    set.add("163984003");
    set.add("289332002");
    set.add("162173004");
    set.add("112070001");
    set.add("124735008");
    set.add("237319004");
    set.add("237320005");
    set.add("396794002");
    set.add("237748007");
    set.add("240164006");
    set.add("167644009");
    set.add("263815005");
    set.add("293382000");
    set.add("292788008");
    set.add("75260002");
    set.add("60086000");
    set.add("128527000");
    set.add("75047002");
    set.add("292129009");
    set.add("292138006");
    set.add("292221003");
    set.add("292204007");
    set.add("292230006");
    set.add("292252002");
    set.add("441858005");
    set.add("292476007");
    set.add("292051004");
    set.add("292061006");
    set.add("292263004");
    set.add("292313007");
    set.add("292279003");
    set.add("292304005");
    set.add("292351007");
    set.add("292359009");
    set.add("292329004");
    set.add("292358001");
    set.add("292385008");
    set.add("292273002");
    set.add("292338002");
    set.add("292151001");
    set.add("292538003");
    set.add("292517009");
    set.add("292428006");
    set.add("292528008");
    set.add("292502005");
    set.add("292505007");
    set.add("292576009");
    set.add("209766006");
    set.add("238867003");
    set.add("402647004");
    set.add("109247006");
    set.add("95365001");
    set.add("234453008");
    set.add("255040004");
    set.add("292627005");
    set.add("293494005");
    set.add("292779004");
    set.add("293561005");
    set.add("293498008");
    set.add("218968003");
    set.add("293458006");
    set.add("293347001");
    set.add("292157002");
    set.add("293354007");
    set.add("293350003");
    set.add("419253001");
    set.add("293386002");
    set.add("293392008");
    set.add("293393003");
    set.add("218972004");
    set.add("292255000");
    set.add("292122000");
    set.add("292104003");
    set.add("292493007");
    set.add("292571004");
    set.add("292592004");
    set.add("292822002");
    set.add("292873005");
    set.add("292903005");
    set.add("292918006");
    set.add("292930000");
    set.add("419056004");
    set.add("292951002");
    set.add("292949001");
    set.add("418328005");
    set.add("293052000");
    set.add("293055003");
    set.add("218356002");
    set.add("293037007");
    set.add("292960005");
    set.add("292964001");
    set.add("292997000");
    set.add("293126009");
    set.add("293146004");
    set.add("293163000");
    set.add("293150006");
    set.add("293162005");
    set.add("440385001");
    set.add("293236006");
    set.add("293258008");
    set.add("293229005");
    set.add("218572006");
    set.add("268795001");
    set.add("206040001");
    set.add("206044005");
    set.add("206018005");
    set.add("268797009");
    set.add("206126004");
    set.add("191345000");
    set.add("399131003");
    set.add("307749004");
    set.add("277785006");
    set.add("371627004");
    set.add("363338001");
    set.add("3160009");
    set.add("73120006");
    set.add("262894003");
    set.add("38780008");
    set.add("302928000");
    set.add("371347004");
    set.add("413835007");
    set.add("27438001");
    set.add("46981006");
    set.add("31323000");
    set.add("190681003");
    set.add("44673006");
    set.add("76175005");
    set.add("297226004");
    set.add("190732007");
    set.add("297235006");
    set.add("297233004");
    set.add("297232009");
    set.add("237913008");
    set.add("237941007");
    set.add("237933007");
    set.add("45812003");
    set.add("402456003");
    set.add("170765005");
    set.add("237624007");
    set.add("237964009");
    set.add("31220004");
    set.add("47719001");
    set.add("47757001");
    set.add("49013001");
    set.add("276262000");
    set.add("238123002");
    set.add("238037008");
    set.add("238041007");
    set.add("129590000");
    set.add("238096001");
    set.add("6241000");
    set.add("363233007");
    set.add("22063003");
    set.add("307127004");
    set.add("2359002");
    set.add("89579000");
    set.add("129645002");
    set.add("237953006");
    set.add("16517004");
    set.add("233720006");
    set.add("192788009");
    set.add("15892005");
    set.add("59990008");
    set.add("58112007");
    set.add("59957008");
    set.add("238018004");
    set.add("29692004");
    set.add("419989001");
    set.add("292090000");
    set.add("292673007");
    set.add("292679006");
    set.add("292707001");
    set.add("292726009");
    set.add("292742002");
    set.add("293405001");
    set.add("292794000");
    set.add("292815005");
    set.add("292842006");
    set.add("292866000");
    set.add("292868004");
    set.add("292950001");
    set.add("293024009");
    set.add("293074006");
    set.add("293081004");
    set.add("293082006");
    set.add("293039005");
    set.add("292980006");
    set.add("292982003");
    set.add("417906002");
    set.add("293254005");
    set.add("293244006");
    set.add("218488009");
    set.add("293426001");
    set.add("293462000");
    set.add("293461007");
    set.add("293473007");
    set.add("293472002");
    set.add("293512008");
    set.add("292539006");
    set.add("292662000");
    set.add("293191008");
    set.add("293202000");
    set.add("298481006");
    set.add("298480007");
    set.add("299555003");
    set.add("95454007");
    set.add("85244007");
    set.add("82542004");
    set.add("44241007");
    set.add("363300000");
    set.add("93546006");
    set.add("105628008");
    set.add("300260007");
    set.add("300263009");
    set.add("248420006");
    set.add("228444003");
    set.add("87866006");
    set.add("236524006");
    set.add("236594004");
    set.add("402361002");
    set.add("292243006");
    set.add("218394002");
    set.add("373932008");
    set.add("58593005");
    set.add("68600005");
    set.add("293086009");
    set.add("403623004");
    set.add("403563005");
    set.add("234217009");
    set.add("281451005");
    set.add("95404001");
    set.add("418019003");
    set.add("288274003");
    set.add("72496005");
    set.add("288926008");
    set.add("288754009");
    set.add("267322007");
    set.add("199284009");
    set.add("260298002");
    set.add("422901001");
    set.add("410471004");
    set.add("95607001");
    set.add("85796009");
    set.add("255320000");
    set.add("163616009");
    set.add("236539007");
    set.add("95375003");
    set.add("13310005");
    set.add("271077003");
    set.add("215982004");
    set.add("35402000");
    set.add("417646006");
    set.add("129063003");
    set.add("360929005");
    set.add("269908006");
    set.add("416463008");
    set.add("363019004");
    set.add("363020005");
    set.add("24346005");
    set.add("75836008");
    set.add("267931000");
    set.add("236506009");
    set.add("271324001");
    set.add("31999004");
    set.add("312135004");
    set.add("72807002");
    set.add("42887008");
    set.add("423629005");
    set.add("302855005");
    set.add("277627005");
    set.add("295642000");
    set.add("296298004");
    set.add("296543008");
    set.add("296605004");
    set.add("296655007");
    set.add("296717009");
    set.add("296748001");
    set.add("296790007");
    set.add("296573004");
    set.add("296920007");
    set.add("296855005");
    set.add("296860009");
    set.add("296953002");
    set.add("297047006");
    set.add("297012003");
    set.add("297074007");
    set.add("295581008");
    set.add("296599006");
    set.add("296666000");
    set.add("296482008");
    set.add("296723004");
    set.add("296731009");
    set.add("296459006");
    set.add("297072006");
    set.add("296797005");
    set.add("296533005");
    set.add("296555002");
    set.add("297045003");
    set.add("296885000");
    set.add("296628003");
    set.add("296624001");
    set.add("75468006");
    set.add("35633007");
    set.add("296466007");
    set.add("371089000");
    set.add("442761002");
    set.add("205512009");
    set.add("296034009");
    set.add("296202009");
    set.add("296250008");
    set.add("296781000");
    set.add("296833002");
    set.add("236019001");
    set.add("417490005");
    set.add("296359007");
    set.add("296421003");
    set.add("296219006");
    set.add("295941005");
    set.add("296461002");
    set.add("299903009");
    set.add("299907005");
    set.add("193250002");
    set.add("60970005");
    set.add("407622005");
    set.add("58460004");
    set.add("281368005");
    set.add("296848008");
    set.add("296919001");
    set.add("296895007");
    set.add("296455000");
    set.add("296438004");
    set.add("296346004");
    set.add("404130002");
    set.add("404146005");
    set.add("93510002");
    set.add("404120006");
    return set;
  }
}