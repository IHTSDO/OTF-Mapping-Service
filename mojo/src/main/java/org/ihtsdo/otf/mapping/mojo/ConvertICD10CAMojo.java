/*
 *    Copyright 2020 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.Map.Entry;

/**
 * Converts icd10ca html files into format required for SimpleLoaderAlgorithm.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal convert-icd10ca
 * @phase package
 */
public class ConvertICD10CAMojo extends AbstractOtfMappingMojo {

  /**
   * The input directory for icd10ca html files
   * @parameter
   * @required
   */
  private String inputDir;

  /**
   * The output directory for simple loader files
   * @parameter
   * @required
   */
  private String outputDir;

  // file writers
  private BufferedWriter conceptWriter = null;

  private BufferedWriter parentChildWriter = null;

  private BufferedWriter conceptAttributeWriter = null;

  private BufferedWriter simpleRefsetMemberWriter = null;

  private BufferedWriter relationshipWriter = null;

  // ignore attributes for these codes - malformed or complex html
  private Set<String> codesToIgnoreAttributes = new HashSet<>();

  // ignore custom embedded tables for these codes
  private Set<String> ignoreEmbedded = new HashSet<>();

  // store output to ensure unique values
  private Set<String> simpleRefsetMemberSet = new HashSet<>();

  // store output to ensure unique values
  private Set<String> conceptRelationshipSet = new HashSet<>();
  
  // store output to ensure unique values
  private Set<String> conceptAttributeSet = new HashSet<>();
  
  //store output to ensure unique values
  private Set<String> parentChildSet = new HashSet<>();
  
  private Map<String, String> conceptMap = new HashMap<>();

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Start converting ICD10CA html files to Simple Loader format");

    try {

      getLog().info("  inputDir: " + inputDir);
      getLog().info("  outputDir: " + outputDir);

      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new MojoFailureException("Specified input directory does not exist");
      }

      File outputDirFile = new File(outputDir);
      if (!outputDirFile.exists()) {
        throw new MojoFailureException("Specified output directory does not exist");
      }

      // ignore attributes for these codes
      codesToIgnoreAttributes.add("J96");
      
      ignoreEmbedded.add("T31");
      ignoreEmbedded.add("T32");

      
      // write concepts.txt
      writeConcepts(inputDirFile, outputDirFile);
      
      for (Entry<String, String> entry : conceptMap.entrySet()) {
        // check if hard-coded fix for Z38 issues needed
        String value = checkForSingletonTwin(entry);
        conceptWriter.write(entry.getKey() + "|" + value + "\n");
      }
      // hard-code burn concepts from T31, T32 embedded tables
      Set<String> burnConcepts = new HashSet<>();
      burnConcepts = addBurnConcepts(burnConcepts);
      for (String burnConcept : burnConcepts) {
        conceptWriter.write(burnConcept + "\n");
      }
      
      // write parent-child.txt
      writeParentChild(inputDirFile, outputDirFile);
      
      // hardcode super-spans to modify parent-child hierarchy (due to erroneous html representation)
      Set<String> superSpans = new HashSet<>();
      superSpans = addSuperSpans(superSpans);
      
      // when writing out parent-children, skip entries where the child has a revised parent
      // from the superSpans list
      for (String member : parentChildSet) {
        boolean found = false;
        for (String span : superSpans) {        
          if(span.endsWith("|" + member.substring(member.indexOf("|") + 1))) {
            found = true;
            System.out.println("invalid parent-child entry: " + member);
            continue;
          }
        }
        if (!found) {
          parentChildWriter.write(member + "\n");
        }
      }
      // write out replacement parent-child entries
      for (String member : superSpans) {
        parentChildWriter.write(member + "\n");
      }
      // write out burnConcepts parent-child entries
      for (String burnConcept : burnConcepts) {
        String code = burnConcept.substring(0, burnConcept.indexOf("|"));
        if (code.contains(".") && Pattern.matches("[A-Z][0-9][0-9].[0-9]", code)) {
          parentChildWriter.write(code.substring(0, code.indexOf(".") ) + "|" + code + "\n");
        } else if (code.contains(".") && Pattern.matches("[A-Z][0-9][0-9].[0-9][0-9]", code)) {
          parentChildWriter.write(code.substring(0, code.indexOf(".") + 2) + "|" + code + "\n");
        }
      }
    
      // write concept-attributes.txt
      writeConceptDetails(inputDirFile);
      for (String member : conceptAttributeSet) {
        // don't write out any blank attributes/descriptions
        if (!member.trim().replaceAll("\\u00A0", "").endsWith("|")) {
          conceptAttributeWriter.write(member + "\n");
        }
      }

      // write simple-refset-members.txt
      for (String member : simpleRefsetMemberSet) {
        simpleRefsetMemberWriter.write(member + "\n");
      }

