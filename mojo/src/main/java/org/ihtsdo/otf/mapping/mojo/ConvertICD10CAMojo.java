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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
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

  //private BufferedReader projectReader = null;
  private BufferedWriter conceptWriter = null;
  private BufferedWriter parentChildWriter = null;
  private BufferedWriter conceptAttributeWriter = null;
	
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
				//projectReader.close();
				conceptWriter.close();
				conceptAttributeWriter.close();
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
		
		File attributesFile = new File(outputDirFile, "concept-attributes.txt");
		// if file doesn't exist, then create it
		if (!attributesFile.exists()) {
			attributesFile.createNewFile();
		}
		conceptAttributeWriter = new BufferedWriter(new FileWriter(attributesFile.getAbsoluteFile()));
		conceptAttributeWriter.write("exclude\n");
		conceptAttributeWriter.write("include\n");
		conceptAttributeWriter.write("note\n");

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
		for (File child : projectFiles) {
			
			//File modifiedChild = new File(child.getAbsoluteFile() + ".update");
			//Files.copy(child.toPath(), modifiedChild.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
			
			modifyFile(child.getAbsolutePath(), "<br/>", "bbrr");
			modifyFile(child.getAbsolutePath(), "</ul>", "</ul>bbrr");
			modifyFile(child.getAbsolutePath(), "<ul", "uul-<ul");
			modifyFile(child.getAbsolutePath(), "</ul>", "</ul>-llu"); 


			org.jsoup.nodes.Document doc = Jsoup.parse(child, null);
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
				Elements includes = row.select("[class='include']");
				Elements excludes = row.select("[class='exclude']");
				Elements notes = row.select("[class='note']");
				

				if (includes != null && includes.size() >= 1) {
					for (Element clude : includes) {
						if (!clude.text().isEmpty()) {
							String newString = clude.text().replaceAll("uul-.+-llu", "");
							for (String str : newString.split("bbrr")) {
								if (!previousCode.isEmpty() && !str.isEmpty()) {
								  conceptAttributeWriter.write(previousCode + "|include|" + str +  "\n");
								}
							}

						}
					}
				}
				if (excludes != null && excludes.size() >= 1) {
					for (Element clude : excludes) {
						if (!clude.text().isEmpty()) {
							String newString = clude.text().replaceAll("uul-.+-llu", "");
							for (String str : newString.split("bbrr")) {
								if (!previousCode.isEmpty() && !str.isEmpty()) {
									conceptAttributeWriter.write(previousCode + "|exclude|" + str +  "\n");
								}
							}
						}
					}
				}
				if (notes != null && notes.size() >= 1) {
					for (Element note : notes) {
						if (!note.text().isEmpty()) {
							String newString = note.text().replaceAll("uul-.+-llu", "");
							for (String str : newString.split("bbrr")) {
								if (!previousCode.isEmpty() && !str.isEmpty()) {
									conceptAttributeWriter.write(previousCode + "|note|" + str +  "\n");
								}
							}
						}
					}
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
						
						if (!conceptMap.containsKey(cleanCode(cols.get(0).text()))) {
						  conceptWriter.write(cleanCode(cols.get(0).text()) + "|" + cols.get(1).text() + "\n");
						  conceptMap.put(cleanCode(cols.get(0).text()), cols.get(1).text());
						}
						previousCode = cleanCode(cols.get(0).text());
					}
				}
			}
		}
  	}

	  static void modifyFile(String filePath, String oldString, String newString)
	    {
	        File fileToBeModified = new File(filePath);
	         
	        String oldContent = "";
	         
	        BufferedReader reader = null;
	         
	        FileWriter writer = null;
	         
	        try
	        {
	        	if(!fileToBeModified.canRead())
	        		fileToBeModified.setReadable(true);
	        	
	        	if(!fileToBeModified.canWrite())
	        		fileToBeModified.setWritable(true);
	        	
	            reader = new BufferedReader(new FileReader(fileToBeModified));
	             
	            //Reading all the lines of input text file into oldContent
	             
	            String line = reader.readLine();
	             
	            while (line != null) 
	            {
	                oldContent = oldContent + line + System.lineSeparator();
	                 
	                line = reader.readLine();
	            }
	             
	            //Replacing oldString with newString in the oldContent
	             
	            String newContent = oldContent.replaceAll(oldString, newString);
	             
	            //Rewriting the input text file with newContent
	             
	            writer = new FileWriter(fileToBeModified);
	             
	            writer.write(newContent);
	        }
	        catch (IOException e)
	        {
	            e.printStackTrace();
	        }
	        finally
	        {
	            try
	            {
	                //Closing the resources
	                 
	                reader.close();
	                 
	                writer.close();
	            } 
	            catch (IOException e) 
	            {
	                e.printStackTrace();
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
			//projectReader = new BufferedReader(new FileReader(child));

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
  	
  	private String cleanCode(String input) {
  		return input.replace("*", "").replace("†", "");
  	}
  	
  	private String stripChars(String input) {
  		return input.replace(".", "").replace("*", "").replace("†", "");
  	}
  	
}