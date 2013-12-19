package org.ihtsdo.otf.mapping.pojo;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapRecord;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapBlock}.
 *
 * @author ${author}
 */
public class MapBlockImpl implements MapBlock {

	/** The id. */
	private Long id;
	
	/** The index. */
	private int index;
	
	/** The rule. */
	private String rule;
	
	/** The map advices. */
	private Set<MapAdvice> mapAdvices;
	
	/** The map groups in this map block. */
	private List<MapGroup> mapGroups;
	
	/** The map record. */
	private MapRecord mapRecord;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getIndex()
	 */
	@Override
	public int getIndex() {
		return index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setIndex(int)
	 */
	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getRule()
	 */
	@Override
	public String getRule() {
		return rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setRule(java.lang.String)
	 */
	@Override
	public void setRule(String rule) {
		this.rule = rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getMapAdvices()
	 */
	@Override
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setMapAdvices(java.util.Set)
	 */
	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#addMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#removeMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getMapGroups()
	 */
	@Override
	public List<MapGroup> getMapGroups() {
		return mapGroups;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setMapGroups(java.util.List)
	 */
	@Override
	public void setMapGroups(List<MapGroup> mapGroups) {
		this.mapGroups = mapGroups;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#addMapGroup(org.ihtsdo.otf.mapping.model.MapGroup)
	 */
	@Override
	public void addMapGroup(MapGroup mapGroup) {
		mapGroups.add(mapGroup);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#removeMapGroup(org.ihtsdo.otf.mapping.model.MapGroup)
	 */
	@Override
	public void removeMapGroup(MapGroup mapGroup) {
		mapGroups.remove(mapGroup);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#getMapRecord()
	 */
	@Override
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapBlock#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + index;
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapGroups == null) ? 0 : mapGroups.hashCode());
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapBlockImpl other = (MapBlockImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (index != other.index)
			return false;
		if (mapAdvices == null) {
			if (other.mapAdvices != null)
				return false;
		} else if (!mapAdvices.equals(other.mapAdvices))
			return false;
		if (mapGroups == null) {
			if (other.mapGroups != null)
				return false;
		} else if (!mapGroups.equals(other.mapGroups))
			return false;
		if (mapRecord == null) {
			if (other.mapRecord != null)
				return false;
		} else if (!mapRecord.equals(other.mapRecord))
			return false;
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapBlockImpl [id=" + id + ", index=" + index + ", rule=" + rule
				+ ", mapAdvices=" + mapAdvices + ", mapGroups=" + mapGroups
				+ ", mapRecord=" + mapRecord + "]";
	}

}
