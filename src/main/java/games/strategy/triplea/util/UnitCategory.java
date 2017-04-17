package games.strategy.triplea.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitTypeComparator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Util;

public class UnitCategory implements Comparable<Object> {
  private final UnitType m_type;
  // Collection of UnitOwners, the type of our dependents, not the dependents
  private Collection<UnitOwner> m_dependents;
  // movement of the units
  private final int m_movement;
  // movement of the units
  private final int m_transportCost;
  // movement of the units
  // private final Territory m_originatingTerr;
  private final PlayerID m_owner;
  // the units in the category, may be duplicates.
  private final List<Unit> m_units = new ArrayList<>();
  private int m_damaged = 0;
  private int m_bombingDamage = 0;
  private boolean m_disabled = false;

  public UnitCategory(final Unit unit) {
    final TripleAUnit taUnit = (TripleAUnit) unit;
    m_type = taUnit.getType();
    m_owner = taUnit.getOwner();
    m_movement = -1;
    m_transportCost = -1;
    m_damaged = taUnit.getHits();
    m_bombingDamage = taUnit.getUnitDamage();
    m_disabled = Matches.UnitIsDisabled.match(unit);
    m_dependents = Collections.emptyList();
  }

  public UnitCategory(final Unit unit, final Collection<Unit> dependents, final int movement, final int transportCost) {
    this(unit, dependents, movement, 0, 0, false, transportCost);
  }

  public UnitCategory(final UnitType type, final PlayerID owner) {
    m_type = type;
    m_dependents = Collections.emptyList();
    m_movement = -1;
    m_transportCost = -1;
    m_owner = owner;
  }

  public UnitCategory(final Unit unit, final Collection<Unit> dependents, final int movement, final int damaged,
      final int bombingDamage, final boolean disabled, final int transportCost) {
    m_type = unit.getType();
    m_movement = movement;
    m_transportCost = transportCost;
    m_owner = unit.getOwner();
    m_damaged = damaged;
    m_bombingDamage = bombingDamage;
    m_disabled = disabled;
    m_units.add(unit);
    createDependents(dependents);
  }

  public int getDamaged() {
    return m_damaged;
  }

  public int getBombingDamage() {
    return m_bombingDamage;
  }

  public boolean hasDamageOrBombingUnitDamage() {
    return m_damaged > 0 || m_bombingDamage > 0;
  }

  public boolean getDisabled() {
    return m_disabled;
  }

  public int getHitPoints() {
    return UnitAttachment.get(m_type).getHitPoints();
  }

  private void createDependents(final Collection<Unit> dependents) {
    m_dependents = new ArrayList<>();
    if (dependents == null) {
      return;
    }
    for (final Unit current : dependents) {
      m_dependents.add(new UnitOwner(current));
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof UnitCategory)) {
      return false;
    }
    final UnitCategory other = (UnitCategory) o;
    // equality of categories does not compare the number
    // of units in the category, so don't compare on m_units
    final boolean equalsIgnoreDamaged = equalsIgnoreDamagedAndBombingDamageAndDisabled(other);
    // return equalsIgnoreDamaged && other.m_damaged == this.m_damaged;
    return equalsIgnoreDamaged && other.m_damaged == this.m_damaged && other.m_bombingDamage == this.m_bombingDamage
        && other.m_disabled == this.m_disabled;
  }

  public boolean equalsIgnoreDamagedAndBombingDamageAndDisabled(final UnitCategory other) {
    final boolean equalsIgnoreDamaged = other.m_type.equals(this.m_type) && other.m_movement == this.m_movement
        && other.m_owner.equals(this.m_owner) && Util.equals(this.m_dependents, other.m_dependents);
    return equalsIgnoreDamaged;
  }

  public boolean equalsIgnoreMovement(final UnitCategory other) {
    final boolean equalsIgnoreMovement = other.m_type.equals(this.m_type) && other.m_owner.equals(this.m_owner)
        && other.m_damaged == this.m_damaged && other.m_bombingDamage == this.m_bombingDamage
        && other.m_disabled == this.m_disabled && Util.equals(this.m_dependents, other.m_dependents);
    return equalsIgnoreMovement;
  }

  public boolean equalsIgnoreDependents(final UnitCategory other) {
    final boolean equalsIgnoreDependents = other.m_type.equals(this.m_type) && other.m_owner.equals(this.m_owner)
        && other.m_movement == this.m_movement && other.m_damaged == this.m_damaged
        && other.m_bombingDamage == this.m_bombingDamage && other.m_disabled == this.m_disabled;
    return equalsIgnoreDependents;
  }

  @Override
  public int hashCode() {
    return m_type.hashCode() | m_owner.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Entry type:").append(m_type.getName()).append(" owner:").append(m_owner.getName()).append(" damaged:")
        .append(m_damaged).append(" bombingUnitDamage:").append(m_bombingDamage).append(" disabled:").append(m_disabled)
        .append(" dependents:").append(m_dependents).append(" movement:").append(m_movement);
    return sb.toString();
  }

  /**
   * Collection of UnitOwners, the type of our dependents, not the dependents.
   */
  public Collection<UnitOwner> getDependents() {
    return m_dependents;
  }

  public List<Unit> getUnits() {
    return m_units;
  }

  public int getMovement() {
    return m_movement;
  }

  public int getTransportCost() {
    return m_transportCost;
  }

  public PlayerID getOwner() {
    return m_owner;
  }

  public void addUnit(final Unit unit) {
    m_units.add(unit);
  }

  void removeUnit(final Unit unit) {
    m_units.remove(unit);
  }

  public UnitType getType() {
    return m_type;
  }

  @Override
  public int compareTo(final Object o) {
    if (o == null) {
      return -1;
    }
    final UnitCategory other = (UnitCategory) o;
    if (!other.m_owner.equals(this.m_owner)) {
      return this.m_owner.getName().compareTo(other.m_owner.getName());
    }
    final int typeCompare = new UnitTypeComparator().compare(this.getType(), other.getType());
    if (typeCompare != 0) {
      return typeCompare;
    }
    if (m_movement != other.m_movement) {
      return m_movement - other.m_movement;
    }
    if (!Util.equals(this.m_dependents, other.m_dependents)) {
      return m_dependents.toString().compareTo(other.m_dependents.toString());
    }
    if (this.m_damaged != other.m_damaged) {
      return this.m_damaged - other.m_damaged;
    }
    if (this.m_bombingDamage != other.m_bombingDamage) {
      return this.m_bombingDamage - other.m_bombingDamage;
    }
    if (this.m_disabled != other.m_disabled) {
      if (m_disabled) {
        return 1;
      }
      return -1;
    }
    return 0;
  }
}
