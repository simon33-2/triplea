package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

/**
 * An abstraction of MoveDelegate in order to allow other delegates to extend this.
 */
public abstract class AbstractMoveDelegate extends BaseTripleADelegate implements IMoveDelegate {
  // A collection of UndoableMoves
  protected List<UndoableMove> m_movesToUndo = new ArrayList<>();
  // protected final TransportTracker m_transportTracker = new TransportTracker();
  // if we are in the process of doing a move. this instance will allow us to resume the move
  protected MovePerformer m_tempMovePerformer;

  public static enum MoveType {
    DEFAULT, SPECIAL
  }

  public AbstractMoveDelegate() {}

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    if (m_tempMovePerformer != null) {
      m_tempMovePerformer.initialize(this);
      m_tempMovePerformer.resume();
      m_tempMovePerformer = null;
    }
  }

  /**
   * Called before the delegate will stop running.
   */
  @Override
  public void end() {
    super.end();
    m_movesToUndo.clear();
  }

  @Override
  public Serializable saveState() {
    final AbstractMoveExtendedDelegateState state = new AbstractMoveExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    state.m_movesToUndo = m_movesToUndo;
    state.m_tempMovePerformer = m_tempMovePerformer;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final AbstractMoveExtendedDelegateState s = (AbstractMoveExtendedDelegateState) state;
    super.loadState(s.superState);
    // if the undo state wasnt saved, then dont load it. prevents overwriting undo state when we restore from an undo
    // move
    if (s.m_movesToUndo != null) {
      m_movesToUndo = s.m_movesToUndo;
    }
    m_tempMovePerformer = s.m_tempMovePerformer;
  }

  @Override
  public List<UndoableMove> getMovesMade() {
    return new ArrayList<>(m_movesToUndo);
  }

  @Override
  public String undoMove(final int moveIndex) {
    if (m_movesToUndo.isEmpty()) {
      return "No moves to undo";
    }
    if (moveIndex >= m_movesToUndo.size()) {
      return "Undo move index out of range";
    }
    final UndoableMove moveToUndo = m_movesToUndo.get(moveIndex);
    if (!moveToUndo.getcanUndo()) {
      return moveToUndo.getReasonCantUndo();
    }
    moveToUndo.undo(m_bridge);
    m_movesToUndo.remove(moveIndex);
    updateUndoableMoveIndexes();
    return null;
  }

  private void updateUndoableMoveIndexes() {
    for (int i = 0; i < m_movesToUndo.size(); i++) {
      m_movesToUndo.get(i).setIndex(i);
    }
  }

  protected void updateUndoableMoves(final UndoableMove currentMove) {
    currentMove.initializeDependencies(m_movesToUndo);
    m_movesToUndo.add(currentMove);
    updateUndoableMoveIndexes();
  }

  protected PlayerID getUnitsOwner(final Collection<Unit> units) {
    // if we are not in edit mode, return m_player. if we are in edit mode, we use whoever's units these are.
    if (units.isEmpty() || !BaseEditDelegate.getEditMode(getData())) {
      return m_player;
    } else {
      return units.iterator().next().getOwner();
    }
  }

  @Override
  public String move(final Collection<Unit> units, final Route route) {
    return move(units, route, Collections.emptyList());
  }

  @Override
  public String move(final Collection<Unit> units, final Route route,
      final Collection<Unit> transportsThatCanBeLoaded) {
    return move(units, route, transportsThatCanBeLoaded, new HashMap<>());
  }

  @Override
  public abstract String move(final Collection<Unit> units, final Route route,
      final Collection<Unit> m_transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> newDependents);

  public static MoveValidationResult validateMove(final MoveType moveType, final Collection<Unit> units,
      final Route route, final PlayerID player, final Collection<Unit> transportsToLoad,
      final Map<Unit, Collection<Unit>> newDependents, final boolean isNonCombat,
      final List<UndoableMove> undoableMoves, final GameData data) {
    if (moveType == MoveType.SPECIAL) {
      return SpecialMoveDelegate.validateMove(units, route, player, transportsToLoad, newDependents, isNonCombat,
          undoableMoves, data);
    }
    return MoveValidator.validateMove(units, route, player, transportsToLoad, newDependents, isNonCombat, undoableMoves,
        data);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand(final PlayerID player) {
    return new AirThatCantLandUtil(m_bridge).getTerritoriesWhereAirCantLand(player);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand() {
    return new AirThatCantLandUtil(m_bridge).getTerritoriesWhereAirCantLand(m_player);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereUnitsCantFight() {
    return new UnitsThatCantFightUtil(getData()).getTerritoriesWhereUnitsCantFight(m_player);
  }

  /**
   * @param unit
   *        referring unit.
   * @param end
   *        target territory
   * @return the route that a unit used to move into the given territory
   */
  public Route getRouteUsedToMoveInto(final Unit unit, final Territory end) {
    return AbstractMoveDelegate.getRouteUsedToMoveInto(m_movesToUndo, unit, end);
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param undoableMoves
   *        list of moves that have been done
   * @param unit
   *        referring unit
   * @param end
   *        target territory
   * @return the route that a unit used to move into the given territory.
   */
  public static Route getRouteUsedToMoveInto(final List<UndoableMove> undoableMoves, final Unit unit,
      final Territory end) {
    final ListIterator<UndoableMove> iter = undoableMoves.listIterator(undoableMoves.size());
    while (iter.hasPrevious()) {
      final UndoableMove move = iter.previous();
      if (!move.getUnits().contains(unit)) {
        continue;
      }
      if (move.getRoute().getEnd().equals(end)) {
        return move.getRoute();
      }
    }
    return null;
  }

  public static BattleTracker getBattleTracker(final GameData data) {
    return DelegateFinder.battleDelegate(data).getBattleTracker();
  }

  protected boolean isWW2V2() {
    return games.strategy.triplea.Properties.getWW2V2(getData());
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    // nothing for now
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    return false;
  }

  @Override
  public boolean postTurnSummary(final PBEMMessagePoster poster, final String title, final boolean includeSaveGame) {
    return poster.post(m_bridge.getHistoryWriter(), title, includeSaveGame);
  }

  public abstract int PUsAlreadyLost(final Territory t);

  public abstract void PUsLost(final Territory t, final int amt);

  @Override
  public Class<IMoveDelegate> getRemoteType() {
    return IMoveDelegate.class;
  }
}


