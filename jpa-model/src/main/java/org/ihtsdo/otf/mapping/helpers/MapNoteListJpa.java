package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.model.MapNote;

/**
 * JAXB enabled implementation of {@link MapNoteList}
 */
@XmlRootElement(name = "mapNoteList")
public class MapNoteListJpa extends AbstractResultList<MapNote> implements
    MapNoteList {

  /** The map notes. */
  private List<MapNote> mapNotes = new ArrayList<>();

  /**
   * Instantiates a new map note list.
   */
  public MapNoteListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapNoteList#addMapNote(org.ihtsdo.otf.mapping
   * .model.MapNote)
   */
  @Override
  public void addMapNote(MapNote mapNote) {
    mapNotes.add(mapNote);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapNoteList#removeMapNote(org.ihtsdo.otf
   * .mapping.model.MapNote)
   */
  @Override
  public void removeMapNote(MapNote mapNote) {
    mapNotes.remove(mapNote);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapNoteList#setMapNotes(java.util.List)
   */
  @Override
  public void setMapNotes(List<MapNote> mapNotes) {
    this.mapNotes = new ArrayList<>();
    this.mapNotes.addAll(mapNotes);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapNoteList#getMapNotes()
   */
  @Override
  @XmlElement(type = MapNoteJpa.class, name = "mapNote")
  public List<MapNote> getMapNotes() {
    return mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapNotes.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapNote> comparator) {
    Collections.sort(mapNotes, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapNote element) {
    return mapNotes.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapNote> getIterable() {
    return mapNotes;
  }
}
