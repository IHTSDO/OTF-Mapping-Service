package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Serializer;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Converts ICD10 ASC file to ICD10CM-style XML.
 * 
 * See admin/indexes/pom.xml for a sample execution.
 * 
 * @goal convert-to-xml
 */
public class AscToXmlMojo extends AbstractMojo {

  /**
   * The input ASC file.
   * 
   * @parameter
   */
  private String inputFile;

  /**
   * The index viewer data direcotry.
   *
   * @parameter
   * @required
   */
  private String inputDir;

  /**
   * The terminology.
   * 
   * @parameter
   */
  String terminology;

  /**
   * The terminology version.
   * 
   * @parameter
   */
  String terminologyVersion;

  /**
   * The document title.
   * 
   * @parameter
   */
  String documentTitle;

  /**
   * Tracks header columns for table-sytle data.
   * 
   * @parameter
   */
  Properties headerProperties;

  /** the pending note value. */
  private String pendingNote = null;

  /** Column count. */
  private int colCt = 0;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting conversion of ASC to XML");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  inputFile = " + inputFile);
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  documentTitle = " + documentTitle);
    getLog().info("  headerProperties = " + headerProperties);

    BufferedReader reader = null;
    String outputDir = "";

    try {

      // set the input directory
      String baseDir = inputDir + "/" + terminology + "/" + terminologyVersion;
      inputDir = baseDir + "/asc";
      outputDir = baseDir + "/xml";
      getLog().info("  inputDir: " + inputDir);
      getLog().info("  outputDir: " + outputDir);

      // Open the input ASC file
      File file = new File(inputDir, inputFile);

      if (!file.exists()) {
        throw new MojoFailureException(
            "Specified input directory does not exist: " + file);
      }

      // Start the DOM root
      Element root = new Element("ICD10.index");

      // Add version and title tags
      Element version = new Element("version");
      version.appendChild("0");
      root.appendChild(version);
      Element title = new Element("title");
      title.appendChild(documentTitle);
      root.appendChild(title);

      // Set parent to root
      Element parent = root;
      // Read through file and process each record
      reader =
          new BufferedReader(new InputStreamReader(new FileInputStream(file),
              "UTF-8"));
      String line = reader.readLine();
      StringBuilder lastLine = new StringBuilder();
      Pattern pattern = Pattern.compile("^[A-Z]+");
      boolean inTable = false;
      int lineCt = 1;
      do {
        // If lines start with captial letter, we've started a new record
        // Otherwise, keep appending data until we've got a complete record
        // need to support line == null
        Matcher m = pattern.matcher(line == null ? "" : line);
        boolean startsWithCapitalLetter = m.find();

        // Identify condition where the file starts with a table
        // These are cases where the entire data is a table and
        // index headers should be written immediately
        if (startsWithCapitalLetter && lineCt == 1 && line != null
            && headerProperties.containsKey(line.substring(0, 1))) {
          // Write the header before the other parts of the tag (but after
          // parent is computed)
          writeHeadersToXml(parent,
              headerProperties.getProperty(line.substring(0, 1)));
          inTable = true;
          lastLine = new StringBuilder(line);
        }

        // Handle the condition where we have a complete record
        else if (startsWithCapitalLetter && lastLine.length() > 0) {
          // Determine if this is a table entry
          if (headerProperties.containsKey(lastLine.substring(0, 1))) {
            // write table entry. if not yet in a table, write headers
            parent =
                writeTableEntryToXml(root, parent, lastLine.toString(),
                    !inTable);

            // If there is a pending note and inTable isn't set yet
            // wait one more time to set it, so the note can be written inside
            // the index headers
            if (pendingNote == null)
              inTable = true;

          } else {
            // Normal entry, if we are in a table, end the table
            if (inTable) {
              // Indicate end of table
              inTable = false;
              writeTableEndToXml(parent);
            }
            parent = writeToXml(root, parent, lastLine.toString());
          }
          lastLine = new StringBuilder(line);

        }

        // Handle the multi-line record case
        else if (line != null) {
          lastLine.append(line.replaceAll("\n", ""));
        }

        // Bail once the last line is processed
        else {
          break;
        }

        // Read another line
        line = reader.readLine();
        lineCt++;

      } while (true);

      // Create a document from the generated element structure
      Document document = new Document(root);

      // Create output directories and write file
      new File(outputDir).mkdirs();
      FileOutputStream outputStream =
          new FileOutputStream(new File(outputDir, "ICD10_" + file.getName()
              + ".xml"));
      Serializer serializer = new Serializer(outputStream, "UTF-8");
      serializer.setIndent(4);
      serializer.write(document);
      outputStream.close();
      reader.close();

      getLog().info("Done...");
    } catch (Exception e) {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      e.printStackTrace();
      throw new MojoExecutionException("Conversion of ASC to XML failed", e);
    }

  }

  /**
   * Create XML dom structure for this entry in the index file.
   *
   * @param root the root
   * @param initialParent the initial parent
   * @param lastLine the last line
   * @return the element
   * @throws Exception the exception
   */
  public Element writeToXml(Element root, Element initialParent, String lastLine)
    throws Exception {

    Logger.getLogger(getClass()).info(lastLine);
    // Setup
    Element parent = initialParent;
    Pattern notEmpty = Pattern.compile("\\S+");
    Pattern upperCase = Pattern.compile("([A-Z])");
    String name = lastLine.substring(38);
    String fields = lastLine.substring(0, 37);
    String[] codes = fields.substring(0, 16).split("\\s+");
    Element term = new Element("term");
    Element title = new Element("title");

    // Verify the expectations regarding first character of each entry
    // This applies to TEIL1.ASC and TEIL2.ASC files if the neoplasm entries
    // have been removed.
    if (!codes[0].startsWith("K") && !codes[0].startsWith("U")) {
      throw new IOException("Unexpected data found in " + inputFile + ", "
          + codes[0]);
    }

    // Get level code
    Character level = codes[0].charAt(1);

    // Handle Notes
    if (level == 'H') {
      pendingNote = lastLine.substring(lastLine.indexOf("Note:"));
      return parent;
    }

    // Handle hierarchical term
    else if (Character.isDigit(level)) {
      // Get level
      int context = Character.getNumericValue(level);

      // If 0 level, handle new mainTerm
      if (context == 0) {

        // first, double-check whether current letter has changed
        Element letter = null;
        Matcher m = upperCase.matcher(name);
        if (m.find()) {
          String index = name.substring(m.start(), m.end());
          Elements letters = root.getChildElements();
          for (int i = 0; i < letters.size(); i++) {
            if (letters.get(i).getLocalName().equals("letter")
                && letters.get(i).getChild(0).getChild(0).getValue()
                    .equals(index))
              letter = letters.get(i);
          }
          // If letter has changed, add a new entry to the DOM (under root)
          if (letter == null) {
            letter = new Element("letter");
            title = new Element("title");
            title.appendChild(index);
            letter.appendChild(title);
            root.appendChild(letter);
          }
        } else {
          throw new Exception(
              "Entry unexpectedly does not have a first captial letter");
        }
        // Add new mainTerm to the current letter and set as the parent
        Element mainTerm = new Element("mainTerm");
        letter.appendChild(mainTerm);
        term = mainTerm;
        parent = mainTerm;
      }

      // Handle the normal term case
      else {

        // Iterate to correct parent based on level setting
        while (parent.getAttribute("level") != null
            && context <= Character.getNumericValue(parent
                .getAttribute("level").getValue().charAt(0)))
          parent = (Element) parent.getParent();
        // Create term element, add to parent
        term = new Element("term");
        term.addAttribute(new Attribute("level", String.valueOf(level)));
        parent.appendChild(term);
        // Set parent to term if it is nested term
        if (parent.getAttribute("level") == null
            || context > Character.getNumericValue(parent.getAttribute("level")
                .getValue().charAt(0)))
          parent = term;
      }

      // Handle notes
      if (pendingNote != null) {
        Element note = new Element("note");
        note.appendChild(pendingNote);
        parent.appendChild(note);
        pendingNote = null;
      }

      // Create title and add to term
      title = new Element("title");
      term.appendChild(title);
      // Add see also tag, if data is present
      if (name.indexOf("see also") != -1) {
        Element seeAlso = new Element("seeAlso");
        seeAlso.appendChild(name.substring(name.indexOf("see also") + 9, (name
            .indexOf(')', name.indexOf("see also") + 9) == -1 ? name.length()
            : name.indexOf(')', name.indexOf("see also") + 9))));
        term.appendChild(seeAlso);
        name = name.substring(0, name.indexOf("see also") - 2);
      }
      // Add see tag, if data is present
      else if (name.indexOf("- see ") != -1 && name.indexOf("see also") == -1
          && name.indexOf('(') == -1) {
        Element see = new Element("see");
        see.appendChild(name.substring(name.indexOf("- see ") + 6, (name
            .indexOf(')', name.indexOf("- see ") + 6) == -1 ? name.length()
            : name.indexOf(')', name.indexOf("- see ") + 6))));
        term.appendChild(see);
        name =
            name.substring(0, name.indexOf("- see ") - 1)
                + (name.indexOf(')', name.indexOf("- see ")) != -1 ? name
                    .substring(name.lastIndexOf(')')) : "");
      }
      // Handle nemod
      if (name.indexOf(" (") != -1) {
        Element nemod = new Element("nemod");
        nemod.appendChild(name.substring(name.indexOf(" (") + 1,
            name.lastIndexOf(')') + 1));
        term.appendChild(nemod);
        name =
            name.substring(0, name.indexOf(" ("))
                + (name.lastIndexOf(')') == name.length() - 1 ? "" : name
                    .substring(name.lastIndexOf(')') + 1));
      }
      // Add code if present (perform XML entity conversions)
      Element code = new Element("code");
      String value = codes[0].substring(2);
      if (!value.isEmpty()) {
        value = value.replace("+", "&#8224;");
        value = value.replace("*", "&#42;");
        code.appendChild(value);
        term.appendChild(code);
      }

      if (codes.length > 1)
        for (int i = 1; i < codes.length; i++) {
          code = new Element("code");
          value = codes[i];
          value = value.replace("+", "&#8224;");
          value = value.replace("*", "&#42;");
          code.appendChild(value);
          term.appendChild(code);
        }
      // Handle "- code each site"
      if (lastLine.indexOf("code each site") != -1) {
        value = " - code each site";
        code.appendChild(value);
        term.appendChild(code);
      }

      String nemod = fields.substring(16, 23);
      Matcher m = notEmpty.matcher(nemod);
      if (m.matches()) {
        code = new Element("code");
        code.appendChild(nemod);
        term.appendChild(code);
      }
      name = name.replaceAll("\\- ", "");
      title.appendChild(name);

    }

    return parent;
  }

  /**
   * Write XML for table headers.
   *
   * @param parent the parent
   * @param headers the header list
   */
  private void writeHeadersToXml(Element parent, String headers) {
    Element indexHeading = new Element("indexHeading");
    parent.appendChild(indexHeading);
    int i = 0;
    for (String header : headers.split(";")) {
      Element head = new Element("head");
      head.addAttribute(new Attribute("col", String.valueOf(++i)));
      indexHeading.appendChild(head);
      head.appendChild(header);
    }
    colCt = i;
  }

  /**
   * Write XML for table end.
   *
   * @param parent the parent element
   */
  @SuppressWarnings("static-method")
  private void writeTableEndToXml(Element parent) {
    Element indexHeading = new Element("endTable");
    parent.appendChild(indexHeading);
  }

  /**
   * Write a table entry to XML.
   *
   * @param root the root element
   * @param initParent the init parent
   * @param lastLine the last line
   * @param writeHeader the write header
   * @return the element
   * @throws Exception the exception
   */
  public Element writeTableEntryToXml(Element root, Element initParent,
    String lastLine, boolean writeHeader) throws Exception {
    Element parent = initParent;
    Pattern notEmpty = Pattern.compile("\\S+");
    Pattern upperCase = Pattern.compile("([A-Z])");
    String name = lastLine.substring(38);
    String fields = lastLine.substring(0, 37);
    // Process fixed-length data
    String codeFields =
        fields.replaceAll("(.........)(.......)(.......)(.......)(.......)",
            "$1;$2;$3;$4;$5");
    String[] codes = codeFields.split(";");
    for (int i = 0; i < codes.length; i++) {
      codes[i] = codes[i].replaceAll(" ", "");
    }
    Element term = new Element("term");
    Element title = new Element("title");
    Character level = codes[0].charAt(1);

    // Notes apply to the NEXT line, so save the value here
    if (level == 'H') {
      pendingNote = lastLine.substring(lastLine.indexOf("Note:"));
      return parent;
    } else if (Character.isDigit(level)) {
      int context = Character.getNumericValue(level);
      if (context == 0) {
        Element letter = null;
        Matcher m = upperCase.matcher(name);
        if (m.find()) {
          String index = name.substring(m.start(), m.end());
          Elements letters = root.getChildElements();
          for (int i = 0; i < letters.size(); i++) {
            if (letters.get(i).getLocalName().equals("letter")
                && letters.get(i).getChild(0).getChild(0).getValue()
                    .equals(index))
              letter = letters.get(i);
          }
          if (letter == null) {
            letter = new Element("letter");
            title = new Element("title");
            title.appendChild(index);
            letter.appendChild(title);
            root.appendChild(letter);
          }

        } else {
          throw new Exception(
              "Entry unexpectedly does not have a first captial letter");
        }
        Element mainTerm = new Element("mainTerm");
        letter.appendChild(mainTerm);
        term = mainTerm;
        parent = mainTerm;
      } else {
        while (parent.getAttribute("level") != null
            && context <= Character.getNumericValue(parent
                .getAttribute("level").getValue().charAt(0)))
          parent = (Element) parent.getParent();
        term = new Element("term");
        term.addAttribute(new Attribute("level", String.valueOf(level)));
        parent.appendChild(term);
        if (parent.getAttribute("level") == null
            || context > Character.getNumericValue(parent.getAttribute("level")
                .getValue().charAt(0)))
          parent = term;
      }
    }

    // Write the header before the other parts of the tag (but after parent is
    // computed)
    if (writeHeader) {
      writeHeadersToXml(parent,
          headerProperties.getProperty(String.valueOf(codes[0].charAt(0))));
    }

    // Handle notes
    if (pendingNote != null) {
      Element note = new Element("note");
      note.appendChild(pendingNote);
      parent.appendChild(note);
      pendingNote = null;
    }

    title = new Element("title");
    term.appendChild(title);
    if (name.indexOf("see also") != -1) {
      Element seeAlso = new Element("seeAlso");
      seeAlso.appendChild(name.substring(name.indexOf("see also") + 9, (name
          .indexOf(')', name.indexOf("see also") + 9) == -1 ? name.length()
          : name.indexOf(')', name.indexOf("see also") + 9))));
      term.appendChild(seeAlso);
      name = name.substring(0, name.indexOf("see also") - 2);
    } else if (name.indexOf("- see ") != -1 && name.indexOf("see also") == -1
        && name.indexOf('(') == -1) {
      Element see = new Element("see");
      see.appendChild(name.substring(name.indexOf("- see ") + 6,
          (name.indexOf(')', name.indexOf("- see ") + 6) == -1 ? name.length()
              : name.indexOf(')', name.indexOf("- see ") + 6))));
      term.appendChild(see);
      name =
          name.substring(0, name.indexOf("- see ") - 1)
              + (name.indexOf(')', name.indexOf("- see ")) != -1 ? name
                  .substring(name.lastIndexOf(')')) : "");
    }
    if (name.indexOf(" (") != -1) {
      Element nemod = new Element("nemod");
      nemod.appendChild(name.substring(name.indexOf(" (") + 1,
          name.lastIndexOf(')') + 1));
      term.appendChild(nemod);
      name =
          name.substring(0, name.indexOf(" ("))
              + (name.lastIndexOf(')') == name.length() - 1 ? "" : name
                  .substring(name.lastIndexOf(')') + 1));
    }
    Element cell = new Element("cell");
    // codes[] indexes 0-n correspond to columns 2-(n-2)
    // i starts at zero cause codes
    for (int i = 0; i <= colCt - 2; i++) {
      cell = new Element("cell");
      cell.addAttribute(new Attribute("col", String.valueOf(i + 2)));
      String value = (i >= codes.length) ? "" : codes[i];
      // codes[0] contains the hierarchical chars, strip them
      if (i == 0)
        value = value.substring(2);
      cell.appendChild(value);
      term.appendChild(cell);
    }
    String nemod = fields.substring(16, 23);
    Matcher m = notEmpty.matcher(nemod);
    if (m.matches()) {
      Element code = new Element("code");
      code.appendChild(nemod);
      term.appendChild(code);
    }
    name = name.replaceAll("\\- ", "");
    title.appendChild(name);

    return parent;
  }
}
