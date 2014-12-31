package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts XML index file to HTML pages.
 * 
 * See admin/indexes/pom.xml for a sample execution.
 * 
 * @goal convert-to-html
 */
public class IndexXmlToHtmlMojo extends AbstractMojo {

  /**
   * The input XML file.
   * @parameter
   */
  private String inputDir;

  /**
   * The input XML file.
   * @parameter
   */
  private String inputFile;

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
   * The domain.
   * 
   * @parameter
   */
  String domain;

  /** The output dir. */
  String outputDir;

  /**
   * The document title (top-level <title> tag)
   */
  String documentTitle;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting to convert index XML to HTML");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  inputFile = " + inputFile);
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  domain = " + domain);

    // Use XML parser to go through extracted XML files
    // and produce HTLM files

    try {

      // set the input directory
      String baseDir = inputDir + "/" + terminology + "/" + terminologyVersion;
      inputDir = baseDir + "/xml";
      outputDir = baseDir + "/html/" + domain;
      getLog().info("  outputDir: " + outputDir);

      // Create path to outputDir
      new File(outputDir).mkdirs();

      // Setup SAX parser
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      DefaultHandler handler = new LocalHandler();

      // Open file and begin parsing
      File file = new File(inputDir, inputFile);
      if (!file.exists()) {
        throw new MojoFailureException("Specified input file does not exist");
      }

      InputStream inputStream = checkForUtf8BOM(new FileInputStream(file));
      Reader reader = new InputStreamReader(inputStream, "UTF-8");
      InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);
      reader.close();
      getLog().info("Done...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Conversion of XML to HTML failed", e);
    }

  }

  /**
   * Check for utf8 bom.
   * 
   * @param inputStream the input stream
   * @return the input stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static InputStream checkForUtf8BOM(InputStream inputStream)
    throws IOException {

    // Open input stream and read past BOM if it exists.
    PushbackInputStream pushbackInputStream =
        new PushbackInputStream(new BufferedInputStream(inputStream), 3);
    byte[] bom = new byte[3];
    if (pushbackInputStream.read(bom) != -1) {
      if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
        pushbackInputStream.unread(bom);
      }
    }
    return pushbackInputStream;
  }

  /**
   * The SAX parser handler
   */
  class LocalHandler extends DefaultHandler {

    /** The output stream. */
    PrintWriter out = null;

    /** The chars. */
    StringBuilder chars = null;

    /** The title chars. */
    StringBuilder titleChars = new StringBuilder();

    /** The in tag flag. */
    Map<String, Integer> inTag = new HashMap<>();

    /** The data. */
    Map<String, String> data = new HashMap<>();

    /** The term level. */
    int level = -1;

    /** The current letter. */
    String letter = null;

    /** The act. */
    int act = 0;

    /** The aname. */
    String aname = null;

    /** Indicates if there is a nemod embedded in the title */
    boolean nemodInTitle = false;

    /** Indicates if currently processing "title" tag */
    boolean inTitle = false;

    /** Indicates whether processing is inside a table or not */
    private boolean inTable = false;

    /** column count */
    private int colCt = 0;

    /** cell count */
    private int cellCt = 0;

    /** the current cell index */
    private int cellIndex = 0;

    /** header string buffer */
    private StringBuilder headerHtml = null;

    /**
     * Instantiates a new local handler.
     */
    public LocalHandler() {
      super();
      // prevent null pointer exception
      inTag.put("mainterm", 0);
      inTag.put("letter", 0);
      inTag.put("term", 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) {

      // Track whether we are in title flag
      if (qName.equalsIgnoreCase("title")) {
        inTitle = true;
      }

      // Handle embedded nemod within title
      if (qName.equalsIgnoreCase("nemod") && inTitle) {
        titleChars = chars;
        nemodInTitle = true;
      }

      // reset "chars" buffer
      chars = new StringBuilder();

      // Track current tag hierarchy
      if (inTag.containsKey(qName.toLowerCase()))
        inTag.put(qName.toLowerCase(), inTag.get(qName.toLowerCase()) + 1);
      else
        inTag.put(qName.toLowerCase(), 1);

      // Set level attribute for current term
      if (qName.equalsIgnoreCase("term")) {
        level = Integer.parseInt(attributes.getValue("level"));
      }

      // Increment aname counter for new term
      if (qName.equalsIgnoreCase("mainterm") || qName.equalsIgnoreCase("term")) {
        act++;
      }

      // Determine current cell index (if doing table rendering)
      if (qName.equalsIgnoreCase("cell") || qName.equalsIgnoreCase("head"))
        cellIndex = Integer.parseInt(attributes.getValue("col"));

      // Handle missing end of table columns if starting a new term
      if (inTable
          && (qName.equalsIgnoreCase("term") || qName
              .equalsIgnoreCase("mainTerm"))) {
        if (cellCt == 1) {
          int x = colCt - 1;
          out.println("</td><td colspan=" + x + ">&nbsp;");
        } else {
          while (cellCt < colCt) {
            out.println("</td><td>&nbsp;");
            cellCt++;
          }
        }
      }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {

      // Handle title characters, including nemod case
      if (qName.equalsIgnoreCase("title") && nemodInTitle) {
        data.put("afterNemodTitleChars", chars.toString());
        chars = titleChars;
        titleChars = new StringBuilder();
      }

      // Put character data into map
      data.put(qName.toLowerCase(), chars.toString().replaceAll("<", "&lt;")
          .replaceAll(">", "&gt;"));

      // If top-level title tag, set documentTitle
      if (qName.equalsIgnoreCase("title") && inTag.get("letter") == 0) {
        documentTitle = data.get("title");
      }

      // Handle end of letter tag
      if (qName.equalsIgnoreCase("letter"))
        endHtml();

      // Handle note
      if (qName.equalsIgnoreCase("note"))
        writeNote(data.get("note"));

      // If inLetter but not inMainTerm and a title tag is ending
      // => start a new document
      if (qName.equalsIgnoreCase("title") && inTag.get("letter") > 0
          && inTag.get("mainterm") == 0) {
        endHtml();

        try {
          out =
              new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                  new File(outputDir, data.get("title") + ".html")), "UTF-8"));
          startHtml();
        } catch (UnsupportedEncodingException e) {
          throw new SAXException(e);
        } catch (FileNotFoundException e) {
          throw new SAXException(e);
        }
      }

      // Handle writing header
      if (qName.equalsIgnoreCase("head")) {
        // If we're inside a letter tag, this is an embedded table
        // and we should write the header immediately
        if (inTag.get("letter") > 0) {
          if (!inTable)
            startTableHtml();
          writeHeaderColumn();
        }

        // otherwise, we should save the header and write it when we start the
        // document (and each subsequent document)
        else {
          if (headerHtml == null) {
            headerHtml = new StringBuilder();
            headerHtml.append(getTableHtml());
            headerHtml.append("\n");
          }
          headerHtml.append(getHeaderColumn());
          headerHtml.append("\n");
        }
      }

      // Handle writing end of table
      if (qName.equalsIgnoreCase("endTable")) {
        if (headerHtml != null)
          throw new SAXException(
              "Unexpected data condition: endTable tag with indexHeadings outside of letter tags");
        if (inTable)
          endTableHtml();
        else
          throw new SAXException(
              "Unexpected data condition: endTable without indexHeading tags");
      }

      // If inMainTerm but not inTerm and a title tag is ending
      // => write the anchor and the entry for the main term.
      if ((qName.equalsIgnoreCase("title") || qName.equalsIgnoreCase("code")
          || qName.equalsIgnoreCase("manif")
          || qName.equalsIgnoreCase("etiology")
          || qName.equalsIgnoreCase("nemod")
          || qName.equalsIgnoreCase("subcat") || qName.equalsIgnoreCase("see")
          || qName.equalsIgnoreCase("seeAlso") || qName
            .equalsIgnoreCase("cell"))
          && inTag.get("mainterm") > 0
          && inTag.get("term") == 0) {
        if (inTable)
          writeMainTermTableInfo(qName.toLowerCase());
        else {
          if (inTable)
            endTableHtml();
          writeMainTermInfo(qName.toLowerCase());
        }
      }

      // If inTerm write term info
      if ((qName.equalsIgnoreCase("title") || qName.equalsIgnoreCase("code")
          || qName.equalsIgnoreCase("manif")
          || qName.equalsIgnoreCase("etiology")
          || qName.equalsIgnoreCase("subcat")
          || qName.equalsIgnoreCase("nemod") || qName.equalsIgnoreCase("see")
          || qName.equalsIgnoreCase("seeAlso") || qName
            .equalsIgnoreCase("cell"))
          && inTag.get("mainterm") > 0
          && inTag.get("term") > 0) {
        if (inTable)
          writeTermTableInfo(qName.toLowerCase());
        else {
          if (inTable)
            endTableHtml();
          writeTermInfo(qName.toLowerCase());
        }
      }

      // at </term> write closing indentation
      // and strip last '.' part off of aname.
      if (qName.equalsIgnoreCase("term")) {
        aname = aname.substring(0, aname.lastIndexOf('.'));
      }

      // Indicate tag end
      inTag.put(qName.toLowerCase(), inTag.get(qName.toLowerCase()) - 1);

      // Remove contents from tracking
      if (qName.equalsIgnoreCase("cell") || qName.equalsIgnoreCase("head")
          || qName.equalsIgnoreCase("code") || qName.equalsIgnoreCase("manif")
          || qName.equalsIgnoreCase("etiology")
          || qName.equalsIgnoreCase("see") || qName.equalsIgnoreCase("subcat")
          || qName.equalsIgnoreCase("seeAlso")
          || (qName.equalsIgnoreCase("nemod") && !nemodInTitle))
        data.remove(qName.toLowerCase());

      // Clear characters buffer
      chars = new StringBuilder();

      // Set inTitle flag
      if (qName.toLowerCase().equals("title") && inTitle)
        inTitle = false;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char ch[], int start, int length) {
      chars.append(new String(ch, start, length));
    }

    /**
     * Write note info. Italicize the first part of the note and split on
     * sentences in a variety of ways to make long paragraph notes more
     * readable.
     */
    private void writeNote(String note) {
      String styledNote = note.replaceAll("Note:", "<i>Note:</i>");
      // Make multiple lines where appropriate
      styledNote =
          styledNote.replaceAll("([^\\.]*\\.)(\\s+\\d)", "$1<br><br>$2");
      styledNote =
          styledNote.replaceAll("([^\\.]+\\.\\s+[^\\.]+\\.)(\\s+)",
              "$1<br><br>");
      out.println("");
      if (inTable)
        out.println("</td></tr><tr><td colspan=\"" + colCt + "\">"
            + "<center><div style=\"text-align: left; width: 95%\">"
            + styledNote + "</div></center>");
      else
        out.println("</p><p><center><div style=\"text-align: left; width: 95%\">"
            + styledNote + "</div></center></p><p>");
    }

    /**
     * Write main term info.
     * 
     * @param key the key
     */
    private void writeMainTermInfo(String key) {
      // Handle title
      if (key.equals("title")) {
        // Set the aname pointer and write it
        aname = letter + act;
        out.println("</p><p><div id=\"" + aname + "\"></div>");
        out.println("<b>" + data.get(key) + "</b>");

        // Handle writing the title, including embedded nemod
        if (nemodInTitle && data.get("nemod") != null) {
          out.println("<b>" + data.get("nemod") + "</b>");
          data.remove("nemod");
          nemodInTitle = false;
          if (data.containsKey("afterNemodTitleChars")) {
            out.println("<b>" + data.get("afterNemodTitleChars") + "</b>");
            data.remove("afterNemodTitleChars");
          }
        }
      }

      // Handle writing the "see" tag
      else if (key.equals("see")) {
        out.println(" &mdash; <i>see</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      }

      // handle writing seealso tag
      else if (key.equals("seealso") && data.get(key).startsWith("subcategory")) {
        out.println(" &mdash; <i>see also</i> " + data.get(key));
      } else if (key.equals("seealso")) {
        out.println(" &mdash; <i>see also</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      }

      // handle writing subcategory
      else if (key.equals("subcat")) {
        out.println(" &mdash; see subcategory " + data.get(key));
      }

      // handle writing etiology tag
      else if (key.equals("etiology")) {
        out.println("[" + data.get(key) + "]");
      }

      // do nothing if encountering a nemod tag within the title
      else if (inTitle && key.equals("nemod")) {
        // print after title

      } else if (key.equals("code")) {
        out.println(" " + data.get(key) + "");
      } else if (key.equals("manif")) {
        out.println(" [" + data.get(key) + "]");
      } else {
        out.println(" " + data.get(key) + "");
      }
    }

    /**
     * Write main term table info.
     * 
     * @param key the key
     */
    private void writeMainTermTableInfo(String key) {
      // Handle the title tag
      if (key.equals("title")) {
        // Reset cell counter and aname pointer
        cellCt = 1;
        cellIndex = 1;
        aname = letter + act;

        // write anchor and title
        out.println("</td></tr><tr><td><div id=\"" + aname + "\"></div>");
        out.println(data.get(key));

        // handle embedded nemod in title
        if (nemodInTitle && data.get("nemod") != null) {
          out.println(data.get("nemod"));
          data.remove("nemod");
          nemodInTitle = false;
        }

      }

      // handle see tag
      else if (key.equals("see")) {
        out.println(" &mdash; <i>see</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      }

      // handle seealso tag
      else if (key.equals("seealso")) {
        out.println(" &mdash; <i>see also</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      }

      // handle cell tag - print blank cells if columns are skipped
      else if (key.equals("cell")) {
        while (++cellCt < cellIndex) {
          out.println("</td><td>&nbsp;");
        }
        String val = data.get(key);
        if (val.equals(""))
          val = "&nbsp;";
        out.println("</td><td>" + val);
      }

      // Ignore nemod if within title
      else if (inTitle && key.equals("nemod")) {
        // print with title

      } else if (key.equals("code")) {
        out.println(" " + data.get(key) + "");
      } else if (key.equals("manif")) {
        out.println(" [" + data.get(key) + "]");
      } else {
        out.println(" " + data.get(key) + "");
      }
    }

    /**
     * Write term info.
     * 
     * @param key the key
     */
    private void writeTermInfo(String key) {
      if (key.equals("title")) {
        aname = aname + "." + act;
        out.println("</p><p>");
        out.println("<div id=\"" + aname + "\"></div>");
        out.println("- - - - - - - - - - - ".substring(0, level * 2)
            + data.get("title"));
        // out.println("<h" + hLevel + ">" + data.get(key) + "</h" +
        // hLevel + ">");
        if (nemodInTitle && data.get("nemod") != null) {
          out.println(data.get("nemod"));
          data.remove("nemod");
          nemodInTitle = false;
          if (data.containsKey("afterNemodTitleChars")) {
            out.println(data.get("afterNemodTitleChars"));
            data.remove("afterNemodTitleChars");
          }
        }
      } else if (key.equals("see")) {
        out.println(" &mdash; <i>see</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      } else if (key.equals("seealso")
          && data.get(key).startsWith("subcategory")) {
        out.println(" &mdash; <i>see also</i> " + data.get(key));
      } else if (key.equals("seealso")) {
        out.println(" &mdash; <i>see also</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      } else if (key.equals("subcat")) {
        out.println(" &mdash; see subcategory " + data.get(key));
      } else if (key.equals("etiology")) {
        out.println("[" + data.get(key) + "]");
      } else if (inTitle && key.equals("nemod")) {
        // print with title
        // write the data associated with this tag.
      } else if (key.equals("code")) {
        out.println(" " + data.get(key) + "");
      } else if (key.equals("manif")) {
        out.println(" [" + data.get(key) + "]");
      } else {
        out.println(" " + data.get(key) + "");
      }
    }

    /**
     * Write term table info.
     * 
     * @param key the key
     */
    private void writeTermTableInfo(String key) {
      if (key.equals("title")) {
        cellCt = 1;
        aname = aname + "." + act;
        out.println("</td></tr><tr><td>");
        out.println("<div id=\"" + aname + "\"></div>");
        out.println("- - - - - - - - - - - ".substring(0, level * 2)
            + data.get("title"));

        if (nemodInTitle && data.get("nemod") != null) {
          out.println(data.get("nemod"));
          data.remove("nemod");
          nemodInTitle = false;
        }
      } else if (key.equals("see")) {
        out.println(" &mdash; <i>see</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      } else if (key.equals("seealso")) {
        out.println(" &mdash; <i>see also</i> <a href=\"\" ng-click=\"performSearchFromLink('"
            + data.get(key) + "')\"" + ">" + data.get(key) + "</a>");
      } else if (key.equals("cell")) {
        while (++cellCt < cellIndex) {
          out.println("</td><td>&nbsp;");
        }
        String val = data.get(key);
        if (val.equals(""))
          val = "&nbsp;";
        out.println("</td><td>" + val);
      } else if (inTitle && key.equals("nemod")) {
        // print with title
      } else if (key.equals("code")) {
        out.println(" " + data.get(key) + "");
      } else if (key.equals("manif")) {
        out.println(" [" + data.get(key) + "]");
      } else {
        out.println(" " + data.get(key) + "");
      }
    }

    /**
     * Write header info.
     * 
     * @param key the key
     */
    private void writeHeaderColumn() {
      // Write entry for "head" tag.
      out.println(getHeaderColumn());
    }

    /**
     * Returns the header info.
     * 
     * @return the header info
     */
    private String getHeaderColumn() {
      StringBuilder sb = new StringBuilder();
      // Write empty fields if columns are missing
      while (++cellCt < cellIndex) {
        if (colCt++ > 0)
          sb.append("</td><td>");
        sb.append("&nbsp;");
      }
      // Write entry for "head" tag.
      if (colCt++ > 0)
        sb.append("</td><td>");
      sb.append("<b>" + data.get("head") + "</b>");
      return sb.toString();
    }

    /**
     * Start table html.
     */
    private void startTableHtml() {
      out.println(getTableHtml());
    }

    /**
     * Returns table html.
     */
    private String getTableHtml() {
      inTable = true;
      colCt = 0;
      return "<table border=1><tr><td>";
    }

    /**
     * end table html.
     */
    private void endTableHtml() {
      out.println("</td></tr></table>");
      inTable = false;
    }

    /**
     * Start html document.
     */
    private void startHtml() {
      out.println("<html><head><style type=\"text/css\">p { margin-top:0 ;margin-bottom:0;}</style></head><body style=\"font-family: sans-serif;\">");
      out.println("<center><h3>" + documentTitle
          + "</h3><div style=\"width: 100%; background-color: #BBBBBB;\">"
          + data.get("title") + "</div></center><p>");
      // handle the case where header columns
      // were encountered outside "letter" tags
      if (headerHtml != null)
        out.println(headerHtml.toString());
      letter = data.get("title");
    }

    /**
     * End html document.
     */
    public void endHtml() {
      if (out != null) {
        if (inTable)
          out.println("</td></tr></table>");
        out.println("</p></body></html>");
        out.close();
      }
    }

  }

}
