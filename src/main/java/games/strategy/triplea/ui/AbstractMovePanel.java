package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.google.common.collect.Sets;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.remote.IAbstractMoveDelegate;

public abstract class AbstractMovePanel extends ActionPanel {
  private static final long serialVersionUID = -4153574987414031433L;
  private static final String s_MOVE_PANEL_CANCEL = "movePanel.cancel";
  private static final Logger s_logger = Logger.getLogger(MovePanel.class.getName());
  private static final int s_entryPadding = 15;
  private final TripleAFrame m_frame;
  private boolean m_listening = false;
  private final JLabel m_actionLabel = new JLabel();
  protected MoveDescription m_moveMessage;
  protected List<UndoableMove> m_undoableMoves;
  protected AbstractAction m_doneMove = new AbstractAction("Done") {
    private static final long serialVersionUID = -6497408896615920650L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (doneMoveAction()) {
        m_moveMessage = null;
        release();
      }
    }
  };
  private final Action m_DONE_MOVE_ACTION = new WeakAction("Done", m_doneMove);
  private final Action m_cancelMove = new AbstractAction("Cancel") {
    private static final long serialVersionUID = -257745862234175428L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      cancelMoveAction();
      if (m_frame != null) {
        m_frame.clearStatusMessage();
      }
      this.setEnabled(false);
      m_CANCEL_MOVE_ACTION.setEnabled(false);
    }
  };

  public AbstractMovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    m_frame = frame;
    m_CANCEL_MOVE_ACTION.setEnabled(false);
    m_undoableMoves = Collections.emptyList();
  }

  /*
   * sub-classes method for done handling
   */
  protected abstract boolean doneMoveAction();

  /*
   * sub-classes method for cancel handling
   */
  protected abstract void cancelMoveAction();

  private final AbstractAction m_CANCEL_MOVE_ACTION = new WeakAction("Cancel", m_cancelMove);
  protected AbstractUndoableMovesPanel m_undoableMovesPanel;
  private IPlayerBridge m_bridge;

  protected IPlayerBridge getPlayerBridge() {
    return m_bridge;
  }

  // m_frame methods
  protected final void clearStatusMessage() {
    m_frame.clearStatusMessage();
  }

  protected final void setStatusErrorMessage(final String message) {
    m_frame.setStatusErrorMessage(message);
  }

  protected final void setStatusWarningMessage(final String message) {
    m_frame.setStatusWarningMessage(message);
  }

  protected final boolean getListening() {
    return m_listening;
  }

  protected final void setMoveMessage(final MoveDescription message) {
    m_moveMessage = message;
  }

  protected final List<UndoableMove> getUndoableMoves() {
    return m_undoableMoves;
  }

  protected final void enableCancelButton() {
    m_CANCEL_MOVE_ACTION.setEnabled(true);
  }

  /**
   * @return m_bridge.getGameData()
   */
  protected final GameData getGameData() {
    return m_bridge.getGameData();
  }

  private IAbstractMoveDelegate getMoveDelegate() {
    return (IAbstractMoveDelegate) m_bridge.getRemoteDelegate();
  }

  @SuppressWarnings("unchecked")
  protected final void updateMoves() {
    m_undoableMoves = (List<UndoableMove>) getMoveDelegate().getMovesMade();
    m_undoableMovesPanel.setMoves(new ArrayList<>(m_undoableMoves));
  }

  public final void cancelMove() {
    m_CANCEL_MOVE_ACTION.actionPerformed(null);
  }

  public final String undoMove(final int moveIndex) {
    return undoMove(moveIndex, false);
  }

  /**
   * Executes an undo move for any of the units passed in as a parameter.
   *
   * <p>
   * "Cannot undo" Error messages are suppressed if any moves cannot be undone
   * (at least until we come up with a way to deal with "n" reasons for an undo
   * failure rather than just one)
   * </p>
   */
  public void undoMoves(final Set<Unit> units) {
    @SuppressWarnings("unchecked")
    final Set<UndoableMove> movesToUndo = getMovesToUndo(units, (List<Object>) getMoveDelegate().getMovesMade());

    if (movesToUndo.size() == 0) {
      final String error =
          "Could not undo any moves, check that the unit has moved and that you can undo the move normally";
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
      return;
    }

    undoMovesInReverseOrder(movesToUndo);
  }

  private static Set<UndoableMove> getMovesToUndo(final Set<Unit> units, final List<Object> movesMade) {
    final Set<UndoableMove> movesToUndo = Sets.newHashSet();

    if (movesMade != null) {
      for (final Object undoableMoveObject : movesMade) {
        if (undoableMoveObject != null) {
          final UndoableMove move = (UndoableMove) undoableMoveObject;
          if (move.containsAnyOf(units) && move.getcanUndo()) {
            movesToUndo.add(move);
          }
        }
      }
    }
    return movesToUndo;
  }

  /*
   * Undo moves in reverse order, from largest index to smallest. Undo will reorder move index numbers, so going top
   * down avoids this renumbering.
   */
  private void undoMovesInReverseOrder(final Set<UndoableMove> movesToUndo) {
    final List<Integer> moveIndexes = getSortedMoveIndexes(movesToUndo);
    for (int i = moveIndexes.size() - 1; i >= 0; i--) {
      undoMove(moveIndexes.get(i));
    }
  }

  private static List<Integer> getSortedMoveIndexes(final Set<UndoableMove> moves) {
    final List<Integer> moveIndexes = new ArrayList<>();
    for (final UndoableMove move : moves) {
      moveIndexes.add(move.getIndex());
    }
    Collections.sort(moveIndexes);
    return moveIndexes;
  }


  protected final String undoMove(final int moveIndex, final boolean suppressError) {
    // clean up any state we may have
    m_CANCEL_MOVE_ACTION.actionPerformed(null);
    // undo the move
    final String error = getMoveDelegate().undoMove(moveIndex);
    if (error != null && !suppressError) {
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
    } else {
      updateMoves();
    }
    undoMoveSpecific();
    return error;
  }

  /**
   * sub-classes method for undo handling.
   */
  protected abstract void undoMoveSpecific();

  protected final void cleanUp() {
    SwingUtilities.invokeLater(() -> {
      s_logger.fine("cleanup");
      if (!m_listening) {
        throw new IllegalStateException("Not listening");
      }
      m_listening = false;
      cleanUpSpecific();
      m_bridge = null;
      m_CANCEL_MOVE_ACTION.setEnabled(false);
      final JComponent rootPane = getRootPane();
      if (rootPane != null) {
        rootPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null);
      }
      removeAll();
      REFRESH.run();
    });
  }

  /*
   * sub-classes method for clean-up
   */
  protected abstract void cleanUpSpecific();

  @Override
  public final void setActive(final boolean active) {
    super.setActive(active);
    SwingUtilities.invokeLater(() -> m_CANCEL_MOVE_ACTION.actionPerformed(null));
  }

  protected final void display(final PlayerID id, final String actionLabel) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      removeAll();
      m_actionLabel.setText(id.getName() + actionLabel);
      add(leftBox(m_actionLabel));
      if (setCancelButton()) {
        add(leftBox(new JButton(m_CANCEL_MOVE_ACTION)));
      }
      add(leftBox(new JButton(m_DONE_MOVE_ACTION)));
      addAdditionalButtons();
      add(Box.createVerticalStrut(s_entryPadding));
      add(m_undoableMovesPanel);
      add(Box.createGlue());
      SwingUtilities.invokeLater(REFRESH);
    });
  }

  protected void addAdditionalButtons() {}

  protected abstract boolean setCancelButton();

  protected static JComponent leftBox(final JComponent c) {
    final Box b = new Box(BoxLayout.X_AXIS);
    b.add(c);
    b.add(Box.createHorizontalGlue());
    return b;
  }

  protected final void setUp(final IPlayerBridge bridge) {
    SwingUtilities.invokeLater(() -> {
      s_logger.fine("setup");
      setUpSpecific();
      m_bridge = bridge;
      updateMoves();
      if (m_listening) {
        throw new IllegalStateException("Not listening");
      }
      m_listening = true;
      if (getRootPane() != null) {
        final String key = s_MOVE_PANEL_CANCEL;
        getRootPane().getActionMap().put(key, m_CANCEL_MOVE_ACTION);
        getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);
      }
    });
  }

  /*
   * sub-classes method for set-up
   */
  protected abstract void setUpSpecific();

  protected void clearDependencies() {
    // used by some subclasses
  }

  public final MoveDescription waitForMove(final IPlayerBridge bridge) {
    setUp(bridge);
    waitForRelease();
    cleanUp();
    final MoveDescription rVal = m_moveMessage;
    m_moveMessage = null;
    clearDependencies();
    return rVal;
  }
}
