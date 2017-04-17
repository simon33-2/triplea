package games.strategy.triplea.ui;

import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Util;

public class MovePanel extends AbstractMovePanel {
  private static final long serialVersionUID = 5004515340964828564L;
  private static final int s_defaultMinTransportCost = 5;
  /**
   * @param s_deselectNumber
   *        adds or removes 10 units (used to remove 1/s_deselectNumber of total units (useful for splitting large
   *        armies), but changed it
   *        after feedback).
   */
  private static final int s_deselectNumber = 10;
  // access only through getter and setter!
  private Territory firstSelectedTerritory;
  private Territory selectedEndpointTerritory;
  private Territory mouseCurrentTerritory;
  private Territory lastFocusedTerritory;
  private List<Territory> forced;
  private boolean nonCombat;
  private Point mouseSelectedPoint;
  private Point mouseCurrentPoint;
  private Point mouseLastUpdatePoint;
  // use a LinkedHashSet because we want to know the order
  private final Set<Unit> selectedUnits = new LinkedHashSet<>();
  private static Map<Unit, Collection<Unit>> s_dependentUnits = new HashMap<>();
  // the must move with details for the currently selected territory
  // note this is kept in sync because we do not modify selectedTerritory directly
  // instead we only do so through the private setter
  private MustMoveWithDetails mustMoveWithDetails = null;
  // cache this so we can update it only when territory/units change
  private List<Unit> unitsThatCanMoveOnRoute;
  private Image currentCursorImage;
  private Route routeCached = null;
  private String displayText = "Combat Move";
  private MoveType moveType = MoveType.DEFAULT;

