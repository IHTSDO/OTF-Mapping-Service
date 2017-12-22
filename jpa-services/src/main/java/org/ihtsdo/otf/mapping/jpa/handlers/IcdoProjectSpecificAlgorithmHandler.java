package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * GMDN project specific algorithm handler.
 */
public class IcdoProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    if (!terminologyId.contains("/")) {
      return false;
    }

    final ContentService contentService = new ContentServiceJpa();
    try {
      // behavior code may be different and not in the source
      // so we just check that the base code is in the source
      SearchResultList list = contentService.findConceptsForQuery(
          "terminologyId:" + terminologyId.substring(0,4) + "* AND terminology:ICDO",
          null);

      return list.getSearchResults().size() > 0;

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }
  
  /* see superclass */
  @Override
  public boolean isMapRecordLineValid(String line) throws Exception {
    
    // Keep only morphology codes (with '/')
    if (!line.contains("/")) {
      return false;
    }
    else{
      return true;
    }
  }  

  /* see superclass */
  @Override
  public List<TreePosition> limitTreePositions(
    List<TreePosition> treePositions) {
    // If the tree structure has more than say 100 positions, just return the
    // top one from each root
    List<TreePosition> result = new ArrayList<TreePosition>();
    if (countTreePositions(treePositions) > 100) {
      for (final TreePosition treePosition : treePositions) {
        stripTreePosition(treePosition);
        result.add(treePosition);
      }
    } else {
      return treePositions;
    }

    return result;
  }

  /**
   * Count tree positions.
   *
   * @param treePositions the tps
   * @return the int
   */
  private int countTreePositions(List<TreePosition> treePositions) {
    int i = 0;
    for (final TreePosition treePosition : treePositions) {
      i++;
      i += countTreePositions(treePosition.getChildren());
    }

    return i;

  }

  /**
   * Strip tree position.
   *
   * @param treePosition the tree position
   */
  private void stripTreePosition(TreePosition treePosition) {
    if (treePosition.getChildren().size() > 1) {
      final TreePosition firstChild = treePosition.getChildren().get(0);
      treePosition.getChildren().clear();
      treePosition.getChildren().add(firstChild);
    }
    for (final TreePosition child : treePosition.getChildren()) {
      stripTreePosition(child);
    }
  }

}
