package games.strategy.triplea.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.AbstractBasePlayer;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;

/**
 * Base class for AIs.
 *
 * <p>
 * Control pausing with the AI pause menu option.
 * AIs should note that any data that is stored in the AI instance, will be lost when the game is restarted.
 * We cannot save data with an AI, since the player may choose to restart the game with a different AI,
 * or with a human player.
 * </p>
 *
 * <p>
 * If an AI finds itself starting in the middle of a move phase, or the middle of a purchase phase,
 * (as would happen if a player saved the game during the middle of an AI's move phase) it is acceptable
 * for the AI to play badly for a turn, but the AI should recover, and play correctly when the next phase
 * of the game starts.
 * </p>
 *
 * <p>
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done
 * through an IDelegate using a change).
 * </p>
 */
public abstract class AbstractAI extends AbstractBasePlayer implements ITripleAPlayer {

  private static final Logger s_logger = Logger.getLogger(AbstractAI.class.getName());

  public AbstractAI(final String name, final String type) {
    super(name, type);
  }

  @Override
  public Territory selectBombardingTerritory(final Unit unit, final Territory unitTerritory,
      final Collection<Territory> territories, final boolean noneAvailable) {
    return territories.iterator().next();
  }

  @Override
  public boolean selectAttackSubs(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectAttackTransports(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectAttackUnits(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectShoreBombard(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean confirmMoveKamikaze() {
    return false;
  }

  @Override
  public boolean confirmMoveHariKari() {
    return false;
  }

  @Override
  public Territory whereShouldRocketsAttack(final Collection<Territory> candidates, final Territory from) {
    return candidates.iterator().next();
  }

  @Override
  public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
      final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
      final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite,
      final boolean allowMultipleHitsPerUnit) {
    if (defaultCasualties.size() != count) {
      throw new IllegalStateException("Select Casualties showing different numbers for number of hits to take vs total "
          + "size of default casualty selections");
    }
    if (defaultCasualties.getKilled().size() <= 0) {
      return new CasualtyDetails(defaultCasualties, false);
    }
    final int numberOfPlanesThatDoNotNeedToLandOnCarriers = 0;
    final CasualtyDetails myCasualties = new CasualtyDetails(false);
    myCasualties.addToDamaged(defaultCasualties.getDamaged());
    final List<Unit> selectFromSorted = new ArrayList<>(selectFrom);
    final List<Unit> interleavedTargetList = new ArrayList<>(
        AIUtils.interleaveCarriersAndPlanes(selectFromSorted, numberOfPlanesThatDoNotNeedToLandOnCarriers));
    for (int i = 0; i < defaultCasualties.getKilled().size(); ++i) {
      myCasualties.addToKilled(interleavedTargetList.get(i));
    }
    if (count != myCasualties.size()) {
      throw new IllegalStateException("AI chose wrong number of casualties");
    }
    return myCasualties;
  }

  @Override
  public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    final Collection<Unit> factories = Match.getMatches(potentialTargets, Matches.UnitCanProduceUnitsAndCanBeDamaged);
    if (factories.isEmpty()) {
      return potentialTargets.iterator().next();
    }
    return factories.iterator().next();
  }

  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved,
      final Territory from) {
    final List<Unit> rVal = new ArrayList<>();
    for (final Unit fighter : fightersThatCanBeMoved) {
      if (Math.random() < 0.8) {
        rVal.add(fighter);
      }
    }
    return rVal;
  }

  @Override
  public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory,
      final String unitMessage) {
    return candidates.iterator().next();
  }

