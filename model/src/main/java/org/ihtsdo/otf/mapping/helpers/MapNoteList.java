package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapNote;

/**
 * Represents a sortable list of {@link MapNote}.
 */
public interface MapNoteList extends ResultList<MapNote> {

  /**
   * Adds the map note.
   * 
   * @param mapNote the map note
   */
  public void addMapNote(MapNote mapNote);

  /**
   * Removes the map note.
   * 
   * @param mapNote the map note
   */
  public void removeMapNote(MapNote mapNote);

  /**
   * Sets the map notes.
   * 
   * @param mapNotes the new map notes
   */
  public void setMapNotes(List<MapNote> mapNotes);

  /**
   * Gets the map notes.
   * 
   * @return the map notes
   */
  public List<MapNote> getMapNotes();

}
