package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;

/**
 * JAXB enabled implementation of {@link ComplexMapRefSetMemberList}.
 */
@XmlRootElement(name = "complexMapRefSetMemberList")
public class ComplexMapRefSetMemberListJpa extends
    AbstractResultList<ComplexMapRefSetMember> implements
    ComplexMapRefSetMemberList {

  /** The map complexMapRefSetMembers. */
  private List<ComplexMapRefSetMember> complexMapRefSetMembers =
      new ArrayList<>();

  /**
   * Instantiates a new map complexMapRefSetMember list.
   */
  public ComplexMapRefSetMemberListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList#
   * addComplexMapRefSetMember(org.ihtsdo.otf.mapping
   * .rf2.ComplexMapRefSetMember)
   */
  @Override
  public void addComplexMapRefSetMember(
    ComplexMapRefSetMember ComplexMapRefSetMember) {
    complexMapRefSetMembers.add(ComplexMapRefSetMember);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList#
   * removeComplexMapRefSetMember(org.ihtsdo.otf
   * .mapping.rf2.ComplexMapRefSetMember)
   */
  @Override
  public void removeComplexMapRefSetMember(
    ComplexMapRefSetMember ComplexMapRefSetMember) {
    complexMapRefSetMembers.remove(ComplexMapRefSetMember);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList#
   * setComplexMapRefSetMembers(java.util.List)
   */
  @Override
  public void setComplexMapRefSetMembers(
    List<ComplexMapRefSetMember> ComplexMapRefSetMembers) {
    this.complexMapRefSetMembers = new ArrayList<>();
    if (ComplexMapRefSetMembers != null) {
      this.complexMapRefSetMembers.addAll(ComplexMapRefSetMembers);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList#
   * getComplexMapRefSetMembers()
   */
  @Override
  @XmlElement(type = ComplexMapRefSetMemberJpa.class, name = "complexMapRefSetMember")
  public List<ComplexMapRefSetMember> getComplexMapRefSetMembers() {
    return complexMapRefSetMembers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return complexMapRefSetMembers.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<ComplexMapRefSetMember> comparator) {
    Collections.sort(complexMapRefSetMembers, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(ComplexMapRefSetMember element) {
    return complexMapRefSetMembers.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  public Iterable<ComplexMapRefSetMember> getIterable() {
    return complexMapRefSetMembers;
  }

}
