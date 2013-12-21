package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapRecord;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * The Map Block Jpa object
 */
@Entity
@Table(name = "map_blocks")
@Audited
@XmlRootElement(name = "mapBlock")
public class MapBlockJpa implements MapBlock {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(targetEntity=MapRecordJpa.class, optional=false)
	@JsonBackReference
	@ContainedIn
	private MapRecord mapRecord;
	
	/** The map entries. */
	@OneToMany(targetEntity=MapGroupJpa.class, cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	@JsonManagedReference
	private List<MapGroup> mapGroups = new ArrayList<MapGroup>();

	/** The index (map priority). */
	@Column(nullable = false)
	private int indexMapPriority;

	/** The rule. */
	@Column(nullable = true, length = 50)
	private String rule;
	
	/** The map advices. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	
	public MapBlockJpa() {
	}

	public MapBlockJpa(Long id, MapRecord mapRecord, List<MapGroup> mapGroups,
			int indexMapPriority, String rule, Set<MapAdvice> mapAdvices) {
		super();
		this.id = id;
		this.mapRecord = mapRecord;
		this.mapGroups = mapGroups;
		this.indexMapPriority = indexMapPriority;
		this.rule = rule;
		this.mapAdvices = mapAdvices;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public int getIndex() {
		return indexMapPriority;
	}

	@Override
	public void setIndex(int index) {
		this.indexMapPriority = index;
	}

	@Override
	public List<MapGroup> getMapGroups() {
		return mapGroups;
	}

	@Override
	public void setMapGroups(List<MapGroup> mapGroups) {
		this.mapGroups = mapGroups;
	}

	@Override
	public void addMapGroup(MapGroup mapGroup) {
		mapGroups.add(mapGroup);
	}

	@Override
	public void removeMapGroup(MapGroup mapGroup) {
		mapGroups.remove(mapGroup);
	}

	@Override
	public String getRule() {
		return rule;
	}

	@Override
	public void setRule(String rule) {
		this.rule = rule;
	}

	@Override
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		this.mapAdvices.add(mapAdvice);
	}

	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		this.mapAdvices.remove(mapAdvice);		
	}

	@Override
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + indexMapPriority;
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapGroups == null) ? 0 : mapGroups.hashCode());
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapBlockJpa other = (MapBlockJpa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (indexMapPriority != other.indexMapPriority)
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

}
