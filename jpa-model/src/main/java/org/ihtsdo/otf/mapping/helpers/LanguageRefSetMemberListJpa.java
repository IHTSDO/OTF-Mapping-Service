package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.LanguageRefSetMemberJpa;


/**
 * JAXB enabled implementation of {@link LanguageRefSetMemberList}.
 */
@XmlRootElement(name = "languageRefSetMemberList")
public class LanguageRefSetMemberListJpa extends
    AbstractResultList<LanguageRefSetMember> implements
    LanguageRefSetMemberList {

  /** The map languageRefSetMembers. */
  private List<LanguageRefSetMember> languageRefSetMembers = new ArrayList<>();

  /**
   * Instantiates a new map languageRefSetMember list.
   */
  public LanguageRefSetMemberListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList#addLanguageRefSetMember
   * (org.ihtsdo.otf.mapping .rf2.LanguageRefSetMember)
   */
  @Override
  public void addLanguageRefSetMember(LanguageRefSetMember LanguageRefSetMember) {
    languageRefSetMembers.add(LanguageRefSetMember);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList#
   * removeLanguageRefSetMember(org.ihtsdo.otf
   * .mapping.rf2.LanguageRefSetMember)
   */
  @Override
  public void removeLanguageRefSetMember(
    LanguageRefSetMember LanguageRefSetMember) {
    languageRefSetMembers.remove(LanguageRefSetMember);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList#
   * setLanguageRefSetMembers(java.util.List)
   */
  @Override
  public void setLanguageRefSetMembers(
    List<LanguageRefSetMember> LanguageRefSetMembers) {
    this.languageRefSetMembers = new ArrayList<>();
    if (LanguageRefSetMembers != null) {
      this.languageRefSetMembers.addAll(LanguageRefSetMembers);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList#
   * getLanguageRefSetMembers()
   */
  @Override
  @XmlElement(type = LanguageRefSetMemberJpa.class, name = "languageRefSetMember")
  public List<LanguageRefSetMember> getLanguageRefSetMembers() {
    return languageRefSetMembers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return languageRefSetMembers.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<LanguageRefSetMember> comparator) {
    Collections.sort(languageRefSetMembers, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(LanguageRefSetMember element) {
    return languageRefSetMembers.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<LanguageRefSetMember> getIterable() {
    return languageRefSetMembers;
  }

}
