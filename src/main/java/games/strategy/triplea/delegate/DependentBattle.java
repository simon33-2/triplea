package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

/**
 * Battle with possible dependencies
 * Includes MustFightBattle and NonFightingBattle.
 */
public abstract class DependentBattle extends AbstractBattle {
  private static final long serialVersionUID = 9119442509652443015L;
  protected Map<Territory, Collection<Unit>> m_attackingFromMap = new HashMap<>();
  protected Set<Territory> m_attackingFrom = new HashSet<>();
  protected final Collection<Territory> m_amphibiousAttackFrom = new ArrayList<>();

  DependentBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final boolean isBombingRun, final BattleType battleType, final GameData data) {
    super(battleSite, attacker, battleTracker, isBombingRun, battleType, data);
  }

  abstract public Collection<Territory> getAttackingFrom();

  abstract public Map<Territory, Collection<Unit>> getAttackingFromMap();

  abstract public Collection<Territory> getAmphibiousAttackTerritories();
}
