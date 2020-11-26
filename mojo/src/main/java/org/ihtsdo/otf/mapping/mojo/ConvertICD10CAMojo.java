/*
 *    Copyright 2020 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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

  private BufferedReader projectReader = null;
  private BufferedWriter conceptWriter = null;
  private BufferedWriter parentChildWriter = null;
	
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

			writeConcepts(inputDirFile, outputDirFile);
			writeParentChild(inputDirFile, outputDirFile);


			getLog().info("done ...");

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Converting of icd10ca html files failed.", e);
		} finally {
			try {
				projectReader.close();
				conceptWriter.close();
				parentChildWriter.close();
			} catch (IOException e1) {
				// do nothing
			}
		}

	}
  
  	/**
	   * Write each ICD10CA conceptId and term to the concepts.txt file.
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
		conceptWriter = new BufferedWriter(new FileWriter(conceptsFile.getAbsoluteFile()));

		// get all relevant icd10ca .html files
		FilenameFilter projectFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (    // remove index files
						!(lowercaseName.startsWith("6") && lowercaseName.length() >= 11)
						// keep only .html files
						&& lowercaseName.endsWith(".html")
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
		for (File child : projectFiles) {
			projectReader = new BufferedReader(new FileReader(child));

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

			// for each row in the table
			for (int i = 2; i < rows.size(); i++) { 
				Element row = rows.get(i);
				Elements cols = row.select("td");

				// Get chapter header concepts
				if (cols.size() >=1 && cols.get(0).text().startsWith("Chapter")) {
					String colText = cols.get(0).text();
					colText = colText.substring(8);
					String code = colText.substring(0, colText.indexOf("-") -1);
					int stop = colText.indexOf("(") != -1 ? colText.indexOf("(") -1 : colText.length();
					String term = colText.substring(colText.indexOf("-") + 2, stop);
					if (!conceptMap.containsKey(code)) {
					  conceptWriter.write(code + "\t" + term + "\n");
					  conceptMap.put(code, term);
					}
				} 
				// if not chapter header
				else if (cols.size() >= 2) {
					// ensure no duplicates
					// ensure no run-on codes (due to embedded tables)
					if (cols.get(0).hasText() && cols.get(1).hasText() && !conceptMap.containsKey(cleanCode(cols.get(0).text()))
							&& Pattern.matches("[A-Z][0-9][0-9].*", cleanCode(cols.get(0).text())) && cols.get(0).text().length() < 10) {
						
						conceptWriter.write(cleanCode(cols.get(0).text()) + "\t" + cols.get(1).text() + "\n");
						conceptMap.put(cleanCode(cols.get(0).text()), cols.get(1).text());
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
		parentChildWriter = new BufferedWriter(new FileWriter(parentChildFile.getAbsoluteFile()));

		// get all relevant .html files
		FilenameFilter projectFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (    // remove index files
						!(lowercaseName.startsWith("6") && lowercaseName.length() >= 11)
						// keep only .html files
						&& lowercaseName.endsWith(".html")
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
			projectReader = new BufferedReader(new FileReader(child));

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
					parentChildWriter.write("root" + "\t" + chapterCode + "\n");
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
					parentChildWriter.write(chapterCode + "\t" + cleanCode(subChapterRange) + "\n");
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
							parentChildWriter.write(prevStack.peek() + "\t" + currentCode + "\n");
							prevStack.push(currentCode);
					    } else if (currentCode.length() == prevStack.peek().length()) {
							prevStack.pop();
							parentChildWriter.write(prevStack.peek() + "\t" + currentCode + "\n");
							prevStack.push(currentCode);
						} else if (currentCode.length() < prevStack.peek().length()){
							while(stripChars(currentCode).length() <= stripChars(prevStack.peek()).length() &&
									!Pattern.matches("[A-Z][0-9][0-9]-[A-Z][0-9][0-9]", prevStack.peek())) {
								prevStack.pop();
							}							
							parentChildWriter.write(prevStack.peek() + "\t" + currentCode + "\n");
							prevStack.push(currentCode);
						} else if (currentCode.length() > prevStack.peek().length()) {
							parentChildWriter.write(prevStack.peek() + "\t" + currentCode + "\n");
							prevStack.push(currentCode);
						}					
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