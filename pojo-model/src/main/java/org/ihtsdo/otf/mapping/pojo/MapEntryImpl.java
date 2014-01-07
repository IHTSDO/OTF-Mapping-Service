package org.ihtsdo.otf.mapping.pojo;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Reference implementation of {@link MapEntry}.
 * Includes hibernate tags for MEME database.
 *
 * @author ${author}
 */
public class MapEntryImpl implements MapEntry {

	/** The id. */
	private Long id;

	/** The map notes. */
	private Set<MapNote> mapNotes;

	/** The target. */
	private String target;

	/** The map advices. */
	private Set<MapAdvice> mapAdvices;
	
	/** The map principles. */
	private Set<MapPrinciple> mapPrinciples;

	/** The rule. */
	private String rule;

	/** The index. */
	private int indexMapPriority;

	/** The relation id. */
	private String relationId;
	
	/** The map record. */
	private MapRecord mapRecord;
	
	/** The map group */
	private int mapGroup;
	
	/** The map block */
	private int mapBlock;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getNotes()
	 */
	@Override
	public Set<MapNote> getMapNotes() {
		return mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setNotes(java.util.List)
	 */
	@Override
	public void setMapNotes(Set<MapNote> mapNotes) {
		this.mapNotes = mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void addMapNote(MapNote note) {
		mapNotes.add(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void removeMapNote(MapNote note) {
		mapNotes.remove(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
	 */
	@Override
	public String getTarget() {
		return target;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setTarget(java.lang.String)
	 */
	@Override
	public void setTarget(String target) {
		this.target = target;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getAdvices()
	 */
	@Override
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setAdvices(java.util.Set)
	 */
	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}
	
	@Override
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getRule()
	 */
	@Override
	public String getRule() {
		return rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setRule(java.lang.String)
	 */
	@Override
	public void setRule(String rule) {
		this.rule = rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getIndexMapPriority()
	 */
	@Override
	public int getIndexMapPriority() {
		return indexMapPriority;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setIndexMapPriority(java.lang.String)
	 */
	@Override
	public void setIndexMapPriority(int indexMapPriority) {
		this.indexMapPriority = indexMapPriority;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getRelationId()
	 */
	@Override
	public String getRelationId() {
		return relationId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setRelationId(java.lang.String)
	 */
	@Override
	public void setRelationId(String relationId) {
		this.relationId = relationId;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapRecord()
	 */
	@Override
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}
	
	@Override
	public int getMapGroup() {
		return this.mapGroup;
	}

	@Override
	public void setMapGroup(int mapGroup) {
		this.mapGroup = mapGroup;
		
	}

	@Override
	public int getMapBlock() {
		return this.mapBlock;
	}

	@Override
	public void setMapBlock(int mapBlock) {
		this.mapBlock = mapBlock;
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + indexMapPriority;
		result = prime * result
				+ ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result
				+ ((mapNotes == null) ? 0 : mapNotes.hashCode());
		result = prime * result
				+ ((mapRecord == null) ? 0 : mapRecord.hashCode());
		result = prime * result
				+ ((relationId == null) ? 0 : relationId.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MapEntryImpl other = (MapEntryImpl) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (indexMapPriority != other.indexMapPriority) {
			return false;
		}
		if (mapAdvices == null) {
			if (other.mapAdvices != null) {
				return false;
			}
		} else if (!mapAdvices.equals(other.mapAdvices)) {
			return false;
		}
		if (mapNotes == null) {
			if (other.mapNotes != null) {
				return false;
			}
		} else if (!mapNotes.equals(other.mapNotes)) {
			return false;
		}
		if (mapRecord == null) {
			if (other.mapRecord != null) {
				return false;
			}
		} else if (!mapRecord.equals(other.mapRecord)) {
			return false;
		}
		if (relationId == null) {
			if (other.relationId != null) {
				return false;
			}
		} else if (!relationId.equals(other.relationId)) {
			return false;
		}
		if (rule == null) {
			if (other.rule != null) {
				return false;
			}
		} else if (!rule.equals(other.rule)) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapEntryImpl [id=" + id + ", mapNotes=" + mapNotes + ", target="
				+ target + ", mapAdvices=" + mapAdvices + ", rule=" + rule + ", indexMapPriority="
				+ indexMapPriority + ", relationId=" + relationId + ", mapRecord=" + mapRecord
				+ "]";
	}

}
