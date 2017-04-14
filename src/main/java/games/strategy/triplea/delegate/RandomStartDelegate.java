package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;

/**
 * This delegate sets up the game according to Risk rules, with a few allowed customizations.
 * Either divide all neutral territories between players randomly, or let them pick one by one.
 * After that, any remaining units get placed one by one.
 * (Note that m_player may not be used here, because this delegate is not run by any player [it is null])
 */
@MapSupport
public class RandomStartDelegate extends BaseTripleADelegate {
  private static final int UNITS_PER_PICK = 1;
  protected PlayerID currentPickingPlayer = null;

  @Override
  public void start() {
    super.start();
    setupBoard();
  }

  /**
   * Called before the delegate will stop running.
   */
  @Override
  public void end() {
    super.end();
    currentPickingPlayer = null;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return !(Match.noneMatch(getData().getMap().getTerritories(), getTerritoryPickableMatch())
        && Match.noneMatch(getData().getPlayerList().getPlayers(), getPlayerCanPickMatch()));
  }

  @Override
  public Serializable saveState() {
    final RandomStartExtendedDelegateState state = new RandomStartExtendedDelegateState();
    state.superState = super.saveState();
    state.currentPickingPlayer = this.currentPickingPlayer;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final RandomStartExtendedDelegateState s = (RandomStartExtendedDelegateState) state;
    super.loadState(s.superState);
    this.currentPickingPlayer = s.currentPickingPlayer;
  }

