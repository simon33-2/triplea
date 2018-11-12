package games.strategy.engine.data;

import java.util.Objects;
import java.util.Properties;

import games.strategy.engine.delegate.IDelegate;

/**
 * A single step in a game.
 *
 * <p>
 * Typically turn based strategy games are composed of a set of distinct phases (in chess this would be two, white move,
 * black move).
 * </p>
 */
public class GameStep extends GameDataComponent {
  private static final long serialVersionUID = -7944468945162840931L;

  private final String name;
  private final String displayName;
  private final PlayerId player;
  private final String delegateName;
  private int runCount = 0;
  private int maxRunCount = -1;
  private final Properties properties;

  /**
   * The keys for all supported game step properties.
   *
   * @see GameStep#getProperties()
   */
  public interface PropertyKeys {
    String SKIP_POSTING = "skipPosting";
    String TURN_SUMMARY_PLAYERS = "turnSummaryPlayers";
    String AIRBORNE_MOVE = "airborneMove";
    String COMBAT_MOVE = "combatMove";
    String NON_COMBAT_MOVE = "nonCombatMove";
    String FIRE_ROCKETS = "fireRockets";
    String REPAIR_UNITS = "repairUnits";
    String GIVE_BONUS_MOVEMENT = "giveBonusMovement";
    String REMOVE_AIR_THAT_CAN_NOT_LAND = "removeAirThatCanNotLand";
    String RESET_UNIT_STATE_AT_START = "resetUnitStateAtStart";
    String RESET_UNIT_STATE_AT_END = "resetUnitStateAtEnd";
    String BID = "bid";
    String COMBINED_TURNS = "combinedTurns";
    String REPAIR_PLAYERS = "repairPlayers";
  }

  /**
   * Creates new GameStep.
   *
   * @param name name of the game step
   * @param displayName name that gets displayed
   * @param player player who executes the game step
   * @param delegate delegate for the game step
   * @param data game data
   * @param stepProperties properties of the game step
   */
  public GameStep(final String name, final String displayName, final PlayerId player, final IDelegate delegate,
      final GameData data, final Properties stepProperties) {
    super(data);
    this.name = name;
    this.displayName = displayName;
    this.player = player;
    delegateName = delegate.getName();
    properties = stepProperties;
  }

  public String getName() {
    return name;
  }

  public PlayerId getPlayerId() {
    return player;
  }

  public IDelegate getDelegate() {
    return getData().getDelegateList().getDelegate(delegateName);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof GameStep)) {
      return false;
    }
    final GameStep other = (GameStep) o;
    return other.name.equals(this.name) && other.delegateName.equals(this.delegateName)
        && other.player.equals(this.player);
  }

  public boolean hasReachedMaxRunCount() {
    return maxRunCount != -1 && maxRunCount <= runCount;
  }

  public void incrementRunCount() {
    runCount++;
  }

  public void setMaxRunCount(final int count) {
    maxRunCount = count;
  }

  public int getMaxRunCount() {
    return maxRunCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, delegateName, player);
  }

  public String getDisplayName() {
    if (displayName == null) {
      return getDelegate().getDisplayName();
    }
    return displayName;
  }

  /**
   * Returns the properties of the game step.
   * Allowed Properties so far:<br>
   * EndTurn delegates -> skipPosting = true/false<br>
   * EndTurn delegates -> turnSummaryPlayers = colon separated list of players for this turn summary<br>
   * Move delegates -> airborneMove = true/false<br>
   * Move delegates -> combatMove = true/false<br>
   * Move delegates -> nonCombatMove = true/false<br>
   * Move delegates -> fireRocketsAfter = true/false<br>
   * Move & EndTurn delegates -> repairUnits = true/false<br>
   * Move delegates -> giveBonusMovement = true/false<br>
   * Move & Place delegates -> removeAirThatCanNotLand = true/false<br>
   * Move delegates -> resetUnitStateAtStart = true/false<br>
   * Move delegates -> resetUnitStateAtEnd = true/false<br>
   * Purchase & Place delegates -> bid = true/false<br>
   * Purchase delegates -> repairPlayers = colon separated list of players which you can repair for<br>
   * Move delegates -> combinedTurns = colon separated list of players which have intermeshed phases<br>
   */
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return "GameStep:" + name + " delegate:" + delegateName + " player:" + player + " displayName:" + displayName;
  }
}
