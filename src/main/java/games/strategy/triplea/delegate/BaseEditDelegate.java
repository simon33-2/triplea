package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.triplea.Constants;

public abstract class BaseEditDelegate extends BasePersistentDelegate {
  public static String EDITMODE_ON = "Turning on Edit Mode";
  public static String EDITMODE_OFF = "Turning off Edit Mode";

  /**
   * Called before the delegate will run, AND before "start" is called.
   */
  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(iDelegateBridge));
  }

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
  }

  @Override
  public void end() {}

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }

  public static boolean getEditMode(final GameData data) {
    final Object editMode = data.getProperties().get(Constants.EDIT_MODE);
    if (editMode == null) {
      return false;
    }
    if (!(editMode instanceof Boolean)) {
      return false;
    }
    return (Boolean) editMode;
  }

  protected String checkPlayerID() {
    final IRemotePlayer remotePlayer = getRemotePlayer();
    if (!bridge.getPlayerID().equals(remotePlayer.getPlayerID())) {
      return "Edit actions can only be performed during players turn";
    }
    return null;
  }

  protected String checkEditMode() {
    final String result = checkPlayerID();
    if (null != result) {
      return result;
    }
    if (!getEditMode(getData())) {
      return "Edit mode is not enabled";
    }
    return null;
  }

  public String setEditMode(final boolean editMode) {
    final IRemotePlayer remotePlayer = getRemotePlayer();
    if (!bridge.getPlayerID().equals(remotePlayer.getPlayerID())) {
      return "Edit Mode can only be toggled during players turn";
    }
    logEvent((editMode ? EDITMODE_ON : EDITMODE_OFF), null);
    bridge.addChange(ChangeFactory.setProperty(Constants.EDIT_MODE, editMode, getData()));
    return null;
  }

  public boolean getEditMode() {
    return getEditMode(getData());
  }

  public String addComment(final String message) {
    String result = null;
    if (null != (result = checkPlayerID())) {
      return result;
    }
    logEvent("COMMENT: " + message, null);
    return null;
  }

  // We don't know the current context, so we need to figure
  // out whether it makes more sense to log a new event or a child.
  // If any child events came before us, then we'll log a child event.
  // Otherwise, we'll log a new event.
  protected void logEvent(final String message, final Object renderingObject) {
    // find last event node
    boolean foundChild = false;
    final GameData game_data = getData();
    game_data.acquireReadLock();
    try {
      HistoryNode curNode = game_data.getHistory().getLastNode();
      while (!(curNode instanceof Step) && !(curNode instanceof Event)) {
        if (curNode instanceof EventChild) {
          foundChild = true;
          break;
        }
        curNode = (HistoryNode) curNode.getPreviousNode();
      }
    } finally {
      game_data.releaseReadLock();
    }
    if (foundChild) {
      bridge.getHistoryWriter().addChildToEvent(message, renderingObject);
    } else {
      bridge.getHistoryWriter().startEvent(message, renderingObject);
    }
  }
}
