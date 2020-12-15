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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
  
  private Set<String> simpleRefsetMemberSet = new HashSet<>();
	
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
			
			writeConcepts(inputDirFile, outputDirFile);
			writeParentChild(inputDirFile, outputDirFile);
			
			for (String member : simpleRefsetMemberSet) {
				simpleRefsetMemberWriter.write(member + "\n");
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
	   * Write each ICD10CA conceptId and term to the concepts.txt file.
	   * Also calls processing for the concept-attributes.txt and concept-relationships.txt files.
	   *
	   * @param inputDirFile the input dir file
	   * @param outputDirFile the output dir file
	   * @throws Exception the exception
	   */
	  private void writeConcepts(File inputDirFile, File outputDirFile)  throws Exception {
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
				if (    // remove index files
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
						// remove Morphology of Neoplasms
						&& !lowercaseName.contains("352738.html")) {
					return true;
				} else {
					return false;
				}
			}
		};
		File[] projectFiles = inputDirFile.listFiles(projectFilter);
		Map<String, String> conceptMap = new HashMap<>();
		
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
			if (rows != null && rows.size() >=3 && rows.get(2) != null && 
					rows.get(2).select("td").get(0).text().length() == 1) {
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
				if (cols.size() >=1 && cols.get(0).text().startsWith("Chapter")) {
					String colText = cols.get(0).text();
					colText = colText.substring(8);
					String code = colText.substring(0, colText.indexOf("-") -1);
					int stop = colText.indexOf("(") != -1 ? colText.indexOf("(") -1 : colText.length();
					String term = colText.substring(colText.indexOf("-") + 2, stop);
					if (!conceptMap.containsKey(code)) {
					  conceptWriter.write(code + "|" + term + "\n");
					  conceptMap.put(code, term);
					  previousCode = code;
					}
				} 
				
				// if not chapter header
				else if (cols.size() >= 2) {
					// ensure no duplicates
					// ensure no run-on codes (due to embedded tables)
					if (cols.get(0).hasText() && cols.get(1).hasText()
							&& Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(cols.get(0).text())) && cols.get(0).text().length() < 10) {
						if (cols.get(0).text().endsWith("*")) {
							  simpleRefsetMemberSet.add(cleanCode(cols.get(0).text()) + "|Asterisk refset");
							  writeRelationship(cols.get(0).text(), cols.get(1).text());
						  } else if (cols.get(0).text().endsWith("†")) {
							  simpleRefsetMemberSet.add(cleanCode(cols.get(0).text()) + "|Dagger refset");
							  writeRelationship(cols.get(0).text(), cols.get(1).text());
						  }			
						if (!conceptMap.containsKey(cleanCode(cols.get(0).text()))) {
						  conceptWriter.write(cleanCode(cols.get(0).text()) + "|" + cols.get(1).text() + "\n");
						  conceptMap.put(cleanCode(cols.get(0).text()), cols.get(1).text());
						}
						previousCode = cols.get(0).text();
					} else if (cols.get(0).hasText() && cols.get(0).className().contentEquals("bl1")) {
						previousCode = cleanCode(cols.get(0).text()).substring(cleanCode(cols.get(0).text()).lastIndexOf("(") + 1,
					    		cleanCode(cols.get(0).text()).lastIndexOf(")"));	
					}

					  
				}
			}
		}
	}

	private void processAttributes(Elements includes, String previousCode, String type) throws Exception {
		if (includes != null && includes.size() >= 1) {
			for (Element clude : includes) {
				String previousText = "";
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
										writeRelationship(previousCode, previousText
												+ (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2 + " "
												+ bullet3);
										conceptAttributeWriter.write(cleanCode(previousCode) + "|" + type + "|" + previousText
												+ (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2 + " "
												+ bullet3 + "\n");
									} else if (bullet.parent().parent().tagName().contentEquals("ul")) {
										bullet2 = bullet.text().trim();
										writeRelationship(previousCode, previousText
												+ (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2);
										// if next bullet isn't level 3, print
										if (nextBullet == null || !nextBullet.parent().parent().parent().tagName()
												.contentEquals("ul")) {
											conceptAttributeWriter.write((cleanCode(previousCode) + "|" + type + "|" + previousText
													+ (previousText.isEmpty() ? "" : " ") + bullet1 + " " + bullet2
													+ "\n"));
										}
									} else {
										bullet1 = bullet.text().trim();
										writeRelationship(previousCode, previousText
												+ (previousText.isEmpty() ? "" : " ") + bullet1);
										// if next bullet isn't level 2, print
										if (nextBullet == null
												|| !nextBullet.parent().parent().tagName().contentEquals("ul")) {
											conceptAttributeWriter.write((cleanCode(previousCode) + "|" + type + "|" + previousText
													+ (previousText.isEmpty() ? "" : " ") + bullet1 + "\n"));
										}
									}
								}
								previousText = "";
							// process line breaks
							} else if (((Element) child).tagName().contentEquals("br")) {
								writeRelationship(previousCode, previousText);
								conceptAttributeWriter.write((cleanCode(previousCode) + "|" + type + "|" + previousText + "\n"));
								previousText = "";
							// otherwise, append text and process next child
							} else {
								previousText = (previousText + (previousText.isEmpty() ? "" : " ")
										+ ((Element) child).text());
							}
						// if TextNode, append text and process next child
						} else if (child instanceof TextNode) {
							
							previousText = (previousText + (previousText.isEmpty() ? "" : " ")
									+  ((TextNode) child).getWholeText().trim());
						}
					}
					if (!previousText.isBlank()) {
						writeRelationship(previousCode, previousText);
						conceptAttributeWriter.write(cleanCode(previousCode) + "|" + type + "|" + previousText + "\n");
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
	  private void writeParentChild(File inputDirFile, File outputDirFile)  throws Exception {
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
				if (    // remove index files
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
			if (rows != null && rows.size() >=3 && rows.get(2) != null && 
					rows.get(2).select("td").get(0).text().length() == 1) {
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
				if (cols.size() >=1 && cols.get(0).text().startsWith("Chapter")) {
					String colText = cols.get(0).text().substring(8);
					chapterCode = colText.substring(0, colText.indexOf("-") -1);

					getLog().info(cols.get(0).text());
					parentChildWriter.write("root" + "|" + chapterCode + "\n");
				}
				
				// Get sub-headers
				if (subChapterHeader != null && subChapterHeader.size() >= 1 && 
						Pattern.matches(".*[A-Z][0-9][0-9].*", subChapterHeader.get(0).text())) {
					getLog().info(subChapterHeader.get(0).text());
					subChapterRange = subChapterHeader.get(0).text().substring(
						subChapterHeader.get(0).text().lastIndexOf('(') + 1, subChapterHeader.get(0).text().lastIndexOf(')'));
					if (Pattern.matches("[A-Z][0-9][0-9]", subChapterRange)) {
						subChapterRange = subChapterRange + "-" + subChapterRange;
					}
					parentChildWriter.write(chapterCode + "|" + cleanCode(subChapterRange) + "\n");
					prevStack.clear();
					prevStack.push(cleanCode(subChapterRange));
				}

				else if (cols.size() >= 2 && !prevStack.isEmpty()) {
					// ensure no run-on codes (due to embedded tables)				
					if (cols.get(0).hasText() && cols.get(1).hasText() 
							&& Pattern.matches("[A-Z][0-9][0-9].*", cols.get(0).text()) 
							&& cols.get(0).text().length() < 10 ) {
				
						String currentCode = cleanCode(cols.get(0).text());
						
						if (prevStack.peek().contentEquals(subChapterRange) ) {
							parentChildWriter.write(prevStack.peek() + "|" + currentCode + "\n");
							prevStack.push(currentCode);
					    } else if (currentCode.length() == prevStack.peek().length()) {
							prevStack.pop();
							parentChildWriter.write(prevStack.peek() + "|" + currentCode + "\n");
							prevStack.push(currentCode);
						} else if (currentCode.length() < prevStack.peek().length()){
							while(stripChars(currentCode).length() <= stripChars(prevStack.peek()).length() &&
									!Pattern.matches("[A-Z][0-9][0-9]-[A-Z][0-9][0-9]", prevStack.peek())) {
								prevStack.pop();
							}							
							parentChildWriter.write(prevStack.peek() + "|" + currentCode + "\n");
							prevStack.push(currentCode);
						} else if (currentCode.length() > prevStack.peek().length()) {
							parentChildWriter.write(prevStack.peek() + "|" + currentCode + "\n");
							prevStack.push(currentCode);
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
					// asterisk to dagger
					if (previousCode.contains("*") && bullet.contains("†")) {
						relationshipWriter.write(cleanCode(previousCode) + "|" + code2.trim() + "|Dagger to asterisk|" + label + "\n");
					// dagger to asterisk
					} else if (previousCode.contains("†") && bullet.contains("*)")) {
						relationshipWriter.write(cleanCode(previousCode) + "|" + code2.trim() + "|Asterisk to dagger|" + label + "\n");
					} else if (previousCode.contains("*") && bullet.contains("*)")) {
						relationshipWriter.write(cleanCode(previousCode) + "|" + code2.trim() + "|Asterisk to asterisk|" + label + "\n");
					} else if (previousCode.contains("†") && bullet.contains("†")) {
						relationshipWriter.write(cleanCode(previousCode) + "|" + code2.trim() + "|Dagger to dagger|" + label + "\n");
					} else {
						relationshipWriter.write(cleanCode(previousCode) + "|" + code2.trim() + "|Reference|" + label + "\n");
					}
				}
			}
		} 	
	}
	
  	private String cleanCode(String input) {
  		return input.replace("*", "").replace("†", "");
  	}
  	
  	private String stripChars(String input) {
  		return input.replace(".", "").replace("*", "").replace("†", "");
  	}
  	
}