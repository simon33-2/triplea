package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

/**
 * Logic to fire rockets.
 */
public class RocketsFireHelper {
  private Set<Territory> attackingFromTerritories = new HashSet<>();
  private Map<Territory,Territory> attackedTerritories = new LinkedHashMap<>();
  private Map<Territory,Unit> attackedUnits = new LinkedHashMap<>();

  private boolean isWW2V2(final GameData data) {
    return games.strategy.triplea.Properties.getWW2V2(data);
  }

  private boolean isAllRocketsAttack(final GameData data) {
    return games.strategy.triplea.Properties.getAllRocketsAttack(data);
  }

  private boolean isRocketsCanFlyOverImpassables(final GameData data) {
    return games.strategy.triplea.Properties.getRocketsCanFlyOverImpassables(data);
  }

  private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
  }

  private boolean isRocketAttacksPerFactoryInfinite(final GameData data) {
    return games.strategy.triplea.Properties.getRocketAttacksPerFactoryInfinite(data);
  }

  private boolean isPUCap(final GameData data) {
    return games.strategy.triplea.Properties.getPUCap(data);
  }

  private boolean isLimitRocketDamagePerTurn(final GameData data) {
    return games.strategy.triplea.Properties.getLimitRocketDamagePerTurn(data);
  }

  private boolean isLimitRocketDamageToProduction(final GameData data) {
    return games.strategy.triplea.Properties.getLimitRocketAndSBRDamageToProduction(data);
  }

  public RocketsFireHelper() {}

  public void findRocketTargets(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    final Collection<Unit> targets = new ArrayList<>();
    final PlayerID player = bridge.getPlayerID();
    final Set<Territory> rocketTerritories = getTerritoriesWithRockets(data, player);
    if (rocketTerritories.isEmpty()) {
      bridge.getHistoryWriter().startEvent(player.getName() + " has no rockets to fire");
      // getRemote(bridge).reportMessage("No rockets to fire", "No rockets to fire");
      return;
    }
    if (isWW2V2(data) || isAllRocketsAttack(data)) {
      fireWW2V2(bridge, data, player, rocketTerritories);
    } else {
      fireWW2V1(bridge, data, player, rocketTerritories);
    }
  }

  private void fireWW2V2(final IDelegateBridge bridge, final GameData data, 
                         final PlayerID player, final Set<Territory> rocketTerritories) {
    for (final Territory territory : rocketTerritories) {
      // Ask the user where each rocket launcher should target.
      getTarget(null, bridge, data, player, territory);
    }
  }

  /** In this rule set, each player only gets one rocket attack per turn. */
  private void fireWW2V1(final IDelegateBridge bridge, final GameData data, 
                         final PlayerID player, final Set<Territory> rocketTerritories) {
    final Set<Territory> targets = new HashSet<>();
    for (final Territory territory : rocketTerritories) {
      targets.addAll(getTargetsWithinRange(territory, data, player));
    }
    if (targets.isEmpty()) {
      bridge.getHistoryWriter().startEvent(player.getName() + " has no targets to attack with rockets");
      return;
    }
    getTarget(targets, bridge, data, player, null);
  }

  Set<Territory> getTerritoriesWithRockets(final GameData data, final PlayerID player) {
    final Set<Territory> territories = new HashSet<>();
    final CompositeMatch<Unit> ownedRockets = rocketMatch(player, data);
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(data);
    for (final Territory current : data.getMap()) {
      if (tracker.wasConquered(current)) {
        continue;
      }
      if (current.getUnits().someMatch(ownedRockets)) {
        territories.add(current);
      }
    }
    return territories;
  }

  CompositeMatch<Unit> rocketMatch(final PlayerID player, final GameData data) {
    return new CompositeMatchAnd<>(Matches.UnitIsRocket, Matches.unitIsOwnedBy(player), Matches.UnitIsNotDisabled,
        Matches.unitIsBeingTransported().invert(), Matches.unitIsSubmerged(data).invert(), Matches.unitHasNotMoved);
  }

  private Set<Territory> getTargetsWithinRange(final Territory territory, final GameData data, final PlayerID player) {
    final int maxDistance = TechAbilityAttachment.getRocketDistance(player, data);
    final Collection<Territory> possible = data.getMap().getNeighbors(territory, maxDistance);
    final Set<Territory> hasFactory = new HashSet<>();
    final CompositeMatchAnd<Territory> allowed =
        new CompositeMatchAnd<>(Matches.territoryAllowsRocketsCanFlyOver(player, data));
    if (!isRocketsCanFlyOverImpassables(data)) {
      allowed.add(Matches.TerritoryIsNotImpassable);
    }
    final CompositeMatchAnd<Unit> attackableUnits =
        new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.unitIsBeingTransported().invert());
    for (final Territory current : possible) {
      final Route route = data.getMap().getRoute(territory, current, allowed);
      if (route != null && route.numberOfSteps() <= maxDistance) {
        if (current.getUnits().someMatch(new CompositeMatchAnd<>(attackableUnits,
            Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(current).invert()))) {
          hasFactory.add(current);
        }
      }
    }
    return hasFactory;
  }

  private void getTarget(Collection<Territory> targets, final IDelegateBridge bridge, final GameData data, 
                         final PlayerID player, final Territory from) {
    // ask even if there is only once choice, that will allow the user to not attack if he doesn't want to
    // Ask the user where each rocket launcher should target.
    
    // While loop is only to allow the target selection to be cancelled in the second prompt in whatShouldBomberBomb
    while (true) {
      if (targets == null) {
        targets = getTargetsWithinRange(from, data, player);
        if (!isRocketAttacksPerFactoryInfinite(data)) {
          targets.removeAll(attackedTerritories.values());
        }
        if (targets.isEmpty()) {
          continue;
        }
      }
      final Territory target = ((ITripleAPlayer) bridge.getRemotePlayer()).whereShouldRocketsAttack(targets, from);
      if (target == null) {
        return;
      }
      final Collection<Unit> enemyUnits = target.getUnits().getMatches(
          new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.unitIsBeingTransported().invert()));
      final Collection<Unit> enemyTargetsTotal =
          Match.getMatches(enemyUnits, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(target).invert());
      if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(bridge.getData())) {
        final HashSet<UnitType> legalTargetsForTheseRockets = new HashSet<>();
        final Collection<Unit> rockets = new ArrayList<>(
          Match.getMatches(from.getUnits().getUnits(), rocketMatch(player, bridge.getData())));
        if (rockets == null) {
          legalTargetsForTheseRockets.addAll(bridge.getData().getUnitTypeList().getAllUnitTypes());
        } else {
          // a hack for now, we let the rockets fire at anyone who could be targetted by any rocket
          for (final Unit r : rockets) {
            legalTargetsForTheseRockets.addAll(UnitAttachment.get(r.getType()).getBombingTargets(data));
          }
        }
        final Collection<Unit> enemyTargets =
            Match.getMatches(enemyTargetsTotal, Matches.unitIsOfTypes(legalTargetsForTheseRockets));
        if (enemyTargets.isEmpty()) {
          // TODO: this sucks
          continue;
        }
        Unit unitTarget = null;
        if (enemyTargets.size() == 1) {
          unitTarget = enemyTargets.iterator().next();
        } else {
          final ITripleAPlayer iplayer = (ITripleAPlayer) bridge.getRemotePlayer(player);
          unitTarget = iplayer.whatShouldBomberBomb(target, enemyTargets, rockets);
        }
        if (unitTarget == null) {
          continue;
        }
        attackedUnits.put(from, unitTarget);
      }
      attackingFromTerritories.add(from);
      attackedTerritories.put(from, target);
      return;
    }
  }

  public void fireRockets(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    final PlayerID player = bridge.getPlayerID();
    final boolean DamageFromBombingDoneToUnits = isDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
    final boolean strictRockets = games.strategy.triplea.Properties.getStrictRockets(data);
    for( final Territory attackFrom :
                 strictRockets ? getTerritoriesWithRockets(data, player) : attackingFromTerritories ) {
      // Roll dice for the rocket attack damage and apply it
      if (strictRockets) {
        attackedTerritories = new LinkedHashMap<>();
        attackedUnits = new LinkedHashMap<>();
        getTarget(null, bridge, data, player, attackFrom);
      }
      final Territory attackedTerritory = attackedTerritories.get(attackFrom);
      if (attackedTerritory == null) {
        continue;
      }
      final PlayerID attackedPlayer = attackedTerritory.getOwner();
      TripleAUnit target = (TripleAUnit) (DamageFromBombingDoneToUnits ? attackedUnits.get(attackFrom) : null);
      final Resource PUs = data.getResourceList().getResource(Constants.PUS);
      // unit damage vs territory damage
      final Collection<Unit> rockets;
      // attackFrom could be null if WW2V1
      if (attackFrom == null) {
        rockets = null;
      } else {
        rockets = new ArrayList<>(Match.getMatches(attackFrom.getUnits().getUnits(), rocketMatch(player, data)));
      }
      final int numberOfAttacks = (rockets == null ? 1
          : Math.min(TechAbilityAttachment.getRocketNumberPerTerritory(player, data),
              TechAbilityAttachment.getRocketDiceNumber(rockets, data)));
      if (numberOfAttacks <= 0) {
        return;
      }
      final String transcript;
      final boolean doNotUseBombingBonus =
          !games.strategy.triplea.Properties.getUseBombingMaxDiceSidesAndBonus(data) || rockets == null;
      int cost = 0;
      if (!games.strategy.triplea.Properties.getLL_DAMAGE_ONLY(data)) {
        if (doNotUseBombingBonus || rockets == null) {
          // no low luck, and no bonus, so just roll based on the map's dice sides
          final int[] rolls = bridge.getRandom(data.getDiceSides(), numberOfAttacks, player, DiceType.BOMBING,
              "Rocket fired by " + player.getName() + " at " + attackedPlayer.getName());
          for (final int r : rolls) {
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
              + MyFormatter.asDice(rolls);
        } else {
          // we must use bombing bonus
          int highestMaxDice = 0;
          int highestBonus = 0;
          final int diceSides = data.getDiceSides();
          for (final Unit u : rockets) {
            final UnitAttachment ua = UnitAttachment.get(u.getType());
            int maxDice = ua.getBombingMaxDieSides();
            int bonus = ua.getBombingBonus();
            // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the
            // map, and zero for the
            // bonus.
            if (maxDice < 0) {
              maxDice = diceSides;
            }
            if (bonus < 0) {
              bonus = 0;
            }
            // we only roll once for rockets, so if there are other rockets here we just roll for the best rocket
            if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
              highestMaxDice = maxDice;
              highestBonus = bonus;
            }
          }
          // now we roll, or don't if there is nothing to roll.
          if (highestMaxDice > 0) {
            final int[] rolls = bridge.getRandom(highestMaxDice, numberOfAttacks, player, DiceType.BOMBING,
                "Rocket fired by " + player.getName() + " at " + attackedPlayer.getName());
            for (int i = 0; i < rolls.length; i++) {
              final int r = rolls[i] + highestBonus;
              rolls[i] = r;
              // we are zero based
              cost += r + 1;
            }
            transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
                + MyFormatter.asDice(rolls);
          } else {
            cost = highestBonus * numberOfAttacks;
            transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " do " + highestBonus
                + " damage for each rocket";
          }
        }
      } else {
        if (doNotUseBombingBonus || rockets == null) {
          // no bonus, so just roll based on the map's dice sides, but modify for LL
          final int maxDice = (data.getDiceSides() + 1) / 3;
          final int bonus = (data.getDiceSides() + 1) / 3;
          final int[] rolls = bridge.getRandom(maxDice, numberOfAttacks, player, DiceType.BOMBING,
              "Rocket fired by " + player.getName() + " at " + attackedPlayer.getName());
          for (int i = 0; i < rolls.length; i++) {
            final int r = rolls[i] + bonus;
            rolls[i] = r;
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
              + MyFormatter.asDice(rolls);
        } else {
          int highestMaxDice = 0;
          int highestBonus = 0;
          final int diceSides = data.getDiceSides();
          for (final Unit u : rockets) {
            final UnitAttachment ua = UnitAttachment.get(u.getType());
            int maxDice = ua.getBombingMaxDieSides();
            int bonus = ua.getBombingBonus();
            // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the
            // map, and zero for the
            // bonus.
            if (maxDice < 0 || doNotUseBombingBonus) {
              maxDice = diceSides;
            }
            if (bonus < 0 || doNotUseBombingBonus) {
              bonus = 0;
            }
            // now, regardless of whether they were set or not, we have to apply "low luck" to them, meaning in this case
            // that we reduce the
            // luck by 2/3.
            if (maxDice >= 5) {
              bonus += (maxDice + 1) / 3;
              maxDice = (maxDice + 1) / 3;
            }
            // we only roll once for rockets, so if there are other rockets here we just roll for the best rocket
            if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
              highestMaxDice = maxDice;
              highestBonus = bonus;
            }
          }
          // now we roll, or don't if there is nothing to roll.
          if (highestMaxDice > 0) {
            final int[] rolls = bridge.getRandom(highestMaxDice, numberOfAttacks, player, DiceType.BOMBING,
                "Rocket fired by " + player.getName() + " at " + attackedPlayer.getName());
            for (int i = 0; i < rolls.length; i++) {
              final int r = rolls[i] + highestBonus;
              rolls[i] = r;
              // we are zero based
              cost += r + 1;
            }
            transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
                + MyFormatter.asDice(rolls);
          } else {
            cost = highestBonus * numberOfAttacks;
            transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " do " + highestBonus
                + " damage for each rocket";
          }
        }
      }
      int territoryProduction = TerritoryAttachment.getProduction(attackedTerritory);
      if (DamageFromBombingDoneToUnits) {
        // we are doing damage to 'target', not to the territory
        final int damageLimit = target.getHowMuchMoreDamageCanThisUnitTake(target, attackedTerritory);
        cost = Math.max(0, Math.min(cost, damageLimit));
        final int totalDamage = target.getUnitDamage() + cost;
        // Record production lost
        // DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
        // apply the hits to the targets
        final IntegerMap<Unit> damageMap = new IntegerMap<>();
        damageMap.put(target, totalDamage);
        bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap));
        // attackedTerritory.notifyChanged();
      }
      // in WW2V2, limit rocket attack cost to production value of factory.
      else if (isWW2V2(data) || isLimitRocketDamageToProduction(data)) {
        // If we are limiting total PUs lost then take that into account
        if (isPUCap(data) || isLimitRocketDamagePerTurn(data)) {
          final int alreadyLost = DelegateFinder.moveDelegate(data).PUsAlreadyLost(attackedTerritory);
          territoryProduction -= alreadyLost;
          territoryProduction = Math.max(0, territoryProduction);
        }
        if (cost > territoryProduction) {
          cost = territoryProduction;
        }
      }
      // Record the PUs lost
      DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
      if (DamageFromBombingDoneToUnits) {
        getRemote(bridge).reportMessage(
            "Rocket attack in " + attackedTerritory.getName() + " does " + cost + " damage to " + target,
            "Rocket attack in " + attackedTerritory.getName() + " does " + cost + " damage to " + target);
        bridge.getHistoryWriter().startEvent("Rocket attack in " + attackedTerritory.getName() + " does " + cost
            + " damage to " + target);
      } else {
        cost *= Properties.getPU_Multiplier(data);
        getRemote(bridge).reportMessage("Rocket attack in " + attackedTerritory.getName() + " costs:" + cost,
            "Rocket attack in " + attackedTerritory.getName() + " costs:" + cost);
        // Trying to remove more PUs than the victim has is A Bad Thing[tm]
        final int availForRemoval = attackedPlayer.getResources().getQuantity(PUs);
        if (cost > availForRemoval) {
          cost = availForRemoval;
        }
        final String transcriptText =
            attackedPlayer.getName() + " lost " + cost + " PUs to rocket attack by " + player.getName();
        bridge.getHistoryWriter().startEvent(transcriptText);
        final Change rocketCharge = ChangeFactory.changeResourcesChange(attackedPlayer, PUs, -cost);
        bridge.addChange(rocketCharge);
      }
      bridge.getHistoryWriter().addChildToEvent(transcript, rockets == null ? null : new ArrayList<>(rockets));
      // this is null in WW2V1
      if (attackFrom != null) {
        if (rockets != null && !rockets.isEmpty()) {
          // TODO: only a certain number fired...
          final Change change = ChangeFactory.markNoMovementChange(Collections.singleton(rockets.iterator().next()));
          bridge.addChange(change);
        } else {
          throw new IllegalStateException("No rockets?" + attackFrom.getUnits().getUnits());
        }
      }
      if (target != null) {
        // kill any units that can die if they have reached max damage (veqryn)
        final UnitAttachment ua = UnitAttachment.get(target.getType());
        if (ua.getCanBeDamaged() && ua.getCanDieFromReachingMaxDamage()) {
          final List<Unit> unitsCanDie = Match.getMatches(Collections.singleton(target), Matches.UnitCanDieFromReachingMaxDamage);
          unitsCanDie
              .retainAll(Match.getMatches(unitsCanDie, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(attackedTerritory)));
          if (!unitsCanDie.isEmpty()) {
            // targets.removeAll(unitsCanDie);
            final Change removeDead = ChangeFactory.removeUnits(attackedTerritory, unitsCanDie);
            final String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + attackedTerritory.getName();
            bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
            bridge.addChange(removeDead);
          }
        }
      }
      // play a sound
      if (cost > 0) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_ROCKET, player);
      }
    }
  }
  
  private ITripleAPlayer getRemote(final IDelegateBridge bridge) {
    return (ITripleAPlayer) bridge.getRemotePlayer();
  }
}
