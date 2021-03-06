package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;

/**
 * Provides some static methods for validating game edits.
 */
public class EditValidator {
  private static String validateTerritoryBasic(final GameData data, final Territory territory) {
    return validateTerritoryBasic(data, territory, null);
  }

  private static String validateTerritoryBasic(final GameData data, final Territory territory, final PlayerID player) {
    final String result = null;
    /*
     * // territory cannot contain enemy units
     * if (!Matches.territoryIsEmptyOfCombatUnits(data, player).match(territory))
     * return "Territory contains enemy units";
     */
    /*
     * // territory cannot be in a pending battle
     * BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
     * if (battleTracker.getPendingBattle(territory, true) != null)
     * return "Territory contains a pending SBR battle";
     * if (battleTracker.getPendingBattle(territory, false) != null)
     * return "Territory contains a pending battle";
     */
    // territory cannot be in an UndoableMove route
    final List<UndoableMove> moves = DelegateFinder.moveDelegate(data).getMovesMade();
    for (final UndoableMove move : moves) {
      if (move.getRoute().getStart() == territory || move.getRoute().getEnd() == territory) {
        return "Territory is start or end of a pending move";
      }
    }
    return result;
  }

  public static String validateChangeTerritoryOwner(final GameData data, final Territory territory,
      final PlayerID player) {
    String result = null;
    if (Matches.TerritoryIsWater.match(territory) && territory.getOwner().equals(PlayerID.NULL_PLAYERID)
        && TerritoryAttachment.get(territory) == null) {
      return "Territory is water and has no attachment";
    }
    if ((result = validateTerritoryBasic(data, territory, player)) != null) {
      return result;
    }
    return result;
  }

