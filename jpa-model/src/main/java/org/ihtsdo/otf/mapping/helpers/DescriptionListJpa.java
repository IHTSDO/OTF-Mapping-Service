package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;

/**
 * JAXB enabled implementation of {@link DescriptionList}.
 */
@XmlRootElement(name = "descriptionList")
public class DescriptionListJpa extends AbstractResultList<Description>
    implements DescriptionList {

  /** The map descriptions. */
  private List<Description> descriptions = new ArrayList<>();

  /**
   * Instantiates a new map description list.
   */
  public DescriptionListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.DescriptionList#addDescription(org.ihtsdo
   * .otf.mapping .rf2.Description)
   */
  @Override
  public void addDescription(Description Description) {
    descriptions.add(Description);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.DescriptionList#removeDescription(org.ihtsdo
   * .otf .mapping.rf2.Description)
   */
  @Override
  public void removeDescription(Description Description) {
    descriptions.remove(Description);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.DescriptionList#setDescriptions(java.util
   * .List)
   */
  @Override
  public void setDescriptions(List<Description> Descriptions) {
    this.descriptions = new ArrayList<>();
    if (Descriptions != null) {
      this.descriptions.addAll(Descriptions);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.DescriptionList#getDescriptions()
   */
  @Override
  @XmlElement(type = DescriptionJpa.class, name = "description")
  public List<Description> getDescriptions() {
    return descriptions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return descriptions.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<Description> comparator) {
    Collections.sort(descriptions, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Description element) {
    return descriptions.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<Description> getIterable() {
    return descriptions;
  }

}
