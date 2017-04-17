package games.strategy.engine.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameSequence extends GameDataComponent implements Iterable<GameStep> {
  private static final long serialVersionUID = 6354618406598578287L;

  private final List<GameStep> m_steps = new ArrayList<>();
  private int m_currentIndex;
  private int m_round = 1;
  private int m_roundOffset = 0;
  private transient Object m_currentStepMutex = new Object();

  public GameSequence(final GameData data) {
    super(data);
  }

  /**
   * Only used when we are trying to export the data to a savegame,
   * and we need to change the round and step to something other than the current round and step
   * (because we are creating a savegame at a certain point in history, for example).
   *
   * @param currentRound
   * @param stepDisplayName
   * @param player
   */
  public synchronized void setRoundAndStep(final int currentRound, final String stepDisplayName,
      final PlayerID player) {
    m_round = currentRound;
    boolean found = false;
    for (int i = 0; i < m_steps.size(); i++) {
      final GameStep step = m_steps.get(i);
      if (step != null && step.getDisplayName().equalsIgnoreCase(stepDisplayName)) {
        if ((player == null && step.getPlayerID() == null) || (player != null && player.equals(step.getPlayerID()))) {
          m_currentIndex = i;
          found = true;
          break;
        }
      }
    }
    if (!found) {
      m_currentIndex = 0;
      System.err.println("Step Not Found (" + stepDisplayName + ":" + player.getName() + "), will instead use: "
          + m_steps.get(m_currentIndex));
    }
  }

  public void addStep(final GameStep step) {
    m_steps.add(step);
  }

  /**
   * Removes the first instance of step.
   */
  protected void remove(final GameStep step) {
    if (!m_steps.contains(step)) {
      throw new IllegalArgumentException("Step does not exist");
    }
    m_steps.remove(step);
  }

  protected void removeStep(final int index) {
    m_steps.remove(index);
  }

  public void removeAllSteps() {
    m_steps.clear();
    m_round = 1;
  }

  public int getRound() {
    return m_round + m_roundOffset;
  }

  public int getRoundOffset() {
    return m_roundOffset;
  }

  public void setRoundOffset(final int roundOffset) {
    m_roundOffset = roundOffset;
  }

  public int getStepIndex() {
    return m_currentIndex;
  }

  void setStepIndex(final int newIndex) {
    if ((newIndex < 0) || (newIndex >= m_steps.size())) {
      throw new IllegalArgumentException("New index out of range: " + newIndex);
    }
    m_currentIndex = newIndex;
  }

  /**
   * @return boolean whether the round has changed.
   */
  public boolean next() {
    synchronized (m_currentStepMutex) {
      m_currentIndex++;
      if (m_currentIndex >= m_steps.size()) {
        m_currentIndex = 0;
        m_round++;
        return true;
      }
      return false;
    }
  }

  /**
   * Only tests to see if we are on the last step.
   * Used for finding if we need to make a new round or not.
   * Does not change any data or fields.
   */
  public boolean testWeAreOnLastStep() {
    synchronized (m_currentStepMutex) {
      return m_currentIndex + 1 >= m_steps.size();
    }
  }

  public GameStep getStep() {
    synchronized (m_currentStepMutex) {
      // since we can now delete game steps mid game, it is a good idea to test if our index is out of range
      if (m_currentIndex < 0) {
        m_currentIndex = 0;
      }
      if (m_currentIndex >= m_steps.size()) {
        next();
      }
      return getStep(m_currentIndex);
    }
  }

  public GameStep getStep(final int index) {
    if ((index < 0) || (index >= m_steps.size())) {
      throw new IllegalArgumentException("Attempt to access invalid state: " + index);
    }
    return m_steps.get(index);
  }

  @Override
  public Iterator<GameStep> iterator() {
    return m_steps.iterator();
  }

  public int size() {
    return m_steps.size();
  }

  /** make sure transient lock object is initialized on deserialization. */
  private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (m_currentStepMutex == null) {
      m_currentStepMutex = new Object();
    }
  }
}