  protected void setupBoard() {
    final GameData data = getData();
    final boolean randomTerritories = games.strategy.triplea.Properties.getTerritoriesAreAssignedRandomly(data);
    final Match<Territory> pickableTerritoryMatch = getTerritoryPickableMatch();
    final Match<PlayerID> playerCanPickMatch = getPlayerCanPickMatch();
    final List<Territory> allPickableTerritories =
        Match.getMatches(data.getMap().getTerritories(), pickableTerritoryMatch);
    final List<PlayerID> playersCanPick = new ArrayList<>();
    playersCanPick.addAll(Match.getMatches(data.getPlayerList().getPlayers(), playerCanPickMatch));
    // we need a main event
    if (!playersCanPick.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Assigning Territories");
    }
    // for random:
    final int[] hitRandom = (!randomTerritories ? new int[0]
        : bridge.getRandom(allPickableTerritories.size(), allPickableTerritories.size(), null, DiceType.ENGINE,
            "Picking random territories"));
    int i = 0;
    int pos = 0;
    // divvy up territories
    while (!allPickableTerritories.isEmpty() && !playersCanPick.isEmpty()) {
      if (currentPickingPlayer == null || !playersCanPick.contains(currentPickingPlayer)) {
        currentPickingPlayer = playersCanPick.get(0);
      }
      if (!ThreadUtil.sleep(250)) {
        return;
      }
      Territory picked;
      if (randomTerritories) {
        pos += hitRandom[i];
        i++;
        final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(currentPickingPlayer, data);
        final List<Unit> units = new ArrayList<>(currentPickingPlayer.getUnits().getUnits());
        Collections.sort(units, new UnitCostComparator(costs));
        final Set<Unit> unitsToPlace = new HashSet<>();
        unitsToPlace.add(units.get(0));
        picked = allPickableTerritories.get(pos % allPickableTerritories.size());
        final CompositeChange change = new CompositeChange();
        change.add(ChangeFactory.changeOwner(picked, currentPickingPlayer));
        final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
        if (!factoryAndInfrastructure.isEmpty()) {
          change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, currentPickingPlayer));
        }
        change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
        change.add(ChangeFactory.addUnits(picked, unitsToPlace));
        bridge.getHistoryWriter().addChildToEvent(currentPickingPlayer.getName() + " receives territory "
            + picked.getName() + " with units " + MyFormatter.unitsToTextNoOwner(unitsToPlace), picked);
        bridge.addChange(change);
      } else {
        Tuple<Territory, Set<Unit>> pick;
        Set<Unit> unitsToPlace;
        while (true) {
          pick = getRemotePlayer(currentPickingPlayer).pickTerritoryAndUnits(
              new ArrayList<>(allPickableTerritories),
              new ArrayList<>(currentPickingPlayer.getUnits().getUnits()), UNITS_PER_PICK);
          picked = pick.getFirst();
          unitsToPlace = pick.getSecond();
          if (!allPickableTerritories.contains(picked)
              || !currentPickingPlayer.getUnits().getUnits().containsAll(unitsToPlace)
              || unitsToPlace.size() > UNITS_PER_PICK || (unitsToPlace.size() < UNITS_PER_PICK
                  && unitsToPlace.size() < currentPickingPlayer.getUnits().getUnits().size())) {
            getRemotePlayer(currentPickingPlayer).reportMessage("Chosen territory or units invalid!",
                "Chosen territory or units invalid!");
          } else {
            break;
          }
        }
        final CompositeChange change = new CompositeChange();
        change.add(ChangeFactory.changeOwner(picked, currentPickingPlayer));
        final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
        if (!factoryAndInfrastructure.isEmpty()) {
          change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, currentPickingPlayer));
        }
        change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
        change.add(ChangeFactory.addUnits(picked, unitsToPlace));
        bridge.getHistoryWriter().addChildToEvent(currentPickingPlayer.getName() + " picks territory "
            + picked.getName() + " and places in it " + MyFormatter.unitsToTextNoOwner(unitsToPlace), unitsToPlace);
        bridge.addChange(change);
      }
      allPickableTerritories.remove(picked);
      final PlayerID lastPlayer = currentPickingPlayer;
      currentPickingPlayer = getNextPlayer(playersCanPick, currentPickingPlayer);
      if (!playerCanPickMatch.match(lastPlayer)) {
        playersCanPick.remove(lastPlayer);
      }
      if (playersCanPick.isEmpty()) {
        currentPickingPlayer = null;
      }
    }
    // place any remaining units
    while (!playersCanPick.isEmpty()) {
      if (currentPickingPlayer == null || !playersCanPick.contains(currentPickingPlayer)) {
        currentPickingPlayer = playersCanPick.get(0);
      }
      final List<Territory> territoriesToPickFrom = data.getMap().getTerritoriesOwnedBy(currentPickingPlayer);
      Tuple<Territory, Set<Unit>> pick;
      Territory picked;
      Set<Unit> unitsToPlace;
      while (true) {
        pick = getRemotePlayer(currentPickingPlayer).pickTerritoryAndUnits(
            new ArrayList<>(territoriesToPickFrom),
            new ArrayList<>(currentPickingPlayer.getUnits().getUnits()), UNITS_PER_PICK);
        picked = pick.getFirst();
        unitsToPlace = pick.getSecond();
        if (!territoriesToPickFrom.contains(picked)
            || !currentPickingPlayer.getUnits().getUnits().containsAll(unitsToPlace)
            || unitsToPlace.size() > UNITS_PER_PICK || (unitsToPlace.size() < UNITS_PER_PICK
                && unitsToPlace.size() < currentPickingPlayer.getUnits().getUnits().size())) {
          getRemotePlayer(currentPickingPlayer).reportMessage("Chosen territory or units invalid!",
              "Chosen territory or units invalid!");
        } else {
          break;
        }
      }
      final CompositeChange change = new CompositeChange();
      final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
      if (!factoryAndInfrastructure.isEmpty()) {
        change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, currentPickingPlayer));
      }
      change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
      change.add(ChangeFactory.addUnits(picked, unitsToPlace));
      bridge.getHistoryWriter().addChildToEvent(currentPickingPlayer.getName() + " places "
          + MyFormatter.unitsToTextNoOwner(unitsToPlace) + " in territory " + picked.getName(), unitsToPlace);
      bridge.addChange(change);
      final PlayerID lastPlayer = currentPickingPlayer;
      currentPickingPlayer = getNextPlayer(playersCanPick, currentPickingPlayer);
      if (!playerCanPickMatch.match(lastPlayer)) {
        playersCanPick.remove(lastPlayer);
      }
      if (playersCanPick.isEmpty()) {
        currentPickingPlayer = null;
      }
    }
  }

  protected PlayerID getNextPlayer(final List<PlayerID> playersCanPick, final PlayerID currentPlayer) {
    int index = playersCanPick.indexOf(currentPlayer);
    if (index == -1) {
      return null;
    }
    index++;
    if (index >= playersCanPick.size()) {
      index = 0;
    }
    return playersCanPick.get(index);
  }

  public Match<Territory> getTerritoryPickableMatch() {
    return new CompositeMatchAnd<>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable,
        Matches.isTerritoryOwnedBy(PlayerID.NULL_PLAYERID), Matches.TerritoryIsEmpty);
  }

  public Match<PlayerID> getPlayerCanPickMatch() {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player) {
        if (player == null || player.equals(PlayerID.NULL_PLAYERID)) {
          return false;
        }
        if (player.getUnits().isEmpty()) {
          return false;
        }
        return !player.getIsDisabled();
      }
    };
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return null;
  }
}


class RandomStartExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 607794506772555083L;
  Serializable superState;
  // add other variables here:
  public PlayerID currentPickingPlayer;
}


class UnitCostComparator implements Comparator<Unit> {
  private final IntegerMap<UnitType> m_costs;

  public UnitCostComparator(final IntegerMap<UnitType> costs) {
    m_costs = costs;
  }

  public UnitCostComparator(final PlayerID player, final GameData data) {
    m_costs = BattleCalculator.getCostsForTUV(player, data);
  }

  @Override
  public int compare(final Unit u1, final Unit u2) {
    return m_costs.getInt(u1.getType()) - m_costs.getInt(u2.getType());
  }
}


class UnitTypeCostComparator implements Comparator<UnitType> {
  private final IntegerMap<UnitType> m_costs;

  public UnitTypeCostComparator(final IntegerMap<UnitType> costs) {
    m_costs = costs;
  }

  public UnitTypeCostComparator(final PlayerID player, final GameData data) {
    m_costs = BattleCalculator.getCostsForTUV(player, data);
  }

  @Override
  public int compare(final UnitType u1, final UnitType u2) {
    return m_costs.getInt(u1) - m_costs.getInt(u2);
  }
}