      // write concept-relationships.txt
      Set<String> code1Code2 = new HashSet<>();
      for (String member : conceptRelationshipSet) {
        String[] fields = member.split("\\|");
        // check for invalid range in code2, if found, modify to single target code
        if (fields[1].contains("-")) {
          if (!conceptMap.containsKey(fields[1])) {
            System.out.println("codeRange invalid " + fields[1]);
            fields[1] = fields[1].substring(0, fields[1].indexOf("-"));
            relationshipWriter.write(fields[0] + "|" + fields[1] + "|" + fields[2] + "|" + fields[3] + "\n");
            continue;
          }
        }
        // only keep one rel for each code1|code2 tuple
        if (!code1Code2.contains(fields[0] + "|" + fields[1] + "|" + fields[2])) {
          relationshipWriter.write(member + "\n");
          code1Code2.add(fields[0] + "|" + fields[1] + "|" + fields[2]);
        }
      }
      
      

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Converting of icd10ca html files failed.", e);
    } finally {
      try {
        conceptWriter.close();
        conceptAttributeWriter.close();
        parentChildWriter.close();
        simpleRefsetMemberWriter.close();
        relationshipWriter.close();
      } catch (IOException e1) {
        // do nothing
      }
    }

  }

  
  /**
   * Write each ICD10CA conceptId and term to the concepts.txt file. Also calls
   * processing for the concept-attributes.txt and concept-relationships.txt
   * files.
   *
   * @param inputDirFile the input dir file
   * @param outputDirFile the output dir file
   * @throws Exception the exception
   */
  private void writeConcepts(File inputDirFile, File outputDirFile) throws Exception {
    File conceptsFile = new File(outputDirFile, "concepts.txt");
    // if file doesn't exist, then create it
    if (!conceptsFile.exists()) {
      conceptsFile.createNewFile();
    }
    conceptWriter = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(conceptsFile.getAbsolutePath()), StandardCharsets.UTF_8));

    File attributesFile = new File(outputDirFile, "concept-attributes.txt");
    // if file doesn't exist, then create it
    if (!attributesFile.exists()) {
      attributesFile.createNewFile();
    }
    conceptAttributeWriter = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(attributesFile.getAbsolutePath()), StandardCharsets.UTF_8));
    conceptAttributeWriter.write("Exclusion\n");
    conceptAttributeWriter.write("Inclusion\n");
    conceptAttributeWriter.write("Note\n");
    conceptAttributeWriter.write("Coding hint\n");

    File simpleRefsetMemberFile = new File(outputDirFile, "simple-refset-members.txt");
    // if file doesn't exist, then create it
    if (!simpleRefsetMemberFile.exists()) {
      simpleRefsetMemberFile.createNewFile();
    }
    simpleRefsetMemberWriter = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(simpleRefsetMemberFile.getAbsolutePath()), StandardCharsets.UTF_8));

    File relationshipFile = new File(outputDirFile, "concept-relationships.txt");
    // if file doesn't exist, then create it
    if (!relationshipFile.exists()) {
      relationshipFile.createNewFile();
    }
    relationshipWriter = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(relationshipFile.getAbsolutePath()), StandardCharsets.UTF_8));

    // get all relevant icd10ca .html files
    FilenameFilter projectFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        String lowercaseName = name.toLowerCase();
        if ( // remove index files
        !(lowercaseName.startsWith("6") && lowercaseName.length() >= 11)
            // keep only .html files
            && lowercaseName.endsWith(".html")
        // ignore conceptDetail files
            && !lowercaseName.startsWith("conceptdetail")
        // remove documentation file
            && !lowercaseName.contentEquals("371216.html")
        // remove tabulation index
            && !lowercaseName.contains("371324.html")
        // remove Appendix B
            && !lowercaseName.contains("371336.html")
        // remove Conventions file
            && !lowercaseName.contains("371198.html")
        // remove Morphology of Neoplasms
            && !lowercaseName.contains("352738.html")) {
          return true;
        } else {
          return false;
        }
      }
    };
    File[] projectFiles = inputDirFile.listFiles(projectFilter);

    // iterate through relevant files
    for (File file : projectFiles) {

      org.jsoup.nodes.Document doc = Jsoup.parse(file, "ISO-8859-9", "");
      ((org.jsoup.nodes.Document) doc).outputSettings().charset().forName("UTF-8");
      ((org.jsoup.nodes.Document) doc).outputSettings().escapeMode(EscapeMode.xhtml);
      if (doc.select("table").size() == 0) {
        continue;
      }

      String previousCode = "";
      Element table = doc.select("table").get(0); // select the first table.
      Elements rows = table.select("tr");

      // if index file, skip this file
      if (rows != null && rows.size() >= 3 && rows.get(2) != null
          && rows.get(2).select("td").get(0).text().length() == 1) {
        continue;
      }

      // for each row in the table
      for (int i = 2; i < rows.size(); i++) {
        Element row = rows.get(i);
        Elements cols = row.select("td");

        // write any attributes for the previousCode
        Elements includes = row.select("[class='include']");
        Elements excludes = row.select("[class='exclude']");
        Elements notes = row.select("[class='note']");
        Elements codealsos = row.select("[class='codealso']");

        if (!codesToIgnoreAttributes.contains(previousCode)) {
          processAttributes(includes, previousCode, "Inclusion");
          processAttributes(excludes, previousCode, "Exclusion");
          processAttributes(notes, previousCode, "Note");
          processAttributes(codealsos, previousCode, "Coding hint");
        }

        // Get chapter header concepts
        if (cols.size() >= 1 && cols.get(0).text().startsWith("Chapter")) {
          String colText = cols.get(0).text();
          colText = colText.substring(8);
          String code = colText.substring(0, colText.indexOf("-") - 1);
          int stop = colText.indexOf("(") != -1 ? colText.indexOf("(") - 1 : colText.length();
          String term = colText.substring(colText.indexOf("-") + 2, stop);
          if (!conceptMap.containsKey(code)) {
            conceptMap.put(code, term);
            previousCode = code;
          }
        }

        // if not chapter header
        else if (cols.size() >= 2) {
         
          // ensure no run-on codes (due to embedded tables)
          if (cols.get(0).hasText() && cols.get(1).hasText()
              && Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(cols.get(0).text()))
              && cols.get(0).text().length() < 10) {
            if (cols.get(0).text().endsWith("*")) {
              simpleRefsetMemberSet.add(cleanCode(cols.get(0).text()) + "|Asterisk refset");
              writeRelationship(cols.get(0).text(), cols.get(1).text());
            } else if (cols.get(0).text().endsWith("†")) {
              simpleRefsetMemberSet.add(cleanCode(cols.get(0).text()) + "|Dagger refset");
              writeRelationship(cols.get(0).text(), cols.get(1).text());
            }
            // ensure no duplicates
            if (!conceptMap.containsKey(cleanCode(cols.get(0).text()))) {
              conceptMap.put(cleanCode(cols.get(0).text()), cols.get(1).text());
            }
            previousCode = cols.get(0).text();
            // process sub-header row
          } else if (cols.get(0).hasText() && cols.get(0).className().contentEquals("bl1")) {
            previousCode = cleanCode(cols.get(0).text()).substring(
                cleanCode(cols.get(0).text()).lastIndexOf("(") + 1,
                cleanCode(cols.get(0).text()).lastIndexOf(")"));
            // process embedded table
          } else if (cols.get(0).getElementsByTag("table").size() > 0) {
            System.out.println("embedded table on " + previousCode);
            if (ignoreEmbedded.contains(previousCode)) {
              continue;
            }
            
            if (cols.get(0).getElementsByTag("thead").size() > 0
                && cols.get(0).getElementsByTag("tbody").size() > 0) {
              // process header row
              Element header = cols.get(0).getElementsByTag("thead").first();
              Elements headerElements = header.getElementsByTag("td");
              List<String> headers = new ArrayList<>();
              for (int j = 1; j < headerElements.size(); j++) {
                headers.add(headerElements.get(j).ownText());
              }
              // process table body rows
              Element body = cols.get(0).getElementsByTag("tbody").first();
              Elements bodyRows = body.getElementsByTag("tr");
              for (Element bodyRow : bodyRows) {
                Elements columns = bodyRow.getElementsByTag("td");
                // save text in first column
                String firstColumnText = "";
                for (int j = 0; j < columns.size(); j++) {
                  // add code in first column
                  if (j == 0) {
                    String firstColumn = columns.get(j).text();
                    String firstColumnCode = firstColumn.substring(0, firstColumn.indexOf(" "));
                    if (Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(firstColumnCode))) {
                      firstColumnText =
                        firstColumn.substring(firstColumn.indexOf(" ") + 1).replace("++", "");
                    } else {
                      firstColumnText = firstColumn;
                    }
                      
                    if (!conceptMap.containsKey(firstColumnCode) && Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(firstColumnCode))) {
                      conceptMap.put(cleanCode(firstColumnCode), firstColumnText);
                    }
                    // process codes in second through n columns
                  } else {
                    // skip any cell with "---"
                    if (!columns.get(j).text().contentEquals("---")
                        && !conceptMap.containsKey(cleanCode(columns.get(j).text()))
                        && Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(columns.get(j).text()))) {
                        conceptMap.put(cleanCode(columns.get(j).text()),
                          firstColumnText + " " + headers.get(j - 1));                   
                    }
                  }
                }
              }
            }
          } 
        }
      }
    }
  }

  private boolean hasBracketImageTag(Elements elements) {
    for (Element el : elements) {
      Elements images = el.getElementsByTag("img");
      if (images.size() > 0 && images.get(0).attr("alt").startsWith("bracket")) {
        return true;
      }
    }
    return false;
  }
  
  private void processAttributes(Elements includes, String previousCode, String type)
    throws Exception {
    if (includes != null && includes.size() >= 1) {
      for (Element clude : includes) {
        String previousText = "";
        // note to notify user that attributes with html brackets are getting skipped
        if (hasBracketImageTag(includes)) {
          System.out.println("else skipped " + previousCode);
          conceptAttributeSet.add(cleanCode(previousCode) + "|Note|"
              + "Incomplete information, look in HTML or elsewhere for full information.");
          continue;
        }
        // skip embedded tables
        if (clude.select("table").size() > 0) {
          continue;
        }
        if (!clude.text().isEmpty()) {
          for (Node child : clude.childNodes()) {
            if (child instanceof Element) {
              String bullet1 = "";
              String bullet2 = "";
              String bullet3 = "";
              // process code blocks of listed bullets
              if (((Element) child).select("ul > li").size() > 0) {
                Elements children = ((Element) child).select("ul > li");
                for (int c = 0; c < children.size(); c++) {
                  Element bullet = children.get(c);
                  Element nextBullet = c + 1 < children.size() ? children.get(c + 1) : null;
                  if (bullet.parent().parent().parent().tagName().contentEquals("ul")) {
                    bullet3 = bullet.text().trim();
                    writeRelationship(previousCode,
                        previousText + (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2
                            + " " + bullet3);
                    conceptAttributeSet.add(cleanCode(previousCode) + "|" + type + "|"
                        + previousText + (previousText.isEmpty() ? "" : " ") + bullet1 + " "
                        + bullet2 + " " + bullet3);
                  } else if (bullet.parent().parent().tagName().contentEquals("ul")) {
                    bullet2 = bullet.text().trim();
                    writeRelationship(previousCode, previousText
                        + (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2);
                    // if next bullet isn't level 3, print
                    if (nextBullet == null
                        || !nextBullet.parent().parent().parent().tagName().contentEquals("ul")) {
                      conceptAttributeSet.add((cleanCode(previousCode) + "|" + type + "|"
                          + previousText + (previousText.isEmpty() ? "" : " ") + bullet1 + " "
                          + bullet2));
                    }
                  } else {
                    bullet1 = bullet.text().trim();
                    writeRelationship(previousCode,
                        previousText + (previousText.isEmpty() ? "" : " ") + bullet1);
                    // if next bullet isn't level 2, print
                    if (nextBullet == null
                        || !nextBullet.parent().parent().tagName().contentEquals("ul")) {
                      conceptAttributeSet.add((cleanCode(previousCode) + "|" + type + "|"
                          + previousText + (previousText.isEmpty() ? "" : " ") + bullet1));
                    }
                  }
                }
                previousText = "";
                // process line breaks
              } else if (((Element) child).tagName().contentEquals("br")) {
                writeRelationship(previousCode, previousText);
                conceptAttributeSet.add((cleanCode(previousCode) + "|" + type + "|" + previousText));
                previousText = "";
                // otherwise, append text and process next child
              } else {
                previousText =
                    (previousText + (previousText.isEmpty() ? "" : " ") + ((Element) child).text());
              }
              // if TextNode, append text and process next child
            } else if (child instanceof TextNode) {

              previousText = (previousText + (previousText.isEmpty() ? "" : " ")
                  + ((TextNode) child).getWholeText().trim());
            }
          }
          if (!previousText.isBlank()) {
            writeRelationship(previousCode, previousText);
            conceptAttributeSet.add(cleanCode(previousCode) + "|" + type + "|" + previousText);
          }
        }
      }
    }
  }

  /**
   * Write parent-child.txt file with parentId childId tuples.
   *
   * @param inputDirFile the input dir file
   * @param outputDirFile the output dir file
   * @throws Exception the exception
   */
  private void writeParentChild(File inputDirFile, File outputDirFile) throws Exception {
    File parentChildFile = new File(outputDirFile, "parent-child.txt");
    // if file doesn't exist, then create it
    if (!parentChildFile.exists()) {
      parentChildFile.createNewFile();
    }
    parentChildWriter = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(parentChildFile.getAbsolutePath()), StandardCharsets.UTF_8));

    // get all relevant .html files
    FilenameFilter projectFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        String lowercaseName = name.toLowerCase();
        if ( // remove index files
        !(lowercaseName.startsWith("6") && lowercaseName.length() >= 11)
            // keep only .html files
            && lowercaseName.endsWith(".html")
        // ignore conceptDetail files
            && !lowercaseName.startsWith("conceptdetail")
        // remove documentation file
            && !lowercaseName.contentEquals("371216.html")
        // remove tabulation index
            && !lowercaseName.contains("371324.html")
        // remove Appendix B
            && !lowercaseName.contains("371336.html")
        // remove Conventions file
            && !lowercaseName.contains("371198.html")
        // remove Morphology of Neoplasms
            && !lowercaseName.contains("352738.html")) {
          return true;
        } else {
          return false;
        }
      }
    };
    File[] projectFiles = inputDirFile.listFiles(projectFilter);

    // iterate through relevant files
    for (File child : projectFiles) {

      org.jsoup.nodes.Document doc = Jsoup.parse(child, null);
      if (doc.select("table").size() == 0) {
        continue;
      }

      Element table = doc.select("table").get(0); // select the first table.
      Elements rows = table.select("tr");

      // if index file, skip this file
      if (rows != null && rows.size() >= 3 && rows.get(2) != null
          && rows.get(2).select("td").get(0).text().length() == 1) {
        continue;
      }

      String chapterCode = "";
      String subChapterRange = "";
      Stack<String> prevStack = new Stack<>();

      // iterate through rows
      for (int i = 2; i < rows.size(); i++) {
        Element row = rows.get(i);
        Elements cols = row.select("td");
        Elements subChapterHeader = row.select("[class='bl1']");
        subChapterHeader.addAll(row.select("[class='bl2']"));
        subChapterHeader.addAll(row.select("[class='bl3']"));

        // Get chapter header concepts
        if (cols.size() >= 1 && cols.get(0).text().startsWith("Chapter")) {
          String colText = cols.get(0).text().substring(8);
          chapterCode = colText.substring(0, colText.indexOf("-") - 1);

          getLog().info(cols.get(0).text());
          parentChildSet.add("root" + "|" + chapterCode);
        }

        // Get sub-headers
        if (subChapterHeader != null && subChapterHeader.size() >= 1
            && Pattern.matches(".*[A-Z][0-9][0-9].*", subChapterHeader.get(0).text())) {
          getLog().info(subChapterHeader.get(0).text());
          subChapterRange = subChapterHeader.get(0).text().substring(
              subChapterHeader.get(0).text().lastIndexOf('(') + 1,
              subChapterHeader.get(0).text().lastIndexOf(')'));
          if (Pattern.matches("[A-Z][0-9][0-9]", subChapterRange)) {
            subChapterRange = subChapterRange + "-" + subChapterRange;
          }
          parentChildSet.add(chapterCode + "|" + cleanCode(subChapterRange));
          prevStack.clear();
          prevStack.push(cleanCode(subChapterRange));
        }

        else if (cols.size() >= 2 && !prevStack.isEmpty()) {
          // ensure no run-on codes (due to embedded tables)
          if (cols.get(0).hasText() && cols.get(1).hasText()
              && Pattern.matches("[A-Z][0-9][0-9].*", cols.get(0).text())
              && cols.get(0).text().length() < 10) {

            String currentCode = cleanCode(cols.get(0).text());

            if (prevStack.peek().contentEquals(subChapterRange)) {
              parentChildSet.add(prevStack.peek() + "|" + currentCode);
              prevStack.push(currentCode);
            } else if (currentCode.length() == prevStack.peek().length()) {
              prevStack.pop();
              parentChildSet.add(prevStack.peek() + "|" + currentCode);
              prevStack.push(currentCode);
            } else if (currentCode.length() < prevStack.peek().length()) {
              while (stripChars(currentCode).length() <= stripChars(prevStack.peek()).length()
                  && !Pattern.matches("[A-Z][0-9][0-9]-[A-Z][0-9][0-9]", prevStack.peek())) {
                prevStack.pop();
              }
              parentChildSet.add(prevStack.peek() + "|" + currentCode);
              prevStack.push(currentCode);
            } else if (currentCode.length() > prevStack.peek().length()) {
              parentChildSet.add(prevStack.peek() + "|" + currentCode);
              prevStack.push(currentCode);
            }
            // process embedded table
          } else if (cols.get(0).getElementsByTag("table").size() > 0) {
            if (ignoreEmbedded.contains(prevStack.peek())) {
              continue;
            }
            if (cols.get(0).getElementsByTag("thead").size() > 0
                && cols.get(0).getElementsByTag("tbody").size() > 0) {
              // process header row
              Element header = cols.get(0).getElementsByTag("thead").first();
              Elements headerElements = header.getElementsByTag("td");
              List<String> headers = new ArrayList<>();
              for (int j = 1; j < headerElements.size(); j++) {
                headers.add(headerElements.get(j).ownText());
              }
              // process table body rows
              Element body = cols.get(0).getElementsByTag("tbody").first();
              Elements bodyRows = body.getElementsByTag("tr");
              for (Element bodyRow : bodyRows) {
                Elements columns = bodyRow.getElementsByTag("td");
                // save text in first column
                String firstColumnCode = "";
                for (int j = 0; j < columns.size(); j++) {
                  // process code in first column
                  if (j == 0) {
                    String firstColumn = columns.get(j).text();
                    firstColumnCode = firstColumn.substring(0, firstColumn.indexOf(" "));
                    if (Pattern.matches("[A-Z][0-9][0-9].*", firstColumnCode)) {
                      parentChildSet.add(prevStack.peek() + "|" + firstColumnCode);
                    }
                    // process codes in second through n columns
                  } else {
                    // skip any cell with "---"
                    if (!columns.get(j).text().contentEquals("---")) {
                      if (Pattern.matches("[A-Z][0-9][0-9].*", firstColumnCode)) {
                        parentChildSet.add(firstColumnCode + "|" + cleanCode(columns.get(j).text()));
                      } else {
                        parentChildSet.add(prevStack.peek() + "|" + cleanCode(columns.get(j).text()));
                      }

                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void writeRelationship(String previousCode, String bullet) throws Exception {
    String label = bullet;
    if (bullet.contains("(")) {
      Matcher m = Pattern.compile("\\((\\s*)[A-Z][0-9][0-9](.*?)\\)").matcher(bullet);
      while (m.find()) {
        Set<String> targetIds = new HashSet<>();
        label = cleanCode(m.group(0).replace("(", "").replace(")", "")).trim();
        // if relationship is to a span of codes, select first code in the span
        if (label.endsWith(".-")) {
          targetIds.add(label.substring(0, label.indexOf(".")));
        } else if (label.contains("-")) {
          targetIds.add(label.substring(0, label.indexOf("-")));
        } else if (label.contains(",")) {
          targetIds.addAll(Set.copyOf(Arrays.asList(label.split(","))));
        } else {
          targetIds.add(label);
        }
        for (String code2 : targetIds) {
          // skip relationships to emergency use codes
          if (code2.startsWith("U")) {
            continue;
          }
          label = label.replaceAll(" ([,\\.\\-])","$1"); 
          if (previousCode.contains("*") && bullet.contains("†")) {
            conceptRelationshipSet.add(
                cleanCode(previousCode) + "|" + formatCode(code2) + "|Dagger to asterisk|" + label);
          } else if (previousCode.contains("†") && bullet.contains("*)")) {
            conceptRelationshipSet.add(
                cleanCode(previousCode) + "|" + formatCode(code2) + "|Asterisk to dagger|" + label);
          } else if (previousCode.contains("*") && bullet.contains("*)")) {
            conceptRelationshipSet.add(cleanCode(previousCode) + "|" + formatCode(code2)
                + "|Asterisk to asterisk|" + label);
          } else if (previousCode.contains("†") && bullet.contains("†")) {
            conceptRelationshipSet.add(
                cleanCode(previousCode) + "|" + formatCode(code2) + "|Dagger to dagger|" + label);
          } else {
            conceptRelationshipSet
                .add(cleanCode(previousCode) + "|" + formatCode(code2) + "|Reference|" + label);
          }
        }
      }
    }
  }

  private void writeConceptDetails(File inputDirFile) throws Exception {

      // get all relevant conceptDetail .html files
      FilenameFilter projectFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercaseName = name.toLowerCase();
          if (
              // conceptDetail files
              lowercaseName.startsWith("conceptdetail")) {
            return true;
          } else {
            return false;
          }
        }
      };
      File[] projectFiles = inputDirFile.listFiles(projectFilter);

      // iterate through relevant files
      for (File child : projectFiles) {

        org.jsoup.nodes.Document doc = Jsoup.parse(child, null);
        if (doc.select("table").size() == 0) {
          continue;
        }
        Elements elmts = doc.select("title");
        String title = elmts.get(0).text();
        System.out.println("concpt detail " + title);

        Element table = doc.select("table").get(0); // select the first table.
        Elements rows = table.select("tr");

        // for each row in the table
        for (int i = 2; i < rows.size(); i++) {
          Element row = rows.get(i);
          Elements cols = row.select("td");

          // write any attributes for the previousCode
          Elements includes = row.select("[class='include']");
          Elements excludes = row.select("[class='exclude']");
          Elements notes = row.select("[class='note']");
          Elements codealsos = row.select("[class='codealso']");
          
          if (!codesToIgnoreAttributes.contains(title)) {
            processAttributes(includes, title, "Inclusion");
            processAttributes(excludes, title, "Exclusion");
            processAttributes(notes, title, "Note");
            processAttributes(codealsos, title, "Coding hint");
          }
        }

      }
  }
  
  private String cleanCode(String input) {
    return input.replace("*", "").replace("†", "").replace("++", "");
  }

  private String stripChars(String input) {
    return input.replace(".", "").replace("*", "").replace("†", "");
  }

  private String formatCode(String code) {
    return code.trim().replaceFirst("^([A-Z][\\d\\.]+)(-[A-Z]\\d+)?.*", "$1$2").replaceFirst("\\.$",
        "");
  }

  // hard-coded superSpans for parent-child tweaking
  private Set<String> addSuperSpans(Set<String> superSpans) {
    superSpans.add("V01-X59|V01-V99");
    superSpans.add("V01-X59|W00-X59");

    superSpans.add("W00-X59|W00-W19");
    superSpans.add("W00-X59|W20-W49");
    superSpans.add("W00-X59|W50-W64");
    superSpans.add("W00-X59|W65-W74");
    superSpans.add("W00-X59|W75-W84");
    superSpans.add("W00-X59|W85-W99");
    superSpans.add("W00-X59|X00-X09");
    superSpans.add("W00-X59|X10-X19");
    superSpans.add("W00-X59|X20-X29");
    superSpans.add("W00-X59|X30-X39");
    superSpans.add("W00-X59|X40-X49");
    superSpans.add("W00-X59|X50-X57");
    superSpans.add("W00-X59|X58-X59");

    superSpans.add("V01-V99|V01-V09");
    superSpans.add("V01-V99|V10-V19");
    superSpans.add("V01-V99|V20-V29");
    superSpans.add("V01-V99|V30-V39");
    superSpans.add("V01-V99|V40-V49");
    superSpans.add("V01-V99|V50-V59");
    superSpans.add("V01-V99|V60-V69");
    superSpans.add("V01-V99|V70-V79");
    superSpans.add("V01-V99|V80-V89");
    superSpans.add("V01-V99|V90-V94");
    superSpans.add("V01-V99|V95-V97");
    superSpans.add("V01-V99|V98-V99");

    superSpans.add("C00-C97|C00-C75");
    superSpans.add("C00-C97|C76-C80");
    superSpans.add("C00-C97|C81-C96");
    superSpans.add("C00-C97|C97-C97");

    superSpans.add("C00-C75|C00-C14");
    superSpans.add("C00-C75|C15-C26");
    superSpans.add("C00-C75|C30-C39");
    superSpans.add("C00-C75|C40-C41");
    superSpans.add("C00-C75|C43-C44");
    superSpans.add("C00-C75|C45-C49");
    superSpans.add("C00-C75|C50-C50");
    superSpans.add("C00-C75|C51-C58");
    superSpans.add("C00-C75|C60-C63");
    superSpans.add("C00-C75|C64-C68");
    superSpans.add("C00-C75|C69-C72");
    superSpans.add("C00-C75|C73-C75");

    superSpans.add("Y40-Y84|Y40-Y59");
    superSpans.add("Y40-Y84|Y60-Y69");
    superSpans.add("Y40-Y84|Y70-Y82");
    superSpans.add("Y40-Y84|Y83-Y84");

    superSpans.add("M00-M25|M00-M03");
    superSpans.add("M00-M25|M05-M14");
    superSpans.add("M00-M25|M15-M19");
    superSpans.add("M00-M25|M20-M25");

    superSpans.add("M40-M54|M40-M43");
    superSpans.add("M40-M54|M45-M49");
    superSpans.add("M40-M54|M50-M54");

    superSpans.add("M60-M79|M60-M63");
    superSpans.add("M60-M79|M65-M68");
    superSpans.add("M60-M79|M70-M79");

    superSpans.add("M80-M94|M80-M85");
    superSpans.add("M80-M94|M86-M90");
    superSpans.add("M80-M94|M91-M94");

    superSpans.add("T20-T32|T20-T25");
    superSpans.add("T20-T32|T26-T28");
    superSpans.add("T20-T32|T29-T32");

    return superSpans;
  }
  
  private Set<String> addBurnConcepts(Set<String> burnConcepts) {
    burnConcepts.add("T31.0|Burns involving less than 10% of body surface");
    burnConcepts.add("T31.1|Burns involving 10-19% of body surface");
    burnConcepts.add("T31.10|Burns involving 10-19% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.11|Burns involving 10-19% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.2|Burns involving 20-29% of body surface");
    burnConcepts.add("T31.20|Burns involving 20-29% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.21|Burns involving 20-29% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.22|Burns involving 20-29% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.3|Burns involving 30-39% of body surface");
    burnConcepts.add("T31.30|Burns involving 30-39% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.31|Burns involving 30-39% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.32|Burns involving 30-39% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.33|Burns involving 30-39% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.4|Burns involving 40-49% of body surface");
    burnConcepts.add("T31.40|Burns involving 40-49% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.41|Burns involving 40-49% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.42|Burns involving 40-49% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.43|Burns involving 40-49% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.44|Burns involving 40-49% of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.5|Burns involving 50-59% of body surface");
    burnConcepts.add("T31.50|Burns involving 50-59% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.51|Burns involving 50-59% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.52|Burns involving 50-59% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.53|Burns involving 50-59% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.54|Burns involving 50-59% of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.55|Burns involving 50-59% of body surface with 50-59% third degree burns");
    burnConcepts.add("T31.6|Burns involving 60-69% of body surface");
    burnConcepts.add("T31.60|Burns involving 60-69% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.61|Burns involving 60-69% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.62|Burns involving 60-69% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.63|Burns involving 60-69% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.64|Burns involving 60-69% of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.65|Burns involving 60-69% of body surface with 50-59% third degree burns");
    burnConcepts.add("T31.66|Burns involving 60-69% of body surface with 60-69% third degree burns");
    burnConcepts.add("T31.7|Burns involving 70-79% of body surface");
    burnConcepts.add("T31.70|Burns involving 70-79% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.71|Burns involving 70-79% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.72|Burns involving 70-79% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.73|Burns involving 70-79% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.74|Burns involving 70-79% of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.75|Burns involving 70-79% of body surface with 50-59% third degree burns");
    burnConcepts.add("T31.76|Burns involving 70-79% of body surface with 60-69% third degree burns");
    burnConcepts.add("T31.77|Burns involving 70-79% of body surface with 70-79% third degree burns");
    burnConcepts.add("T31.8|Burns involving 80-89% of body surface");
    burnConcepts.add("T31.80|Burns involving 80-89% of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.81|Burns involving 80-89% of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.82|Burns involving 80-89% of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.83|Burns involving 80-89% of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.84|Burns involving 80-89% of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.85|Burns involving 80-89% of body surface with 50-59% third degree burns");
    burnConcepts.add("T31.86|Burns involving 80-89% of body surface with 60-69% third degree burns");
    burnConcepts.add("T31.87|Burns involving 80-89% of body surface with 70-79% third degree burns");
    burnConcepts.add("T31.88|Burns involving 80-89% of body surface with 80-89% third degree burns");
    burnConcepts.add("T31.9|Burns involving 90% or more of body surface");
    burnConcepts.add("T31.90|Burns involving 90% or more of body surface with 0% to 9% third degree burns");
    burnConcepts.add("T31.91|Burns involving 90% or more of body surface with 10-19% third degree burns");
    burnConcepts.add("T31.92|Burns involving 90% or more of body surface with 20-29% third degree burns");
    burnConcepts.add("T31.93|Burns involving 90% or more of body surface with 30-39% third degree burns");
    burnConcepts.add("T31.94|Burns involving 90% or more of body surface with 40-49% third degree burns");
    burnConcepts.add("T31.95|Burns involving 90% or more of body surface with 50-59% third degree burns");
    burnConcepts.add("T31.96|Burns involving 90% or more of body surface with 60-69% third degree burns");
    burnConcepts.add("T31.97|Burns involving 90% or more of body surface with 70-79% third degree burns");
    burnConcepts.add("T31.98|Burns involving 90% or more of body surface with 80-89% third degree burns");
    burnConcepts.add("T31.99|Burns involving 90% or more of body surface with 90% or more third degree burns");
    
    burnConcepts.add("T32.0|Corrosions involving less than 10% of body surface");
    burnConcepts.add("T32.1|Corrosions involving 10-19% of body surface");
    burnConcepts.add("T32.10|Corrosions involving 10-19% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.11|Corrosions involving 10-19% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.2|Corrosions involving 20-29% of body surface");
    burnConcepts.add("T32.20|Corrosions involving 20-29% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.21|Corrosions involving 20-29% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.22|Corrosions involving 20-29% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.3|Corrosions involving 30-39% of body surface");
    burnConcepts.add("T32.30|Corrosions involving 30-39% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.31|Corrosions involving 30-39% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.32|Corrosions involving 30-39% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.33|Corrosions involving 30-39% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.4|Corrosions involving 40-49% of body surface");
    burnConcepts.add("T32.40|Corrosions involving 40-49% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.41|Corrosions involving 40-49% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.42|Corrosions involving 40-49% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.43|Corrosions involving 40-49% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.44|Corrosions involving 40-49% of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.5|Corrosions involving 50-59% of body surface");
    burnConcepts.add("T32.50|Corrosions involving 50-59% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.51|Corrosions involving 50-59% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.52|Corrosions involving 50-59% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.53|Corrosions involving 50-59% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.54|Corrosions involving 50-59% of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.55|Corrosions involving 50-59% of body surface with 50-59% third degree corrosion");
    burnConcepts.add("T32.6|Corrosions involving 60-69% of body surface");
    burnConcepts.add("T32.60|Corrosions involving 60-69% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.61|Corrosions involving 60-69% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.62|Corrosions involving 60-69% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.63|Corrosions involving 60-69% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.64|Corrosions involving 60-69% of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.65|Corrosions involving 60-69% of body surface with 50-59% third degree corrosion");
    burnConcepts.add("T32.66|Corrosions involving 60-69% of body surface with 60-69% third degree corrosion");
    burnConcepts.add("T32.7|Corrosions involving 70-79% of body surface");
    burnConcepts.add("T32.70|Corrosions involving 70-79% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.71|Corrosions involving 70-79% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.72|Corrosions involving 70-79% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.73|Corrosions involving 70-79% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.74|Corrosions involving 70-79% of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.75|Corrosions involving 70-79% of body surface with 50-59% third degree corrosion");
    burnConcepts.add("T32.76|Corrosions involving 70-79% of body surface with 60-69% third degree corrosion");
    burnConcepts.add("T32.77|Corrosions involving 70-79% of body surface with 70-79% third degree corrosion");
    burnConcepts.add("T32.8|Corrosions involving 80-89% of body surface");
    burnConcepts.add("T32.80|Corrosions involving 80-89% of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.81|Corrosions involving 80-89% of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.82|Corrosions involving 80-89% of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.83|Corrosions involving 80-89% of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.84|Corrosions involving 80-89% of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.85|Corrosions involving 80-89% of body surface with 50-59% third degree corrosion");
    burnConcepts.add("T32.86|Corrosions involving 80-89% of body surface with 60-69% third degree corrosion");
    burnConcepts.add("T32.87|Corrosions involving 80-89% of body surface with 70-79% third degree corrosion");
    burnConcepts.add("T32.88|Corrosions involving 80-89% of body surface with 80-89% third degree corrosion");
    burnConcepts.add("T32.9|Corrosions involving 90% or more of body surface");
    burnConcepts.add("T32.90|Corrosions involving 90% or more of body surface with 0% to 9% third degree corrosion");
    burnConcepts.add("T32.91|Corrosions involving 90% or more of body surface with 10-19% third degree corrosion");
    burnConcepts.add("T32.92|Corrosions involving 90% or more of body surface with 20-29% third degree corrosion");
    burnConcepts.add("T32.93|Corrosions involving 90% or more of body surface with 30-39% third degree corrosion");
    burnConcepts.add("T32.94|Corrosions involving 90% or more of body surface with 40-49% third degree corrosion");
    burnConcepts.add("T32.95|Corrosions involving 90% or more of body surface with 50-59% third degree corrosion");
    burnConcepts.add("T32.96|Corrosions involving 90% or more of body surface with 60-69% third degree corrosion");
    burnConcepts.add("T32.97|Corrosions involving 90% or more of body surface with 70-79% third degree corrosion");
    burnConcepts.add("T32.98|Corrosions involving 90% or more of body surface with 80-89% third degree corrosion");
    burnConcepts.add("T32.99|Corrosions involving 90% or more of body surface with 90% or more third degree corrosion");
  
    return burnConcepts;
  }
  
  private String checkForSingletonTwin(Entry<String, String> entry) {
    if(entry.getKey().startsWith("Z38.0") && !entry.getKey().contentEquals("Z38.0")) {
      return "Singleton, born in hospital, " + entry.getValue();
    } else if (entry.getKey().startsWith("Z38.3") && !entry.getKey().contentEquals("Z38.3")) {
      return "Twin, born in hospital, " + entry.getValue();
    } else {
      return entry.getValue();
    }
  }
}