  public static String validateAddUnits(final GameData data, final Territory territory, final Collection<Unit> units) {
    String result = null;
    if (units.isEmpty()) {
      return "No units selected";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // check land/water sanity
    if (territory.isWater()) {
      if (!Match.allMatch(units, Matches.UnitIsSea)) {
        if (Match.someMatch(units, Matches.UnitIsLand)) {
          if (!Match.allMatch(units, Matches.alliedUnit(player, data))) {
            return "Can't add mixed nationality units to water";
          }
          final Match<Unit> friendlySeaTransports =
              new CompositeMatchAnd<>(Matches.UnitIsTransport, Matches.UnitIsSea, Matches.alliedUnit(player, data));
          final Collection<Unit> seaTransports = Match.getMatches(units, friendlySeaTransports);
          final Collection<Unit> landUnitsToAdd = Match.getMatches(units, Matches.UnitIsLand);
          if (!Match.allMatch(landUnitsToAdd, Matches.UnitCanBeTransported)) {
            return "Can't add land units that can't be transported, to water";
          }
          seaTransports.addAll(territory.getUnits().getMatches(friendlySeaTransports));
          if (seaTransports.isEmpty()) {
            return "Can't add land units to water without enough transports";
          }
          final Map<Unit, Unit> mapLoading = TransportUtils.mapTransportsToLoad(landUnitsToAdd, seaTransports);
          if (!mapLoading.keySet().containsAll(landUnitsToAdd)) {
            return "Can't add land units to water without enough transports";
          }
        }
        if (Match.someMatch(units, Matches.UnitIsAir)) {
          if (Match.someMatch(units,
              new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.UnitCanLandOnCarrier.invert()))) {
            return "Cannot add air to water unless it can land on carriers";
          }
          // Set up matches
          final Match<Unit> friendlyCarriers =
              new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.alliedUnit(player, data));
          final Match<Unit> friendlyAirUnits =
              new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.alliedUnit(player, data));
          // Determine transport capacity
          final int carrierCapacityTotal =
              AirMovementValidator.carrierCapacity(territory.getUnits().getMatches(friendlyCarriers), territory)
                  + AirMovementValidator.carrierCapacity(units, territory);
          final int carrierCost = AirMovementValidator.carrierCost(territory.getUnits().getMatches(friendlyAirUnits))
              + AirMovementValidator.carrierCost(units);
          if (carrierCapacityTotal < carrierCost) {
            return "Can't add more air units to water without sufficient space";
          }
        }
      }
    } else {
      /*
       * // Can't add to enemy territory
       * if (Matches.isTerritoryEnemy(player, data).match(territory) && !Matches.TerritoryIsWater.match(territory))
       * return "Can't add units to enemy territory";
       */
      if (Match.someMatch(units, Matches.UnitIsSea)) {
        return "Can't add sea units to land";
      }
    }
    if ((result = validateTerritoryBasic(data, territory, player)) != null) {
      return result;
    }
    return result;
  }

  public static String validateRemoveUnits(final GameData data, final Territory territory,
      final Collection<Unit> units) {
    String result = null;
    if (units.isEmpty()) {
      return "No units selected";
    }
    final PlayerID player = units.iterator().next().getOwner();
    /*
     * all units should be same owner
     * if (!Match.allMatch(units, Matches.unitIsOwnedBy(player)))
     * return "Not all units have the same owner";
     */
    if ((result = validateTerritoryBasic(data, territory, player)) != null) {
      return result;
    }
    // if transport selected, all transported units must be deleted too
    for (final Unit unit : Match.getMatches(units, Matches.UnitCanTransport)) {
      if (!units.containsAll(TransportTracker.transporting(unit))) {
        return "Can't remove transport without removing transported units";
      }
    }
    // if transported units selected, transport must be deleted too
    for (final Unit unit : Match.getMatches(units, Matches.UnitCanBeTransported)) {
      final Unit transport = TransportTracker.transportedBy(unit);
      if (transport != null && !units.contains(transport)) {
        return "Can't remove transported units without removing transport";
      }
    }
    // TODO: if carrier selected, all carried planes must be deleted too
    // TODO: if carried planes selected, carrier must be deleted too
    return result;
  }

  public static String validateAddTech(final GameData data, final Collection<TechAdvance> techs,
      final PlayerID player) {
    final String result = null;
    if (techs == null) {
      return "No tech selected";
    }
    if (player == null) {
      return "No player selected";
    }
    if (!games.strategy.triplea.Properties.getTechDevelopment(data)) {
      return "Technology not enabled";
    }
    if (player.getAttachment(Constants.TECH_ATTACHMENT_NAME) == null) {
      return "Player has no Tech Attachment";
    }
    for (final TechAdvance tech : techs) {
      if (tech == null) {
        return "No tech selected";
      }
      if (!TechnologyDelegate.getAvailableTechs(player, data).contains(tech)) {
        return "Technology not available for this player";
      }
    }
    return result;
  }

  public static String validateRemoveTech(final GameData data, final Collection<TechAdvance> techs,
      final PlayerID player) {
    final String result = null;
    if (techs == null) {
      return "No tech selected";
    }
    if (player == null) {
      return "No player selected";
    }
    if (!games.strategy.triplea.Properties.getTechDevelopment(data)) {
      return "Technology not enabled";
    }
    for (final TechAdvance tech : techs) {
      if (tech == null) {
        return "No tech selected";
      }
      if (!TechTracker.getCurrentTechAdvances(player, data).contains(tech)) {
        return "Player does not have this tech";
      }
      if (tech.getProperty().equals(TechAdvance.TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY)) {
        return "Cannot remove " + TechAdvance.TECH_NAME_INDUSTRIAL_TECHNOLOGY;
      }
      if (tech.getProperty().equals(TechAdvance.TECH_PROPERTY_IMPROVED_SHIPYARDS)) {
        return "Cannot remove " + TechAdvance.TECH_NAME_IMPROVED_SHIPYARDS;
      }
    }
    return result;
  }

  public static String validateChangeHitDamage(final GameData data, final IntegerMap<Unit> unitDamageMap,
      final Territory territory) {
    String result = null;
    if (unitDamageMap == null || unitDamageMap.isEmpty()) {
      return "Damage map is empty";
    }
    if ((result = validateTerritoryBasic(data, territory)) != null) {
      return result;
    }
    final Collection<Unit> units = new ArrayList<>(unitDamageMap.keySet());
    if (!territory.getUnits().getUnits().containsAll(units)) {
      return "Selected Territory does not contain all of the selected units";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // all units should be same owner
    if (!Match.allMatch(units, Matches.unitIsOwnedBy(player))) {
      return "Not all units have the same owner";
    }
    if (!Match.allMatch(units, Matches.UnitHasMoreThanOneHitPointTotal)) {
      return "Not all units have more than one total hitpoints";
    }
    for (final Unit u : units) {
      final int dmg = unitDamageMap.getInt(u);
      if (dmg < 0 || dmg >= UnitAttachment.get(u.getType()).getHitPoints()) {
        return "Damage cannot be less than zero or equal to or greater than unit hitpoints (if you want to kill the unit, use remove unit)";
      }
    }
    return result;
  }

  public static String validateChangeBombingDamage(final GameData data, final IntegerMap<Unit> unitDamageMap,
      final Territory territory) {
    String result = null;
    if (unitDamageMap == null || unitDamageMap.isEmpty()) {
      return "Damage map is empty";
    }
    if ((result = validateTerritoryBasic(data, territory)) != null) {
      return result;
    }
    if (!games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
      return "Game does not allow bombing damage";
    }
    final Collection<Unit> units = new ArrayList<>(unitDamageMap.keySet());
    if (!territory.getUnits().getUnits().containsAll(units)) {
      return "Selected Territory does not contain all of the selected units";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // all units should be same owner
    if (!Match.allMatch(units, Matches.unitIsOwnedBy(player))) {
      return "Not all units have the same owner";
    }
    if (!Match.allMatch(units, Matches.UnitCanBeDamaged)) {
      return "Not all units can take bombing damage";
    }
    for (final Unit u : units) {
      final int dmg = unitDamageMap.getInt(u);
      if (dmg < 0 || dmg > ((TripleAUnit) u).getHowMuchDamageCanThisUnitTakeTotal(u, territory)) {
        return "Damage cannot be less than zero or greater than the max damage of the unit";
      }
    }
    return result;
  }

  public static String validateChangePoliticalRelationships(final GameData data,
      final Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges) {
    final String result = null;
    if (relationshipChanges == null || relationshipChanges.isEmpty()) {
      return "Relationship Changes are empty";
    }
    for (final Triple<PlayerID, PlayerID, RelationshipType> relationshipChange : relationshipChanges) {
      if (relationshipChange.getFirst() == null || relationshipChange.getSecond() == null) {
        return "Players are null";
      }
      if (relationshipChange.getThird() == null) {
        return "New Relationship is null";
      }
    }
    return result;
  }
}