  @Override
  public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories) {
    return true;
  }

  @Override
  public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory,
      final Collection<Territory> possibleTerritories, final String message) {
    return null;
  }

  @Override
  public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    return null;
  }

  @Override
  public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible,
      final String message) {
    return null;
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return false;
  }

  // TODO: This really needs to be rewritten with some basic logic
  @Override
  public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion,
      final boolean politics) {
    // we are dead, just accept
    if (!getPlayerID().amNotDeadYet(getGameData())) {
      return true;
    }
    // not related to politics? just accept i guess
    if (!politics) {
      return true;
    }
    // politics from ally? accept
    if (Matches.isAllied(getPlayerID(), getGameData()).match(playerSendingProposal)) {
      return true;
    }
    // would we normally be allies?
    final List<String> allies = Arrays.asList(new String[] {Constants.PLAYER_NAME_AMERICANS,
        Constants.PLAYER_NAME_AUSTRALIANS, Constants.PLAYER_NAME_BRITISH, Constants.PLAYER_NAME_CANADIANS,
        Constants.PLAYER_NAME_CHINESE, Constants.PLAYER_NAME_FRENCH, Constants.PLAYER_NAME_RUSSIANS});
    if (allies.contains(getPlayerID().getName()) && allies.contains(playerSendingProposal.getName())) {
      return true;
    }
    final List<String> axis = Arrays.asList(new String[] {Constants.PLAYER_NAME_GERMANS, Constants.PLAYER_NAME_ITALIANS,
        Constants.PLAYER_NAME_JAPANESE, Constants.PLAYER_NAME_PUPPET_STATES});
    if (axis.contains(getPlayerID().getName()) && axis.contains(playerSendingProposal.getName())) {
      return true;
    }
    final Collection<String> myAlliances =
        new HashSet<>(getGameData().getAllianceTracker().getAlliancesPlayerIsIn(getPlayerID()));
    myAlliances.retainAll(getGameData().getAllianceTracker().getAlliancesPlayerIsIn(playerSendingProposal));
    if (!myAlliances.isEmpty()) {
      return true;
    }
    return Math.random() < .5;
  }

  @Override
  public HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack) {
    final PlayerID id = getPlayerID();
    // we are going to just assign random attacks to each unit randomly, til we run out of tokens to attack with.
    final PlayerAttachment pa = PlayerAttachment.get(id);
    if (pa == null) {
      return null;
    }
    final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
    if (resourcesAndAttackValues.size() <= 0) {
      return null;
    }
    final IntegerMap<Resource> playerResourceCollection = id.getResources().getResourcesCopy();
    final IntegerMap<Resource> attackTokens = new IntegerMap<>();
    for (final Resource possible : resourcesAndAttackValues.keySet()) {
      final int amount = playerResourceCollection.getInt(possible);
      if (amount > 0) {
        attackTokens.put(possible, amount);
      }
    }
    if (attackTokens.size() <= 0) {
      return null;
    }
    final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> rVal = new HashMap<>();
    for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet()) {
      if (attackTokens.size() <= 0) {
        continue;
      }
      final Territory t = entry.getKey();
      final List<Unit> targets = new ArrayList<>(entry.getValue());
      Collections.shuffle(targets);
      for (final Unit u : targets) {
        if (attackTokens.size() <= 0) {
          continue;
        }
        final IntegerMap<Resource> rMap = new IntegerMap<>();
        final Resource r = attackTokens.keySet().iterator().next();
        final int num = Math.min(attackTokens.getInt(r),
            (UnitAttachment.get(u.getType()).getHitPoints() * (Math.random() < .3 ? 1 : (Math.random() < .5 ? 2 : 3))));
        rMap.put(r, num);
        HashMap<Unit, IntegerMap<Resource>> attMap = rVal.get(t);
        if (attMap == null) {
          attMap = new HashMap<>();
        }
        attMap.put(u, rMap);
        rVal.put(t, attMap);
        attackTokens.add(r, -num);
        if (attackTokens.getInt(r) <= 0) {
          attackTokens.removeKey(r);
        }
      }
    }
    return rVal;
  }

  @Override
  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final List<Territory> territoryChoices,
      final List<Unit> unitChoices, final int unitsPerPick) {
    pause();
    final GameData data = getGameData();
    final PlayerID me = getPlayerID();
    final Territory picked;
    if (territoryChoices == null || territoryChoices.isEmpty()) {
      picked = null;
    } else if (territoryChoices.size() == 1) {
      picked = territoryChoices.get(0);
    } else {
      Collections.shuffle(territoryChoices);
      final List<Territory> notOwned = Match.getMatches(territoryChoices, Matches.isTerritoryOwnedBy(me).invert());
      if (notOwned.isEmpty()) {
        // only owned territories left
        final boolean nonFactoryUnitsLeft = Match.someMatch(unitChoices, Matches.UnitCanProduceUnits.invert());
        final Match<Unit> ownedFactories =
            new CompositeMatchAnd<>(Matches.UnitCanProduceUnits, Matches.unitIsOwnedBy(me));
        final List<Territory> capitals = TerritoryAttachment.getAllCapitals(me, data);
        final List<Territory> test = new ArrayList<>(capitals);
        test.retainAll(territoryChoices);
        final List<Territory> territoriesWithFactories =
            Match.getMatches(territoryChoices, Matches.territoryHasUnitsThatMatch(ownedFactories));
        if (!nonFactoryUnitsLeft) {
          test.retainAll(Match.getMatches(test, Matches.territoryHasUnitsThatMatch(ownedFactories).invert()));
          if (!test.isEmpty()) {
            picked = test.get(0);
          } else {
            if (capitals.isEmpty()) {
              capitals.addAll(Match.getMatches(data.getMap().getTerritories(), new CompositeMatchAnd<>(
                  Matches.isTerritoryOwnedBy(me), Matches.territoryHasUnitsOwnedBy(me), Matches.TerritoryIsLand)));
            }
            final List<Territory> doesNotHaveFactoryYet =
                Match.getMatches(territoryChoices, Matches.territoryHasUnitsThatMatch(ownedFactories).invert());
            if (capitals.isEmpty() || doesNotHaveFactoryYet.isEmpty()) {
              picked = territoryChoices.get(0);
            } else {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(capitals.get(0), doesNotHaveFactoryYet, Match.getAlwaysMatch());
              picked = distanceMap.lowestKey();
            }
          }
        } else {
          final int maxTerritoriesToPopulate = Math.min(territoryChoices.size(),
              Math.max(4, Match.countMatches(unitChoices, Matches.UnitCanProduceUnits)));
          test.addAll(territoriesWithFactories);
          if (!test.isEmpty()) {
            if (test.size() < maxTerritoriesToPopulate) {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(test.get(0), territoryChoices, Match.getAlwaysMatch());
              for (int i = 0; i < maxTerritoriesToPopulate; i++) {
                final Territory choice = distanceMap.lowestKey();
                distanceMap.removeKey(choice);
                test.add(choice);
              }
            }
            Collections.shuffle(test);
            picked = test.get(0);
          } else {
            if (capitals.isEmpty()) {
              capitals.addAll(Match.getMatches(data.getMap().getTerritories(), new CompositeMatchAnd<>(
                  Matches.isTerritoryOwnedBy(me), Matches.territoryHasUnitsOwnedBy(me), Matches.TerritoryIsLand)));
            }
            if (capitals.isEmpty()) {
              picked = territoryChoices.get(0);
            } else {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(capitals.get(0), territoryChoices, Match.getAlwaysMatch());
              if (territoryChoices.contains(capitals.get(0))) {
                distanceMap.put(capitals.get(0), 0);
              }
              final List<Territory> choices = new ArrayList<>();
              for (int i = 0; i < maxTerritoriesToPopulate; i++) {
                final Territory choice = distanceMap.lowestKey();
                distanceMap.removeKey(choice);
                choices.add(choice);
              }
              Collections.shuffle(choices);
              picked = choices.get(0);
            }
          }
        }
      } else {
        // pick a not owned territory if possible
        final List<Territory> capitals = TerritoryAttachment.getAllCapitals(me, data);
        final List<Territory> test = new ArrayList<>(capitals);
        test.retainAll(notOwned);
        if (!test.isEmpty()) {
          picked = test.get(0);
        } else {
          if (capitals.isEmpty()) {
            capitals.addAll(Match.getMatches(data.getMap().getTerritories(), new CompositeMatchAnd<>(
                Matches.isTerritoryOwnedBy(me), Matches.territoryHasUnitsOwnedBy(me), Matches.TerritoryIsLand)));
          }
          if (capitals.isEmpty()) {
            picked = territoryChoices.get(0);
          } else {
            final IntegerMap<Territory> distanceMap =
                data.getMap().getDistance(capitals.get(0), notOwned, Match.getAlwaysMatch());
            picked = distanceMap.lowestKey();
          }
        }
      }
    }
    final Set<Unit> unitsToPlace = new HashSet<>();
    if (unitChoices != null && !unitChoices.isEmpty() && unitsPerPick > 0) {
      Collections.shuffle(unitChoices);
      final List<Unit> nonFactory = Match.getMatches(unitChoices, Matches.UnitCanProduceUnits.invert());
      if (nonFactory.isEmpty()) {
        for (int i = 0; i < unitsPerPick && !unitChoices.isEmpty(); i++) {
          unitsToPlace.add(unitChoices.get(0));
        }
      } else {
        for (int i = 0; i < unitsPerPick && !nonFactory.isEmpty(); i++) {
          unitsToPlace.add(nonFactory.get(0));
        }
      }
    }
    return Tuple.of(picked, unitsToPlace);
  }

  @Override
  public void confirmEnemyCasualties(final GUID battleId, final String message, final PlayerID hitPlayer) {}

  @Override
  public void reportError(final String error) {}

  @Override
  public void reportMessage(final String message, final String title) {}

  @Override
  public void confirmOwnCasualties(final GUID battleId, final String message) {
    pause();
  }

  @Override
  public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message,
      final int diceSides) {
    final int[] dice = new int[numRolls];
    for (int i = 0; i < numRolls; i++) {
      dice[i] = (int) Math.ceil(Math.random() * diceSides);
    }
    return dice;
  }

  /**
   * The given phase has started. We parse the phase name and call the appropriate method.
   */
  @Override
  public final void start(final String name) {
    super.start(name);
    final PlayerID id = getPlayerID();
    if (name.endsWith("Bid")) {
      final IPurchaseDelegate purchaseDelegate = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
      final String propertyName = id.getName() + " bid";
      final int bidAmount = getGameData().getProperties().get(propertyName, 0);
      purchase(true, bidAmount, purchaseDelegate, getGameData(), id);
    } else if (name.endsWith("Purchase")) {
      final IPurchaseDelegate purchaseDelegate = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
      final Resource PUs = getGameData().getResourceList().getResource(Constants.PUS);
      final int leftToSpend = id.getResources().getQuantity(PUs);
      purchase(false, leftToSpend, purchaseDelegate, getGameData(), id);
    } else if (name.endsWith("Tech")) {
      final ITechDelegate techDelegate = (ITechDelegate) getPlayerBridge().getRemoteDelegate();
      tech(techDelegate, getGameData(), id);
    } else if (name.endsWith("Move")) {
      final IMoveDelegate moveDel = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
      if (name.endsWith("AirborneCombatMove")) {
      } else {
        move(name.endsWith("NonCombatMove"), moveDel, getGameData(), id);
      }
    } else if (name.endsWith("Battle")) {
      battle((IBattleDelegate) getPlayerBridge().getRemoteDelegate(), getGameData(), id);
    } else if (name.endsWith("Politics")) {
      politicalActions();
    } else if (name.endsWith("Place")) {
      final IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
      place(name.indexOf("Bid") != -1, placeDel, getGameData(), id);
    } else if (name.endsWith("EndTurn")) {
      endTurn((IAbstractForumPosterDelegate) getPlayerBridge().getRemoteDelegate(), getGameData(), id);
    }
  }

  /************************
   * The following methods are called when the AI starts a phase.
   *************************/
  /**
   * It is the AI's turn to purchase units.
   *
   * @param purcahseForBid
   *        - is this a bid purchase, or a normal purchase
   * @param PUsToSpend
   *        - how many PUs we have to spend
   * @param purchaseDelegate
   *        - the purchase delgate to buy things with
   * @param data
   *        - the GameData
   * @param player
   *        - the player to buy for
   */
  protected abstract void purchase(boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaseDelegate,
      GameData data, PlayerID player);

  /**
   * It is the AI's turn to roll for technology.
   *
   * @param techDelegate
   *        - the tech delegate to roll for
   * @param data
   *        - the game data
   * @param player
   *        - the player to roll tech for
   */
  protected abstract void tech(ITechDelegate techDelegate, GameData data, PlayerID player);

  /**
   * It is the AI's turn to move. Make all moves before returning from this method.
   *
   * @param nonCombat
   *        - are we moving in combat, or non combat
   * @param moveDel
   *        - the move delegate to make moves with
   * @param data
   *        - the current game data
   * @param player
   *        - the player to move with
   */
  protected abstract void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player);

  /**
   * It is the AI's turn to place units. get the units available to place with player.getUnits()
   *
   * @param placeForBid
   *        - is this a placement for bid
   * @param placeDelegate
   *        - the place delegate to place with
   * @param data
   *        - the current Game Data
   * @param player
   *        - the player to place for
   */
  protected abstract void place(boolean placeForBid, IAbstractPlaceDelegate placeDelegate, GameData data,
      PlayerID player);

  /**
   * No need to override this.
   */
  protected void endTurn(final IAbstractForumPosterDelegate endTurnForumPosterDelegate, final GameData data,
      final PlayerID player) {
    // we should not override this...
  }

  /**
   * It is the AI's turn to fight. Subclasses may override this if they want, but
   * generally the AI does not need to worry about the order of fighting battles.
   *
   * @param battleDelegate
   *        the battle delegate to query for battles not fought and the
   * @param data
   *        - the current GameData
   * @param player
   *        - the player to fight for
   */
  protected void battle(final IBattleDelegate battleDelegate, final GameData data, final PlayerID player) {
    // generally all AI's will follow the same logic.
    // loop until all battles are fought.
    // rather than try to analyze battles to figure out which must be fought before others
    // as in the case of a naval battle preceding an amphibious attack,
    // keep trying to fight every battle
    while (true) {
      final BattleListing listing = battleDelegate.getBattles();
      if (listing.isEmpty()) {
        return;
      }
      for (final Entry<BattleType, Collection<Territory>> entry : listing.getBattles().entrySet()) {
        for (final Territory current : entry.getValue()) {
          final String error = battleDelegate.fightBattle(current, entry.getKey().isBombingRun(), entry.getKey());
          if (error != null) {
            s_logger.fine(error);
          }
        }
      }
    }
  }

  protected void politicalActions() {
    final IPoliticsDelegate iPoliticsDelegate = (IPoliticsDelegate) getPlayerBridge().getRemoteDelegate();
    final GameData data = getGameData();
    final PlayerID id = getPlayerID();
    final float numPlayers = data.getPlayerList().getPlayers().size();
    final PoliticsDelegate politicsDelegate = DelegateFinder.politicsDelegate(data);
    // We want to test the conditions each time to make sure they are still valid
    if (Math.random() < .5) {
      final List<PoliticalActionAttachment> actionChoicesTowardsWar =
          AIPoliticalUtils.getPoliticalActionsTowardsWar(id, politicsDelegate.getTestedConditions(), data);
      if (actionChoicesTowardsWar != null && !actionChoicesTowardsWar.isEmpty()) {
        Collections.shuffle(actionChoicesTowardsWar);
        int i = 0;
        // should we use bridge's random source here?
        final double random = Math.random();
        int MAX_WAR_ACTIONS_PER_TURN =
            (random < .5 ? 0 : (random < .9 ? 1 : (random < .99 ? 2 : (int) numPlayers / 2)));
        if ((MAX_WAR_ACTIONS_PER_TURN > 0)
            && (Match.countMatches(data.getRelationshipTracker().getRelationships(id), Matches.RelationshipIsAtWar))
                / numPlayers < 0.4) {
          if (Math.random() < .9) {
            MAX_WAR_ACTIONS_PER_TURN = 0;
          } else {
            MAX_WAR_ACTIONS_PER_TURN = 1;
          }
        }
        final Iterator<PoliticalActionAttachment> actionWarIter = actionChoicesTowardsWar.iterator();
        while (actionWarIter.hasNext() && MAX_WAR_ACTIONS_PER_TURN > 0) {
          final PoliticalActionAttachment action = actionWarIter.next();
          if (!Matches.AbstractUserActionAttachmentCanBeAttempted(politicsDelegate.getTestedConditions())
              .match(action)) {
            continue;
          }
          i++;
          if (i > MAX_WAR_ACTIONS_PER_TURN) {
            break;
          }
          iPoliticsDelegate.attemptAction(action);
        }
      }
    } else {
      final List<PoliticalActionAttachment> actionChoicesOther =
          AIPoliticalUtils.getPoliticalActionsOther(id, politicsDelegate.getTestedConditions(), data);
      if (actionChoicesOther != null && !actionChoicesOther.isEmpty()) {
        Collections.shuffle(actionChoicesOther);
        int i = 0;
        // should we use bridge's random source here?
        final double random = Math.random();
        final int MAX_OTHER_ACTIONS_PER_TURN =
            (random < .3 ? 0 : (random < .6 ? 1 : (random < .9 ? 2 : (random < .99 ? 3 : (int) numPlayers))));
        final Iterator<PoliticalActionAttachment> actionOtherIter = actionChoicesOther.iterator();
        while (actionOtherIter.hasNext() && MAX_OTHER_ACTIONS_PER_TURN > 0) {
          final PoliticalActionAttachment action = actionOtherIter.next();
          if (!Matches.AbstractUserActionAttachmentCanBeAttempted(politicsDelegate.getTestedConditions())
              .match(action)) {
            continue;
          }
          if (action.getCostPU() > 0 && action.getCostPU() > id.getResources().getQuantity(Constants.PUS)) {
            continue;
          }
          i++;
          if (i > MAX_OTHER_ACTIONS_PER_TURN) {
            break;
          }
          iPoliticsDelegate.attemptAction(action);
        }
      }
    }
  }

  /**
   * Pause the game to allow the human player to see what is going on.
   */
  protected void pause() {
    ThreadUtil.sleep(AbstractUIContext.getAIPauseDuration());
  }

}
