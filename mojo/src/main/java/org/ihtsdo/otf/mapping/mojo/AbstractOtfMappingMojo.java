/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.text.MessageFormat;

import javax.xml.bind.annotation.XmlSchema;

import org.apache.maven.plugin.AbstractMojo;

/**
 * The Class AbstractMojo.
 */
public abstract class AbstractOtfMappingMojo extends AbstractMojo {
  
  protected void setupBindInfoPackage() {
    String nsuri = "http://www.hibernate.org/xsd/orm/hbm";
    String packageInfoClassName =
        "org.hibernate.boot.jaxb.hbm.spi.package-info";
    getLog().info("  running setup bind info package");

    try {
      final Class<?> packageInfoClass = Class.forName(packageInfoClassName);
      final XmlSchema xmlSchema =
          packageInfoClass.getAnnotation(XmlSchema.class);
      if (xmlSchema == null) {
        this.getLog().warn(MessageFormat.format(
            "Class [{0}] is missing the [{1}] annotation. Processing bindings will probably fail.",
            packageInfoClassName, XmlSchema.class.getName()));
      } else {
        final String namespace = xmlSchema.namespace();
        if (nsuri.equals(namespace)) {
          this.getLog().warn(MessageFormat.format(
              "Namespace of the [{0}] annotation does not match [{1}]. Processing bindings will probably fail.",
              XmlSchema.class.getName(), nsuri));
        }
      }
    } catch (ClassNotFoundException cnfex) {
      this.getLog().warn(MessageFormat.format(
          "Class [{0}] could not be found. Processing bindings will probably fail.",
          packageInfoClassName), cnfex);
    }
  }
}