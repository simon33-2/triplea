package games.strategy.engine.chat;

import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import games.strategy.net.INode;

public interface IPlayerActionFactory {
  IPlayerActionFactory NULL_FACTORY = clickedOn -> Collections.emptyList();

  /**
   * The mouse has been clicked on a player, create a list of actions to be displayed.
   */
  List<Action> mouseOnPlayer(INode clickedOn);
}
