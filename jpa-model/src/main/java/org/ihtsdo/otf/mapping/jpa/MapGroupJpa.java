package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapRecord;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

// TODO: Auto-generated Javadoc
/**
 * The Class MapGroupJpa.
 *
 * @author ${author}
 */
@Entity
@Table(name = "map_groups")
@Audited
@XmlRootElement(name = "mapGroup")
public class MapGroupJpa implements MapGroup {

	public MapGroupJpa() {
	}

	public MapGroupJpa(Long id, List<MapEntry> mapEntries, MapRecord mapRecord,
			MapBlock mapBlock, int indexMapPriority) {
		super();
		this.id = id;
		this.mapEntries = mapEntries;
		this.mapRecord = mapRecord;
		this.mapBlock = mapBlock;
		this.indexMapPriority = indexMapPriority;
	}

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The map entries. */
	@OneToMany(targetEntity=MapEntryJpa.class, cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	@JsonManagedReference
	private List<MapEntry> mapEntries = new ArrayList<MapEntry>();

	/** The map record. */
	@ManyToOne(targetEntity=MapRecordJpa.class, optional=false)
	@JsonBackReference
	@ContainedIn
	private MapRecord mapRecord;
	
	/** The map block. */
	@ManyToOne(targetEntity=MapBlockJpa.class, optional=true)
	@JsonBackReference
	@ContainedIn
	private MapBlock mapBlock;
	
	/** The index (map priority). */
	@Column(nullable = false)
	private int indexMapPriority;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getIndex()
	 */
	@Override
	public int getIndex() {
		return indexMapPriority;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setIndex(int)
	 */
	@Override
	public void setIndex(int index) {
		this.indexMapPriority = index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapEntries()
	 */
	@Override
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapEntries(java.util.List)
	 */
	@Override
	public void setMapEntries(List<MapEntry> mapEntries) {
		this.mapEntries = mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#addMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void addMapEntry(MapEntry mapEntry) {
		mapEntries.add(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#removeMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void removeMapEntry(MapEntry mapEntry) {
		mapEntries.remove(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapRecord()
	 */
	@Override
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapBlock()
	 */
	@Override
	public MapBlock getMapBlock() {
		return mapBlock;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapBlock(org.ihtsdo.otf.mapping.model.MapBlock)
	 */
	@Override
	public void setMapBlock(MapBlock mapBlock) {
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
		result = prime * result + ((mapBlock == null) ? 0 : mapBlock.hashCode());
		result =
				prime * result + ((mapEntries == null) ? 0 : mapEntries.hashCode());
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
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
		MapGroupJpa other = (MapGroupJpa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (indexMapPriority != other.indexMapPriority)
			return false;
		if (mapBlock == null) {
			if (other.mapBlock != null)
				return false;
		} else if (!mapBlock.equals(other.mapBlock))
			return false;
		if (mapEntries == null) {
			if (other.mapEntries != null)
				return false;
		} else if (!mapEntries.equals(other.mapEntries))
			return false;
		if (mapRecord == null) {
			if (other.mapRecord != null)
				return false;
		} else if (!mapRecord.equals(other.mapRecord))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapGroupJpa [id=" + id + ", mapEntries=" + mapEntries
				+ ", mapRecord=" + mapRecord + ", mapBlock=" + mapBlock
				+ ", indexMapPriority=" + indexMapPriority + "]";
	}

}