  /** Creates new MovePanel. */
  public MovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map, frame);
    m_undoableMovesPanel = new UndoableMovesPanel(data, this);
    mouseCurrentTerritory = null;
    unitsThatCanMoveOnRoute = Collections.emptyList();
    currentCursorImage = null;
  }

  // Same as above! Delete this crap after refactoring.
  public static void clearDependents(final Collection<Unit> units) {
    for (final Unit unit : units) {
      if (Matches.UnitIsAirTransport.match(unit)) {
        s_dependentUnits.remove(unit);
      }
    }
  }

  @Override
  protected void clearDependencies() {
    s_dependentUnits.clear();
  }

  public void setMoveType(final MoveType moveType) {
    this.moveType = moveType;
  }

  private PlayerID getUnitOwner(final Collection<Unit> units) {
    if (BaseEditDelegate.getEditMode(getData()) && units != null && !units.isEmpty()) {
      return units.iterator().next().getOwner();
    } else {
      return getCurrentPlayer();
    }
  }

  /**
   * Sort the specified units in preferred movement or unload order.
   */
  private void sortUnitsToMove(final List<Unit> units, final Route route) {
    if (units == null || units.isEmpty()) {
      return;
    } else if (route == null) {
      final Exception nullRouteError = (new IllegalArgumentException("route is not supposed to be null"));
      ClientLogger.logQuietly(
          "Programming error, route should not be null here. Aborting sort operation and returning.", nullRouteError);
    }

    final Comparator<Unit> unitComparator;
    // sort units based on which transports are allowed to unload
    if (route.isUnload() && Match.someMatch(units, Matches.UnitIsLand)) {
      unitComparator = UnitComparator.getUnloadableUnitsComparator(units, route, getUnitOwner(units));
    } else {
      unitComparator = UnitComparator.getMovableUnitsComparator(units, route);
    }

    Collections.sort(units, unitComparator);
  }

  /**
   * Sort the specified transports in preferred load order.
   */
  private void sortTransportsToLoad(final List<Unit> transports, final Route route) {
    if (transports.isEmpty()) {
      return;
    }
    Collections.sort(transports,
        UnitComparator.getLoadableTransportsComparator(transports, route, getUnitOwner(transports)));
  }

  /**
   * Sort the specified transports in preferred unload order.
   */
  private void sortTransportsToUnload(final List<Unit> transports, final Route route) {
    if (transports.isEmpty()) {
      return;
    }
    Collections.sort(transports,
        UnitComparator.getUnloadableTransportsComparator(transports, route, getUnitOwner(transports), true));
  }

  /**
   * Return the units that are to be unloaded for this route.
   * If needed will ask the user what transports to unload.
   * This is needed because the user needs to be able to select what transports to unload
   * in the case where some transports have different movement, different
   * units etc
   */
  private Collection<Unit> getUnitsToUnload(final Route route, final Collection<Unit> unitsToUnload) {
    final Collection<Unit> allUnits = getFirstSelectedTerritory().getUnits().getUnits();
    final List<Unit> candidateUnits = Match.getMatches(allUnits, getUnloadableMatch(route, unitsToUnload));
    if (unitsToUnload.size() == candidateUnits.size()) {
      return unitsToUnload;
    }
    final List<Unit> candidateTransports =
        Match.getMatches(allUnits, Matches.unitIsTransportingSomeCategories(candidateUnits));

    // Remove all incapable transports
    final Collection<Unit> incapableTransports =
        Match.getMatches(candidateTransports, Matches.transportCannotUnload(route.getEnd()));
    candidateTransports.removeAll(incapableTransports);
    if (candidateTransports.size() == 0) {
      return Collections.emptyList();
    }

    // Just one transport, don't bother to ask
    if (candidateTransports.size() == 1) {
      return unitsToUnload;
    }

    // Are the transports all of the same type and if they are, then don't ask
    final Collection<UnitCategory> categories =
        UnitSeperator.categorize(candidateTransports, mustMoveWithDetails.getMustMoveWith(), true, false);
    if (categories.size() == 1) {
      return unitsToUnload;
    }
    sortTransportsToUnload(candidateTransports, route);

    // unitsToUnload are actually dependents, but need to select transports
    final Set<Unit> defaultSelections = TransportUtils.findMinTransportsToUnload(unitsToUnload, candidateTransports);

    // Match criteria to ensure that chosen transports will match selected units
    final Match<Collection<Unit>> transportsToUnloadMatch = new Match<Collection<Unit>>() {
      @Override
      public boolean match(final Collection<Unit> units) {
        final List<Unit> sortedTransports = Match.getMatches(units, Matches.UnitIsTransport);
        final Collection<Unit> availableUnits = new ArrayList<>(unitsToUnload);

        // track the changing capacities of the transports as we assign units
        final IntegerMap<Unit> capacityMap = new IntegerMap<>();
        for (final Unit transport : sortedTransports) {
          final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
          capacityMap.add(transport, TransportUtils.getTransportCost(transporting));
        }
        boolean hasChanged = false;
        final Comparator<Unit> increasingCapacityComparator =
            UnitComparator.getIncreasingCapacityComparator(sortedTransports);

        // This algorithm will ensure that it is actually possible to distribute
        // the selected units amongst the current selection of chosen transports.
        do {
          hasChanged = false;

          // Sort transports by increasing capacity
          Collections.sort(sortedTransports, increasingCapacityComparator);

          // Try to remove one unit from each transport, in succession
          final Iterator<Unit> transportIter = sortedTransports.iterator();
          while (transportIter.hasNext()) {
            final Unit transport = transportIter.next();
            final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
            if (transporting == null) {
              continue;
            }
            final Collection<UnitCategory> transCategories = UnitSeperator.categorize(transporting);
            final Iterator<Unit> unitIter = availableUnits.iterator();
            while (unitIter.hasNext()) {
              final Unit unit = unitIter.next();
              final Collection<UnitCategory> unitCategory = UnitSeperator.categorize(Collections.singleton(unit));

              // Is one of the transported units of the same type we want to unload?
              if (Util.someIntersect(transCategories, unitCategory)) {

                // Unload the unit, remove the transport from our list, and continue
                hasChanged = true;
                unitIter.remove();
                transportIter.remove();
                break;
              }
            }
          }
          // Repeat until there are no units left or no changes occur
        } while (availableUnits.size() > 0 && hasChanged);

        // If we haven't seen all of the transports (and removed them) then there are extra transports that don't fit
        return (sortedTransports.size() == 0);
      }
    };

    // Choosing what transports to unload
    final UnitChooser chooser = new UnitChooser(candidateTransports, defaultSelections,
        mustMoveWithDetails.getMustMoveWith(), /* categorizeMovement */true, /* categorizeTransportCost */false,
        getGameData(), /* allowTwoHit */false, getMap().getUIContext(), transportsToUnloadMatch);
    chooser.setTitle("What transports do you want to unload");
    final int option =
        JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to unload",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if (option != JOptionPane.OK_OPTION) {
      return Collections.emptyList();
    }
    final Collection<Unit> chosenTransports = Match.getMatches(chooser.getSelected(), Matches.UnitIsTransport);
    final List<Unit> allUnitsInSelectedTransports = new ArrayList<>();
    for (final Unit transport : chosenTransports) {
      final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
      if (transporting != null) {
        allUnitsInSelectedTransports.addAll(transporting);
      }
    }
    allUnitsInSelectedTransports.retainAll(candidateUnits);
    sortUnitsToMove(allUnitsInSelectedTransports, route);
    final List<Unit> rVal = new ArrayList<>();
    final List<Unit> sortedTransports = new ArrayList<>(chosenTransports);
    Collections.sort(sortedTransports, UnitComparator.getIncreasingCapacityComparator(sortedTransports));
    final Collection<Unit> selectedUnits = new ArrayList<>(unitsToUnload);

    // First pass: choose one unit from each selected transport
    for (final Unit transport : sortedTransports) {
      boolean hasChanged = false;
      final Iterator<Unit> selectedIter = selectedUnits.iterator();
      while (selectedIter.hasNext()) {
        final Unit selected = selectedIter.next();
        final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
        for (final Unit candidate : transporting) {
          if (selected.getType().equals(candidate.getType()) && selected.getOwner().equals(candidate.getOwner())
              && selected.getHits() == candidate.getHits()) {
            hasChanged = true;
            rVal.add(candidate);
            allUnitsInSelectedTransports.remove(candidate);
            selectedIter.remove();
            break;
          }
        }
        if (hasChanged) {
          break;
        }
      }
    }

    // Now fill remaining slots in preferred unit order
    for (final Unit selected : selectedUnits) {
      final Iterator<Unit> candidateIter = allUnitsInSelectedTransports.iterator();
      while (candidateIter.hasNext()) {
        final Unit candidate = candidateIter.next();
        if (selected.getType().equals(candidate.getType()) && selected.getOwner().equals(candidate.getOwner())
            && selected.getHits() == candidate.getHits()) {
          rVal.add(candidate);
          candidateIter.remove();
          break;
        }
      }
    }
    return rVal;
  }

  private CompositeMatch<Unit> getUnloadableMatch(final Route route, final Collection<Unit> units) {
    final CompositeMatch<Unit> unloadable = new CompositeMatchAnd<>();
    unloadable.add(getMovableMatch(route, units));
    unloadable.add(Matches.UnitIsLand);
    return unloadable;
  }

  private CompositeMatch<Unit> getMovableMatch(final Route route, final Collection<Unit> units) {
    final CompositeMatch<Unit> movable = new CompositeMatchAnd<>();
    if (!BaseEditDelegate.getEditMode(getData())) {
      movable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
    }
    /*
     * if you do not have selection of zero-movement units enabled,
     * this will restrict selection to units with 1 or more movement
     */
    if (!games.strategy.triplea.Properties.getSelectableZeroMovementUnits(getData())) {
      movable.add(Matches.UnitCanMove);
    }
    if (!nonCombat) {
      movable.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
    }
    if (route != null) {
      final Match<Unit> enoughMovement = new Match<Unit>() {
        @Override
        public boolean match(final Unit u) {
          if (BaseEditDelegate.getEditMode(getData())) {
            return true;
          }
          return TripleAUnit.get(u).getMovementLeft() >= route.getMovementCost(u);
        }
      };
      if (route.isUnload()) {
        final CompositeMatch<Unit> landOrCanMove = new CompositeMatchOr<>();
        landOrCanMove.add(Matches.UnitIsLand);
        final CompositeMatch<Unit> notLandAndCanMove = new CompositeMatchAnd<>();
        notLandAndCanMove.add(enoughMovement);
        notLandAndCanMove.add(Matches.UnitIsNotLand);
        landOrCanMove.add(notLandAndCanMove);
        movable.add(landOrCanMove);
      } else {
        movable.add(enoughMovement);
      }
    }
    if (route != null && route.getEnd() != null) {
      final boolean water = route.getEnd().isWater();
      if (water && !route.isLoad()) {
        movable.add(Matches.UnitIsNotLand);
      }
      if (!water) {
        movable.add(Matches.UnitIsNotSea);
      }
    }
    if (units != null && !units.isEmpty()) {
      // force all units to have the same owner in edit mode
      final PlayerID owner = getUnitOwner(units);
      if (BaseEditDelegate.getEditMode(getData())) {
        movable.add(Matches.unitIsOwnedBy(owner));
      }
      final CompositeMatch<Unit> rightUnitTypeMatch = new CompositeMatchOr<>();
      for (final Unit unit : units) {
        if (unit.getOwner().equals(owner)) {
          rightUnitTypeMatch.add(Matches.unitIsOfType(unit.getType()));
        }
      }
      movable.add(rightUnitTypeMatch);
    }
    return movable;
  }

  private Route getRoute(final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    getData().acquireReadLock();
    try {
      if (forced == null) {
        return getRouteNonForced(start, end, selectedUnits);
      } else {
        return getRouteForced(start, end, selectedUnits);
      }
    } finally {
      getData().releaseReadLock();
    }
  }

  /**
   * Get the route including the territories that we are forced to move through.
   */
  private Route getRouteForced(final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    if (forced == null || forced.size() == 0) {
      throw new IllegalStateException("No forced territories:" + forced + " end:" + end + " start:" + start);
    }
    final Iterator<Territory> iter = forced.iterator();
    Territory last = getFirstSelectedTerritory();
    Territory current = null;
    Route total = new Route();
    total.setStart(last);
    while (iter.hasNext()) {
      current = iter.next();
      final Route add = getData().getMap().getRoute(last, current);
      final Route newTotal = Route.join(total, add);
      if (newTotal == null) {
        return total;
      }
      total = newTotal;
      last = current;
    }
    if (!end.equals(last)) {
      final Route add = getRouteNonForced(last, end, selectedUnits);
      final Route newTotal = Route.join(total, add);
      if (newTotal != null) {
        total = newTotal;
      }
    }
    return total;
  }

  /**
   * Get the route ignoring forced territories.
   */
  private Route getRouteNonForced(final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    // can't rely on current player being the unit owner in Edit Mode
    // look at the units being moved to determine allies and enemies
    final PlayerID owner = getUnitOwner(selectedUnits);
    return MoveValidator.getBestRoute(start, end, getData(), owner, selectedUnits,
        !GameStepPropertiesHelper.isAirborneMove(getData()));
  }

  private void updateUnitsThatCanMoveOnRoute(final Collection<Unit> units, final Route route) {
    if (route == null || route.hasNoSteps()) {
      clearStatusMessage();
      getMap().showMouseCursor();
      currentCursorImage = null;
      unitsThatCanMoveOnRoute = new ArrayList<>(units);
      return;
    }
    getMap().hideMouseCursor();
    // TODO kev check for already loaded airTransports
    Collection<Unit> transportsToLoad = Collections.emptyList();
    if (MoveValidator.isLoad(units, s_dependentUnits, route, getData(), getCurrentPlayer())) {
      transportsToLoad = route.getEnd().getUnits().getMatches(
          new CompositeMatchAnd<>(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), getData())));
    }
    List<Unit> best = new ArrayList<>(units);
    // if the player selects a land unit and other units
    // when the
    // only consider the non land units
    if (route.getStart().isWater() && route.getEnd() != null && route.getEnd().isWater() && !route.isLoad()) {
      best = Match.getMatches(best, new InverseMatch<>(Matches.UnitIsLand));
    }
    sortUnitsToMove(best, route);
    Collections.reverse(best);
    List<Unit> bestWithDependents = addMustMoveWith(best);
    MoveValidationResult allResults;
    getData().acquireReadLock();
    try {
      allResults = AbstractMoveDelegate.validateMove(moveType, bestWithDependents, route, getCurrentPlayer(),
          transportsToLoad, s_dependentUnits, nonCombat, getUndoableMoves(), getData());
    } finally {
      getData().releaseReadLock();
    }
    MoveValidationResult lastResults = allResults;
    if (!allResults.isMoveValid()) {
      // if the player is invading only consider units that can invade
      if (!nonCombat && route.isUnload()
          && Matches.isTerritoryEnemy(getCurrentPlayer(), getData()).match(route.getEnd())) {
        best = Match.getMatches(best, Matches.UnitCanInvade);
        bestWithDependents = addMustMoveWith(best);
        lastResults = AbstractMoveDelegate.validateMove(moveType, bestWithDependents, route, getCurrentPlayer(),
            transportsToLoad, s_dependentUnits, nonCombat, getUndoableMoves(), getData());
      }
      while (!best.isEmpty() && !lastResults.isMoveValid()) {
        best = best.subList(1, best.size());
        bestWithDependents = addMustMoveWith(best);
        lastResults = AbstractMoveDelegate.validateMove(moveType, bestWithDependents, route, getCurrentPlayer(),
            transportsToLoad, s_dependentUnits, nonCombat, getUndoableMoves(), getData());
      }
    }
    if (allResults.isMoveValid()) {
      // valid move
      if (bestWithDependents.containsAll(selectedUnits)) {
        clearStatusMessage();
        currentCursorImage = null;
      } else {
        setStatusWarningMessage("Not all units can move there");
        currentCursorImage = getMap().getWarningImage().orElse(null);
      }
    } else {
      String message = allResults.getError();
      if (message == null) {
        message = allResults.getDisallowedUnitWarning(0);
      }
      if (message == null) {
        message = allResults.getUnresolvedUnitWarning(0);
      }
      if (!lastResults.isMoveValid()) {
        setStatusErrorMessage(message);
        currentCursorImage = getMap().getErrorImage().orElse(null);
      } else {
        setStatusWarningMessage(message);
        currentCursorImage = getMap().getWarningImage().orElse(null);
      }
    }
    if (unitsThatCanMoveOnRoute.size() != new HashSet<>(unitsThatCanMoveOnRoute).size()) {
      cancelMove();
      return;
    }
    unitsThatCanMoveOnRoute = new ArrayList<>(bestWithDependents);
  }

  private List<Unit> addMustMoveWith(final List<Unit> best) {
    final List<Unit> bestWithDependents = new ArrayList<>(best);
    for (final Unit u : best) {
      if (mustMoveWithDetails.getMustMoveWith().containsKey(u)) {
        final Collection<Unit> mustMoveWith = mustMoveWithDetails.getMustMoveWith().get(u);
        if (mustMoveWith != null) {
          for (final Unit m : mustMoveWith) {
            if (!bestWithDependents.contains(m)) {
              bestWithDependents.addAll(mustMoveWith);
            }
          }
        }
      }
    }
    return bestWithDependents;
  }

  /**
   * Route can be null.
   */
  final void updateRouteAndMouseShadowUnits(final Route route) {
    routeCached = route;
    getMap().setRoute(route, mouseSelectedPoint, mouseCurrentPoint, currentCursorImage);
    if (route == null) {
      getMap().setMouseShadowUnits(null);
    } else {
      getMap().setMouseShadowUnits(unitsThatCanMoveOnRoute);
    }
  }

  /**
   * Allow the user to select what transports to load.
   * If null is returned, the move should be canceled.
   */
  private Collection<Unit> getTransportsToLoad(final Route route, final Collection<Unit> unitsToLoad,
      final boolean disablePrompts) {
    if (!route.isLoad()) {
      return Collections.emptyList();
    }
    if (Match.someMatch(unitsToLoad, Matches.UnitIsAir)) {
      return Collections.emptyList();
    }
    final Collection<Unit> endOwnedUnits = route.getEnd().getUnits().getUnits();
    final PlayerID unitOwner = getUnitOwner(unitsToLoad);
    final MustMoveWithDetails endMustMoveWith =
        MoveValidator.getMustMoveWith(route.getEnd(), endOwnedUnits, s_dependentUnits, getData(), unitOwner);
    int minTransportCost = s_defaultMinTransportCost;
    for (final Unit unit : unitsToLoad) {
      minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
    }
    final CompositeMatch<Unit> candidateTransportsMatch = new CompositeMatchAnd<>();
    candidateTransportsMatch.add(Matches.UnitIsTransport);
    candidateTransportsMatch.add(Matches.alliedUnit(unitOwner, getGameData()));
    final List<Unit> candidateTransports = Match.getMatches(endOwnedUnits, candidateTransportsMatch);

    // remove transports that don't have enough capacity
    final Iterator<Unit> transportIter = candidateTransports.iterator();
    while (transportIter.hasNext()) {
      final Unit transport = transportIter.next();
      final int capacity = TransportTracker.getAvailableCapacity(transport);
      if (capacity < minTransportCost) {
        transportIter.remove();
      }
    }

    // nothing to choose
    if (candidateTransports.isEmpty()) {
      return Collections.emptyList();
    }

    // sort transports in preferred load order
    sortTransportsToLoad(candidateTransports, route);
    final List<Unit> availableUnits = new ArrayList<>(unitsToLoad);
    final IntegerMap<Unit> availableCapacityMap = new IntegerMap<>();
    for (final Unit transport : candidateTransports) {
      final int capacity = TransportTracker.getAvailableCapacity(transport);
      availableCapacityMap.put(transport, capacity);
    }
    final Set<Unit> defaultSelections = new HashSet<>();

    // Algorithm to choose defaultSelections (transports to load)
    // We are trying to determine which transports are the best defaults to select for loading,
    // and so we need a modified algorithm based strictly on candidateTransports order:
    // - owned, capable transports are chosen first; attempt to fill them
    // - allied, capable transports are chosen next; attempt to fill them
    // - finally, incapable transports are chosen last (will generate errors)
    // Note that if any allied transports qualify as defaults, we will always prompt with a
    // UnitChooser later on so that it is obvious to the player.
    boolean useAlliedTransports = false;
    final Collection<Unit> capableTransports = new ArrayList<>(candidateTransports);

    // only allow incapable transports for updateUnitsThatCanMoveOnRoute
    // so that we can have a nice UI error shown if these transports
    // are selected, since it may not be obvious
    final Collection<Unit> incapableTransports =
        Match.getMatches(capableTransports, Matches.transportCannotUnload(route.getEnd()));
    capableTransports.removeAll(incapableTransports);
    final Match<Unit> alliedMatch = new Match<Unit>() {
      @Override
      public boolean match(final Unit transport) {
        return (!transport.getOwner().equals(unitOwner));
      }
    };
    final Collection<Unit> alliedTransports = Match.getMatches(capableTransports, alliedMatch);
    capableTransports.removeAll(alliedTransports);

    // First, load capable transports
    final Map<Unit, Unit> unitsToCapableTransports =
        TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, capableTransports);
    for (final Unit unit : unitsToCapableTransports.keySet()) {
      final Unit transport = unitsToCapableTransports.get(unit);
      final int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
      availableCapacityMap.add(transport, (-1 * unitCost));
      defaultSelections.add(transport);
    }
    availableUnits.removeAll(unitsToCapableTransports.keySet());

    // Next, load allied transports
    final Map<Unit, Unit> unitsToAlliedTransports =
        TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, alliedTransports);
    for (final Unit unit : unitsToAlliedTransports.keySet()) {
      final Unit transport = unitsToAlliedTransports.get(unit);
      final int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
      availableCapacityMap.add(transport, (-1 * unitCost));
      defaultSelections.add(transport);
      useAlliedTransports = true;
    }
    availableUnits.removeAll(unitsToAlliedTransports.keySet());

    // only allow incapable transports for updateUnitsThatCanMoveOnRoute
    // so that we can have a nice UI error shown if these transports
    // are selected, since it may not be obvious
    if (getSelectedEndpointTerritory() == null) {
      final Map<Unit, Unit> unitsToIncapableTransports =
          TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, incapableTransports);
      for (final Unit unit : unitsToIncapableTransports.keySet()) {
        final Unit transport = unitsToIncapableTransports.get(unit);
        final int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
        availableCapacityMap.add(transport, (-1 * unitCost));
        defaultSelections.add(transport);
      }
      availableUnits.removeAll(unitsToIncapableTransports.keySet());
    } else {
      candidateTransports.removeAll(incapableTransports);
    }

    // return defaults if we aren't allowed to prompt
    if (disablePrompts) {
      return defaultSelections;
    }

    // force UnitChooser to pop up if we are choosing allied transports
    if (!useAlliedTransports) {
      if (candidateTransports.size() == 1) {
        return candidateTransports;
      }
      // all the same type, dont ask unless we have more than 1 unit type
      if (UnitSeperator.categorize(candidateTransports, endMustMoveWith.getMustMoveWith(), true, false).size() == 1
          && unitsToLoad.size() == 1) {
        return candidateTransports;
      }
      // If we've filled all transports, then no user intervention is required.
      // It is possible to make "wrong" decisions if there are mixed unit types and
      // mixed transport categories, but there is no UI to manage that anyway.
      // Players will need to load incrementally in such cases.
      if (defaultSelections.containsAll(candidateTransports)) {
        return candidateTransports;
      }
    }

    // the match criteria to ensure that chosen transports will match selected units
    final Match<Collection<Unit>> transportsToLoadMatch = new Match<Collection<Unit>>() {
      @Override
      public boolean match(final Collection<Unit> units) {
        final Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
        // prevent too many transports from being selected
        return (transports.size() <= Math.min(unitsToLoad.size(), candidateTransports.size()));
      }
    };
    final UnitChooser chooser = new UnitChooser(candidateTransports, defaultSelections,
        endMustMoveWith.getMustMoveWith(), /* categorizeMovement */true, /* categorizeTransportCost */false,
        getGameData(), /* allowTwoHit */false, getMap().getUIContext(), transportsToLoadMatch);
    chooser.setTitle("What transports do you want to load");
    final int option =
        JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to load",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if (option != JOptionPane.OK_OPTION) {
      return Collections.emptyList();
    }
    return chooser.getSelected(false);
  }

  private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener() {
    @Override
    public void unitsSelected(final List<Unit> units, final Territory t, final MouseDetails me) {
      if (!getListening()) {
        return;
      }
      // check if we can handle this event, are we active?
      if (!getActive()) {
        return;
      }
      if (t == null) {
        return;
      }
      final boolean rightMouse = me.isRightButton();
      final boolean isMiddleMouseButton = me.getButton() == MouseEvent.BUTTON2;
      final boolean noSelectedTerritory = (firstSelectedTerritory == null);
      final boolean isFirstSelectedTerritory = (firstSelectedTerritory == t);
      // select units
      final GameData data = getData();
      data.acquireReadLock();
      try {
        // de select units
        if (rightMouse && !noSelectedTerritory && !m_map.wasLastActionDraggingAndReset()) {
          deselectUnits(units, t, me);
        } else if (!isMiddleMouseButton && !rightMouse && (noSelectedTerritory || isFirstSelectedTerritory)) {
          selectUnitsToMove(units, t, me);
        } else if (!rightMouse && me.isControlDown() && !isFirstSelectedTerritory) {
          selectWayPoint(t);
        } else if (!rightMouse && !noSelectedTerritory && !isFirstSelectedTerritory && !isMiddleMouseButton) {
          selectEndPoint(t);
        }
      } finally {
        data.releaseReadLock();
      }
      getMap().requestFocusInWindow();
    }

    private void selectUnitsToMove(final List<Unit> units, final Territory t, final MouseDetails me) {
      // are any of the units ours, note - if no units selected thats still ok
      if (!BaseEditDelegate.getEditMode(getData()) || !selectedUnits.isEmpty()) {
        for (final Unit unit : units) {
          if (!unit.getOwner().equals(getUnitOwner(selectedUnits))) {
            return;
          }
        }
      }
      // basic match criteria only
      final CompositeMatch<Unit> unitsToMoveMatch = getMovableMatch(null, null);
      final Match<Collection<Unit>> ownerMatch = new Match<Collection<Unit>>() {
        @Override
        public boolean match(final Collection<Unit> unitsToCheck) {
          final PlayerID owner = unitsToCheck.iterator().next().getOwner();
          for (final Unit unit : unitsToCheck) {
            if (!owner.equals(unit.getOwner())) {
              return false;
            }
          }
          return true;
        }
      };
      if (units.isEmpty() && selectedUnits.isEmpty()) {
        if (!me.isShiftDown()) {
          final List<Unit> unitsToMove = t.getUnits().getMatches(unitsToMoveMatch);
          if (unitsToMove.isEmpty()) {
            return;
          }
          final String text = "Select units to move from " + t.getName();
          UnitChooser chooser;
          if (BaseEditDelegate.getEditMode(getData()) && !Match
              .getMatches(unitsToMove, Matches.unitIsOwnedBy(getUnitOwner(unitsToMove))).containsAll(unitsToMove)) {
            // use matcher to prevent units of different owners being chosen
            chooser = new UnitChooser(unitsToMove, selectedUnits, /* mustMoveWith */null,
                /* categorizeMovement */false, /* categorizeTransportCost */false, getData(), /* allowTwoHit */false,
                getMap().getUIContext(), ownerMatch);
          } else {
            chooser =
                new UnitChooser(unitsToMove, selectedUnits, /* mustMoveWith */null, /* categorizeMovement */false,
                    /* categorizeTransportCost */false, getData(), /* allowTwoHit */false, getMap().getUIContext());
          }
          final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text,
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
          if (option != JOptionPane.OK_OPTION) {
            return;
          }
          if (chooser.getSelected(false).isEmpty()) {
            return;
          }
          selectedUnits.addAll(chooser.getSelected(false));
        }
      }
      if (getFirstSelectedTerritory() == null) {
        setFirstSelectedTerritory(t);
        mouseSelectedPoint = me.getMapPoint();
        mouseCurrentPoint = me.getMapPoint();
        enableCancelButton();
      }
      if (!getFirstSelectedTerritory().equals(t)) {
        throw new IllegalStateException("Wrong selected territory");
      }
      // add all
      if (me.isShiftDown()) {
        // prevent units of multiple owners from being chosen in edit mode
        final CompositeMatch<Unit> ownedNotFactory = new CompositeMatchAnd<>();
        if (!BaseEditDelegate.getEditMode(getData())) {
          ownedNotFactory.add(unitsToMoveMatch);
        } else if (!selectedUnits.isEmpty()) {
          ownedNotFactory.add(unitsToMoveMatch);
          ownedNotFactory.add(Matches.unitIsOwnedBy(getUnitOwner(selectedUnits)));
        } else {
          ownedNotFactory.add(unitsToMoveMatch);
          ownedNotFactory.add(Matches.unitIsOwnedBy(getUnitOwner(t.getUnits().getUnits())));
        }
        selectedUnits.addAll(t.getUnits().getMatches(ownedNotFactory));
      } else if (me.isControlDown()) {
        selectedUnits.addAll(Match.getMatches(units, unitsToMoveMatch));
      } else { // add one
        // best candidate unit for route is chosen dynamically later
        // check for alt key - add 1/10 of total units (useful for splitting large armies)
        final List<Unit> unitsToMove = Match.getMatches(units, unitsToMoveMatch);
        Collections.sort(unitsToMove, UnitComparator.getHighestToLowestMovementComparator());

        final int iterCount = (me.isAltDown()) ? s_deselectNumber : 1;

        int addCount = 0;
        for (final Unit unit : unitsToMove) {
          if (!selectedUnits.contains(unit)) {
            selectedUnits.add(unit);
            addCount++;
            if (addCount >= iterCount) {
              break;
            }
          }
        }
      }
      if (!selectedUnits.isEmpty()) {
        mouseLastUpdatePoint = me.getMapPoint();
        final Route route = getRoute(getFirstSelectedTerritory(), t, selectedUnits);
        // Load Bombers with paratroops
        if ((!nonCombat || IsParatroopersCanMoveDuringNonCombat(getData()))
            && TechAttachment.isAirTransportable(getCurrentPlayer())
            && Match.someMatch(selectedUnits,
                new CompositeMatchAnd<>(Matches.UnitIsAirTransport, Matches.unitHasNotMoved))) {
          final PlayerID player = getCurrentPlayer();
          // TODO Transporting allied units
          // Get the potential units to load
          final CompositeMatch<Unit> unitsToLoadMatch = new CompositeMatchAnd<>();
          unitsToLoadMatch.add(Matches.UnitIsAirTransportable);
          unitsToLoadMatch.add(Matches.unitIsOwnedBy(player));
          unitsToLoadMatch.add(Matches.unitHasNotMoved);
          final Collection<Unit> unitsToLoad =
              Match.getMatches(route.getStart().getUnits().getUnits(), unitsToLoadMatch);
          unitsToLoad.removeAll(selectedUnits);
          for (final Unit u : s_dependentUnits.keySet()) {
            unitsToLoad.removeAll(s_dependentUnits.get(u));
          }
          // Get the potential air transports to load
          final CompositeMatch<Unit> candidateAirTransportsMatch = new CompositeMatchAnd<>();
          candidateAirTransportsMatch.add(Matches.UnitIsAirTransport);
          candidateAirTransportsMatch.add(Matches.unitIsOwnedBy(player));
          candidateAirTransportsMatch.add(Matches.unitHasNotMoved);
          candidateAirTransportsMatch.add(Matches.transportIsNotTransporting());
          final Collection<Unit> candidateAirTransports =
              Match.getMatches(t.getUnits().getMatches(unitsToMoveMatch), candidateAirTransportsMatch);
          // candidateAirTransports.removeAll(selectedUnits);
          candidateAirTransports.removeAll(s_dependentUnits.keySet());
          if (unitsToLoad.size() > 0 && candidateAirTransports.size() > 0) {
            final Collection<Unit> airTransportsToLoad = getAirTransportsToLoad(candidateAirTransports);
            selectedUnits.addAll(airTransportsToLoad);
            if (!airTransportsToLoad.isEmpty()) {
              final Collection<Unit> loadedAirTransports =
                  getLoadedAirTransports(route, unitsToLoad, airTransportsToLoad, player);
              selectedUnits.addAll(loadedAirTransports);
              final MoveDescription message =
                  new MoveDescription(loadedAirTransports, route, airTransportsToLoad, s_dependentUnits);
              setMoveMessage(message);
            }
          }
        }
        updateUnitsThatCanMoveOnRoute(selectedUnits, route);
        updateRouteAndMouseShadowUnits(route);
      } else {
        setFirstSelectedTerritory(null);
      }
    }

    public Collection<Unit> getAirTransportsToLoad(final Collection<Unit> candidateAirTransports) {
      final Set<Unit> defaultSelections = new HashSet<>();
      // prevent too many bombers from being selected
      final Match<Collection<Unit>> transportsToLoadMatch = new Match<Collection<Unit>>() {
        @Override
        public boolean match(final Collection<Unit> units) {
          final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
          return (airTransports.size() <= candidateAirTransports.size());
        }
      };
      // Allow player to select which to load.
      final UnitChooser chooser = new UnitChooser(candidateAirTransports, defaultSelections, s_dependentUnits,
          /* categorizeMovement */true, /* categorizeTransportCost */false, getGameData(), /* allowTwoHit */false,
          getMap().getUIContext(), transportsToLoadMatch);
      chooser.setTitle("Select air transports to load");
      final int option =
          JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to load",
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
      if (option != JOptionPane.OK_OPTION) {
        return Collections.emptyList();
      }
      return chooser.getSelected(true);
    }

    /**
     * Allow the user to select what units to load.
     * If null is returned, the move should be canceled.
     */
    public Collection<Unit> getLoadedAirTransports(final Route route, final Collection<Unit> capableUnitsToLoad,
        final Collection<Unit> capableTransportsToLoad, final PlayerID player) {
      // Get the minimum transport cost of a candidate unit
      int minTransportCost = Integer.MAX_VALUE;
      for (final Unit unit : capableUnitsToLoad) {
        minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
      }
      final Collection<Unit> airTransportsToLoad = new ArrayList<>();
      for (final Unit bomber : capableTransportsToLoad) {
        final int capacity = TransportTracker.getAvailableCapacity(bomber);
        if (capacity >= minTransportCost) {
          airTransportsToLoad.add(bomber);
        }
      }
      // If no airTransports can be loaded, return the empty set
      if (airTransportsToLoad.isEmpty()) {
        return airTransportsToLoad;
      }
      final Set<Unit> defaultSelections = new HashSet<>();
      // Check to see if there's room for the selected units
      final Match<Collection<Unit>> unitsToLoadMatch = new Match<Collection<Unit>>() {
        @Override
        public boolean match(final Collection<Unit> units) {
          final Collection<Unit> unitsToLoad = Match.getMatches(units, Matches.UnitIsAirTransportable);
          final Map<Unit, Unit> unitMap = TransportUtils.mapTransportsToLoad(unitsToLoad, airTransportsToLoad);
          boolean ableToLoad = true;
          for (final Unit unit : unitsToLoad) {
            if (!unitMap.keySet().contains(unit)) {
              ableToLoad = false;
            }
          }
          return ableToLoad;
        }
      };
      List<Unit> loadedUnits = new ArrayList<>(capableUnitsToLoad);
      if (!airTransportsToLoad.isEmpty()) {
        // Get a list of the units that could be loaded on the transport (based upon transport capacity)
        final List<Unit> unitsToLoad =
            TransportUtils.findUnitsToLoadOnAirTransports(capableUnitsToLoad, airTransportsToLoad);
        final String title = "Load air transports";
        final String action = "load";
        loadedUnits = UserChooseUnits(defaultSelections, unitsToLoadMatch, unitsToLoad, title, action);
        final Map<Unit, Unit> mapping = TransportUtils.mapTransportsToLoad(loadedUnits, airTransportsToLoad);
        for (final Unit unit : mapping.keySet()) {
          final Collection<Unit> unitsColl = new ArrayList<>();
          unitsColl.add(unit);
          final Unit airTransport = mapping.get(unit);
          if (s_dependentUnits.containsKey(airTransport)) {
            unitsColl.addAll(s_dependentUnits.get(airTransport));
          }
          s_dependentUnits.put(airTransport, unitsColl);
          mustMoveWithDetails = MoveValidator.getMustMoveWith(route.getStart(),
              route.getStart().getUnits().getUnits(), s_dependentUnits, getData(), player);
        }
      }
      return loadedUnits;
    }

    private void deselectUnits(List<Unit> units, final Territory t, final MouseDetails me) {
      final Collection<Unit> unitsToRemove = new ArrayList<>(selectedUnits.size());
      // we have right clicked on a unit stack in a different territory
      if (!getFirstSelectedTerritory().equals(t)) {
        units = Collections.emptyList();
      }
      // remove the dependent units so we don't have to micromanage them
      final List<Unit> unitsWithoutDependents = new ArrayList<>(selectedUnits);
      for (final Unit unit : selectedUnits) {
        final Collection<Unit> forced = mustMoveWithDetails.getMustMoveWith().get(unit);
        if (forced != null) {
          unitsWithoutDependents.removeAll(forced);
        }
      }
      // no unit selected, remove the most recent, but skip dependents
      if (units.isEmpty()) {
        if (me.isControlDown()) {
          selectedUnits.clear();
          // Clear the stored dependents for AirTransports
          if (!s_dependentUnits.isEmpty()) {
            s_dependentUnits.clear();
          }
        } else if (!unitsWithoutDependents.isEmpty()) {
          // check for alt key - remove 1/10 of total units (useful for splitting large armies)
          final int iterCount = (me.isAltDown()) ? s_deselectNumber : 1;
          // remove the last iterCount elements
          for (int i = 0; i < iterCount; i++) {
            unitsToRemove.add(unitsWithoutDependents.get(unitsWithoutDependents.size() - 1));
            // Clear the stored dependents for AirTransports
            if (!s_dependentUnits.isEmpty()) {
              for (final Unit airTransport : unitsWithoutDependents) {
                if (s_dependentUnits.containsKey(airTransport)) {
                  unitsToRemove.addAll(s_dependentUnits.get(airTransport));
                  s_dependentUnits.remove(airTransport);
                }
              }
            }
          }
        }
      } else { // we have actually clicked on a specific unit
        // remove all if control is down
        if (me.isControlDown()) {
          unitsToRemove.addAll(units);
          // Clear the stored dependents for AirTransports
          if (!s_dependentUnits.isEmpty()) {
            for (final Unit airTransport : unitsWithoutDependents) {
              if (s_dependentUnits.containsKey(airTransport)) {
                unitsToRemove.addAll(s_dependentUnits.get(airTransport));
                s_dependentUnits.remove(airTransport);
              }
            }
          }
        } else { // remove one
          if (!getFirstSelectedTerritory().equals(t)) {
            throw new IllegalStateException("Wrong selected territory");
          }
          // doesn't matter which unit we remove since units are assigned to routes later
          // check for alt key - remove 1/10 of total units (useful for splitting large armies)
          // changed to just remove 10 units
          // (int) Math.max(1, Math.floor(units.size() / s_deselectNumber))
          final int iterCount = (me.isAltDown()) ? s_deselectNumber : 1;
          int remCount = 0;
          for (final Unit unit : units) {
            if (selectedUnits.contains(unit) && !unitsToRemove.contains(unit)) {
              unitsToRemove.add(unit);
              // Clear the stored dependents for AirTransports
              if (!s_dependentUnits.isEmpty()) {
                for (final Unit airTransport : unitsWithoutDependents) {
                  if (s_dependentUnits.containsKey(airTransport)) {
                    s_dependentUnits.get(airTransport).remove(unit);
                  }
                }
              }
              remCount++;
              if (remCount >= iterCount) {
                break;
              }
            }
          }
        }
      }
      // perform the remove
      selectedUnits.removeAll(unitsToRemove);
      if (selectedUnits.isEmpty()) {
        // nothing left, cancel move
        cancelMove();
      } else {
        mouseLastUpdatePoint = me.getMapPoint();
        updateUnitsThatCanMoveOnRoute(selectedUnits, getRoute(getFirstSelectedTerritory(), t, selectedUnits));
        updateRouteAndMouseShadowUnits(getRoute(getFirstSelectedTerritory(), t, selectedUnits));
      }
    }

    private void selectWayPoint(final Territory territory) {
      if (forced == null) {
        forced = new ArrayList<>();
      }
      if (!forced.contains(territory)) {
        forced.add(territory);
      }
      updateRouteAndMouseShadowUnits(
          getRoute(getFirstSelectedTerritory(), getFirstSelectedTerritory(), selectedUnits));
    }

    private CompositeMatch<Unit> getUnloadableMatch() {
      // are we unloading everything? if we are then we dont need to select the transports
      final CompositeMatch<Unit> unloadable = new CompositeMatchAnd<>();
      unloadable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
      unloadable.add(Matches.UnitIsLand);
      if (nonCombat) {
        unloadable.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
      }
      return unloadable;
    }

    private void selectEndPoint(final Territory territory) {
      final Route route = getRoute(getFirstSelectedTerritory(), territory, selectedUnits);
      final List<Unit> units = unitsThatCanMoveOnRoute;
      setSelectedEndpointTerritory(territory);
      if (units.isEmpty() || route == null) {
        cancelMove();
        return;
      }
      Collection<Unit> transports = null;
      final CompositeMatch<Unit> paratroopNBombers = new CompositeMatchAnd<>();
      paratroopNBombers.add(Matches.UnitIsAirTransport);
      paratroopNBombers.add(Matches.UnitIsAirTransportable);
      final boolean paratroopsLanding = Match.someMatch(units, paratroopNBombers);
      if (route.isLoad() && Match.someMatch(units, Matches.UnitIsLand)) {
        transports = getTransportsToLoad(route, units, false);
        if (transports.isEmpty()) {
          cancelMove();
          return;
        }
      } else if ((route.isUnload() && Match.someMatch(units, Matches.UnitIsLand)) || paratroopsLanding) {
        final List<Unit> unloadAble = Match.getMatches(selectedUnits, getUnloadableMatch());
        final Collection<Unit> canMove = new ArrayList<>(getUnitsToUnload(route, unloadAble));
        canMove.addAll(Match.getMatches(selectedUnits, new InverseMatch<>(getUnloadableMatch())));
        if (paratroopsLanding) {
          transports = canMove;
        }
        if (canMove.isEmpty()) {
          cancelMove();
          return;
        } else {
          selectedUnits.clear();
          selectedUnits.addAll(canMove);
        }
      } else {
        // keep a map of the max number of each eligible unitType that can be chosen
        final IntegerMap<UnitType> maxMap = new IntegerMap<>();
        for (final Unit unit : units) {
          maxMap.add(unit.getType(), 1);
        }
        // this match will make sure we can't select more units
        // of a specific type then we had originally selected
        final Match<Collection<Unit>> unitTypeCountMatch = new Match<Collection<Unit>>() {
          @Override
          public boolean match(final Collection<Unit> units) {
            final IntegerMap<UnitType> currentMap = new IntegerMap<>();
            for (final Unit unit : units) {
              currentMap.add(unit.getType(), 1);
            }
            return maxMap.greaterThanOrEqualTo(currentMap);
          }
        };
        allowSpecificUnitSelection(units, route, false, unitTypeCountMatch);
        if (units.isEmpty()) {
          cancelMove();
          return;
        }
      }
      final MoveDescription message = new MoveDescription(units, route, transports, s_dependentUnits);
      setMoveMessage(message);
      setFirstSelectedTerritory(null);
      setSelectedEndpointTerritory(null);
      mouseCurrentTerritory = null;
      forced = null;
      updateRouteAndMouseShadowUnits(null);
      release();
    }
  };

  /**
   * Allow the user to select specific units, if for example some units
   * have different movement
   * Units are sorted in preferred order, so units represents the default selections.
   */
  private boolean allowSpecificUnitSelection(final Collection<Unit> units, final Route route, boolean mustQueryUser,
      final Match<Collection<Unit>> matchCriteria) {
    final List<Unit> candidateUnits = getFirstSelectedTerritory().getUnits().getMatches(getMovableMatch(route, units));
    if (!mustQueryUser) {
      final Set<UnitCategory> categories =
          UnitSeperator.categorize(candidateUnits, mustMoveWithDetails.getMustMoveWith(), true, false);
      for (final UnitCategory category1 : categories) {
        // we cant move these, dont bother to check
        if (category1.getMovement() == 0) {
          continue;
        }
        for (final UnitCategory category2 : categories) {
          // we cant move these, dont bother to check
          if (category2.getMovement() == 0) {
            continue;
          }
          // if we find that two categories are compatable, and some units
          // are selected from one category, but not the other
          // then the user has to refine his selection
          if (category1 != category2 && category1.getType() == category2.getType() && !category1.equals(category2)) {
            // if we are moving all the units from both categories, then nothing to choose
            if (units.containsAll(category1.getUnits()) && units.containsAll(category2.getUnits())) {
              continue;
            }
            // if we are moving some of the units from either category, then we need to stop
            if (!Util.intersection(category1.getUnits(), units).isEmpty()
                || !Util.intersection(category2.getUnits(), units).isEmpty()) {
              mustQueryUser = true;
            }
          }
        }
      }
    }
    if (mustQueryUser) {
      final List<Unit> defaultSelections = new ArrayList<>(units.size());
      if (route.isLoad()) {
        final Collection<Unit> transportsToLoad = new ArrayList<>(getTransportsToLoad(route, units, false));
        defaultSelections.addAll(TransportUtils.mapTransports(route, units, transportsToLoad).keySet());
      } else {
        defaultSelections.addAll(units);
      }
      // sort candidateUnits in preferred order
      sortUnitsToMove(candidateUnits, route);
      final UnitChooser chooser =
          new UnitChooser(candidateUnits, defaultSelections, mustMoveWithDetails.getMustMoveWith(), true, false,
              getGameData(), false, getMap().getUIContext(), matchCriteria);
      final String text = "Select units to move from " + getFirstSelectedTerritory() + ".";
      final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text,
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
      if (option != JOptionPane.OK_OPTION) {
        units.clear();
        return false;
      }
      units.clear();
      units.addAll(chooser.getSelected(false));
    }
    // add the dependent units
    final List<Unit> unitsCopy = new ArrayList<>(units);
    for (final Unit unit : unitsCopy) {
      final Collection<Unit> forced = mustMoveWithDetails.getMustMoveWith().get(unit);
      if (forced != null) {
        // add dependent if necessary
        for (final Unit dependent : forced) {
          if (unitsCopy.indexOf(dependent) == -1) {
            units.add(dependent);
          }
        }
      }
    }
    return true;
  }

  private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener() {
    @Override
    public void mouseEnter(final List<Unit> units, final Territory territory, final MouseDetails me) {
      if (!getListening()) {
        return;
      }
      final PlayerID owner = getUnitOwner(selectedUnits);
      final CompositeMatchAnd<Unit> match =
          new CompositeMatchAnd<>(Matches.unitIsOwnedBy(owner)/* , Matches.UnitIsNotFactory */);
      match.add(Matches.UnitCanMove);
      final boolean someOwned = Match.someMatch(units, match);
      final boolean isCorrectTerritory = firstSelectedTerritory == null || firstSelectedTerritory == territory;
      if (someOwned && isCorrectTerritory) {
        final Map<Territory, List<Unit>> highlight = new HashMap<>();
        highlight.put(territory, units);
        getMap().setUnitHighlight(highlight);
      } else {
        getMap().setUnitHighlight(null);
      }
    }
  };
  private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {
    @Override
    public void territorySelected(final Territory territory, final MouseDetails me) {}

    @Override
    public void mouseMoved(final Territory territory, final MouseDetails me) {
      if (!getListening()) {
        return;
      }
      if (getFirstSelectedTerritory() != null && territory != null) {
        Route route;
        if (mouseCurrentTerritory == null || !mouseCurrentTerritory.equals(territory)
            || mouseCurrentPoint.equals(mouseLastUpdatePoint)) {
          route = getRoute(getFirstSelectedTerritory(), territory, selectedUnits);
          getData().acquireReadLock();
          try {
            updateUnitsThatCanMoveOnRoute(selectedUnits, route);
            // now, check if there is a better route for just the units that can get there (we check only air since that
            // is the only one for
            // which the route may actually change much)
            if (unitsThatCanMoveOnRoute.size() < selectedUnits.size() && (unitsThatCanMoveOnRoute.size() == 0
                || Match.allMatch(unitsThatCanMoveOnRoute, Matches.UnitIsAir))) {
              final Collection<Unit> airUnits = Match.getMatches(selectedUnits, Matches.UnitIsAir);
              if (airUnits.size() > 0) {
                route = getRoute(getFirstSelectedTerritory(), territory, airUnits);
                updateUnitsThatCanMoveOnRoute(airUnits, route);
              }
            }
          } finally {
            getData().releaseReadLock();
          }
        } else {
          route = routeCached;
        }
        mouseCurrentPoint = me.getMapPoint();
        updateRouteAndMouseShadowUnits(route);
      }
      mouseCurrentTerritory = territory;
    }
  };

  @Override
  public final String toString() {
    return "MovePanel";
  }

  final void setFirstSelectedTerritory(final Territory firstSelectedTerritory) {
    if (this.firstSelectedTerritory == firstSelectedTerritory) {
      return;
    }
    this.firstSelectedTerritory = firstSelectedTerritory;
    if (firstSelectedTerritory == null) {
      mustMoveWithDetails = null;
    } else {
      mustMoveWithDetails = MoveValidator.getMustMoveWith(firstSelectedTerritory,
          firstSelectedTerritory.getUnits().getUnits(), s_dependentUnits, getData(), getCurrentPlayer());
    }
  }

  private Territory getFirstSelectedTerritory() {
    return firstSelectedTerritory;
  }

  final void setSelectedEndpointTerritory(final Territory selectedEndpointTerritory) {
    this.selectedEndpointTerritory = selectedEndpointTerritory;
  }

  private Territory getSelectedEndpointTerritory() {
    return selectedEndpointTerritory;
  }

  private static boolean IsParatroopersCanMoveDuringNonCombat(final GameData data) {
    return games.strategy.triplea.Properties.getParatroopersCanMoveDuringNonCombat(data);
  }

  private final List<Unit> UserChooseUnits(final Set<Unit> defaultSelections,
      final Match<Collection<Unit>> unitsToLoadMatch, final List<Unit> unitsToLoad, final String title,
      final String action) {
    // Allow player to select which to load.
    final UnitChooser chooser = new UnitChooser(unitsToLoad, defaultSelections, s_dependentUnits,
        /* categorizeMovement */false, /* categorizeTransportCost */true, getGameData(), /* allowTwoHit */false,
        getMap().getUIContext(), unitsToLoadMatch);
    chooser.setTitle(title);
    final int option =
        JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What units do you want to " + action,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if (option != JOptionPane.OK_OPTION) {
      return Collections.emptyList();
    }
    return chooser.getSelected(true);
  }

  @Override
  protected final void cleanUpSpecific() {
    getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
    getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
    getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
    getMap().setUnitHighlight(null);
    selectedUnits.clear();
    updateRouteAndMouseShadowUnits(null);
    forced = null;
  }

  @Override
  protected final void cancelMoveAction() {
    setFirstSelectedTerritory(null);
    setSelectedEndpointTerritory(null);
    mouseCurrentTerritory = null;
    forced = null;
    selectedUnits.clear();
    currentCursorImage = null;
    updateRouteAndMouseShadowUnits(null);
    getMap().showMouseCursor();
    getMap().setMouseShadowUnits(null);
  }

  @Override
  protected final void undoMoveSpecific() {
    getMap().setRoute(null);
  }

  public final void setNonCombat(final boolean nonCombat) {
    this.nonCombat = nonCombat;
  }

  public final void setDisplayText(final String displayText) {
    this.displayText = displayText;
  }

  @Override
  public final void display(final PlayerID id) {
    super.display(id, displayText);
  }

  @Override
  protected final void setUpSpecific() {
    setFirstSelectedTerritory(null);
    forced = null;
    getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
    getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
    getMap().addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
  }

  public KeyListener getCustomKeyListeners() {
    return new KeyListener() {
      @Override
      public void keyTyped(final KeyEvent e) {}

      @Override
      public void keyPressed(final KeyEvent e) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_N:
            centerOnNextMoveableUnit();
            break;
          case KeyEvent.VK_F:
            highlightMoveableUnits();
            break;
          case KeyEvent.VK_U:
            if (getMap().getHighlightedUnits() != null && !getMap().getHighlightedUnits().isEmpty()) {
              m_undoableMovesPanel.undoMoves(getMap().getHighlightedUnits());
            }
            break;
        }
      }

      @Override
      public void keyReleased(final KeyEvent e) {}
    };

  }


  @Override
  protected boolean doneMoveAction() {
    if (m_undoableMovesPanel.getCountOfMovesMade() == 0) {
      final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(MovePanel.this),
          "Are you sure you dont want to move?", "End Move", JOptionPane.YES_NO_OPTION);
      return rVal == JOptionPane.YES_OPTION;
    }
    return true;
  }

  @Override
  protected boolean setCancelButton() {
    return true;
  }

  private void centerOnNextMoveableUnit() {
    final List<Territory> allTerritories;
    getData().acquireReadLock();
    try {
      allTerritories = new ArrayList<>(getData().getMap().getTerritories());
    } finally {
      getData().releaseReadLock();
    }
    final CompositeMatchAnd<Unit> moveableUnitOwnedByMe =
        new CompositeMatchAnd<>(Matches.unitIsOwnedBy(getCurrentPlayer()), Matches.unitHasMovementLeft);
    if (!nonCombat) {
      // if not non combat, cannot move aa units
      moveableUnitOwnedByMe.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
    }
    final int size = allTerritories.size();
    // new focused index is 1 greater
    int newFocusedIndex = lastFocusedTerritory == null ? 0 : allTerritories.indexOf(lastFocusedTerritory) + 1;
    if (newFocusedIndex >= size) {
      // if we are larger than the number of territories, we must start back at zero
      newFocusedIndex = 0;
    }
    Territory newFocusedTerritory = null;
    // make sure we go through every single territory on the board
    int i = 0;
    while (i < size) {
      final Territory t = allTerritories.get(newFocusedIndex);
      final List<Unit> matchedUnits = t.getUnits().getMatches(moveableUnitOwnedByMe);
      if (matchedUnits.size() > 0) {
        newFocusedTerritory = t;
        final Map<Territory, List<Unit>> highlight = new HashMap<>();
        highlight.put(t, matchedUnits);
        getMap().setUnitHighlight(highlight);
        break;
      }
      // make sure to cycle through the front half of territories
      if ((newFocusedIndex + 1) >= size) {
        newFocusedIndex = 0;
      } else {
        newFocusedIndex++;
      }
      i++;
    }
    if (newFocusedTerritory != null) {
      lastFocusedTerritory = newFocusedTerritory;
      getMap().centerOn(newFocusedTerritory);
    }
  }

  private void highlightMoveableUnits() {
    final List<Territory> allTerritories;
    getData().acquireReadLock();
    try {
      allTerritories = new ArrayList<>(getData().getMap().getTerritories());
    } finally {
      getData().releaseReadLock();
    }
    final CompositeMatchAnd<Unit> moveableUnitOwnedByMe =
        new CompositeMatchAnd<>(Matches.unitIsOwnedBy(getCurrentPlayer()), Matches.unitHasMovementLeft);
    if (!nonCombat) {
      // if not non combat, cannot move aa units
      moveableUnitOwnedByMe.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
    }
    final Map<Territory, List<Unit>> highlight = new HashMap<>();
    for (final Territory t : allTerritories) {
      final List<Unit> moveableUnits = t.getUnits().getMatches(moveableUnitOwnedByMe);
      if (!moveableUnits.isEmpty()) {
        highlight.put(t, moveableUnits);
      }
    }
    if (!highlight.isEmpty()) {
      getMap().setUnitHighlight(highlight);
    }
  }
}
