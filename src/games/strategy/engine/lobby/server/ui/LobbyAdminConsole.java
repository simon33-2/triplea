package games.strategy.engine.lobby.server.ui;

import java.awt.BorderLayout;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import games.strategy.debug.HeartBeat;
import games.strategy.debug.IHeartBeat;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.lobby.client.ui.LobbyGamePanel;
import games.strategy.engine.lobby.server.IRemoteHostUtils;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.RemoteHostUtils;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.util.MD5Crypt;

/**
 * GUI for the lobby.
 */
public class LobbyAdminConsole extends JFrame {
  private static final long serialVersionUID = -3982159130973521505L;
  private static final Logger s_logger = Logger.getLogger(LobbyAdminConsole.class.getName());
  private final LobbyServer m_server;
  private JButton m_backupNow;
  private JButton m_exit;
  private JButton m_bootPlayer;
  private JButton m_debugPlayer;
  private JButton m_remoteHostActions;
  private DBExplorerPanel m_executor;
  private AllUsersPanel m_allUsers;
  private LobbyGamePanel m_lobbyGamePanel;
  private ChatMessagePanel m_chatPanel;

  public LobbyAdminConsole(final LobbyServer server) {
    super("Lobby Admin Console");
    m_server = server;
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    m_backupNow = new JButton("Backup Now");
    m_bootPlayer = new JButton("Boot Player");
    m_debugPlayer = new JButton("Debug Player");
    m_remoteHostActions = new JButton("Remote Host Actions");
    m_exit = new JButton("Exit");
    m_executor = new DBExplorerPanel();
    m_allUsers = new AllUsersPanel(m_server.getMessenger());
    m_lobbyGamePanel = new LobbyGamePanel(m_server.getMessengers());
    final Chat chat =
        new Chat(LobbyServer.LOBBY_CHAT, m_server.getMessengers(), Chat.CHAT_SOUND_PROFILE.LOBBY_CHATROOM);
    m_chatPanel = new ChatMessagePanel(chat);
  }

  private void layoutComponents() {
    final JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    toolBar.add(m_exit);
    toolBar.add(m_bootPlayer);
    toolBar.add(m_backupNow);
    toolBar.add(m_debugPlayer);
    toolBar.add(m_remoteHostActions);
    add(toolBar, BorderLayout.NORTH);
    final JSplitPane leftTopSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    leftTopSplit.setTopComponent(m_executor);
    leftTopSplit.setBottomComponent(m_lobbyGamePanel);
    final JSplitPane letSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    letSplit.setTopComponent(leftTopSplit);
    letSplit.setBottomComponent(m_chatPanel);
    final JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(letSplit);
    mainSplit.setRightComponent(m_allUsers);
    add(mainSplit, BorderLayout.CENTER);
  }

  private void setupListeners() {
    m_bootPlayer.addActionListener(new BootPlayerAction(this, m_server.getMessenger()));
    m_debugPlayer.addActionListener(e -> debugPlayer());
    m_remoteHostActions.addActionListener(e -> remoteHostActions());
    m_exit.addActionListener(e -> {
      final int option = JOptionPane.showConfirmDialog(LobbyAdminConsole.this, "Are you Sure?", "Are you Sure",
          JOptionPane.YES_NO_OPTION);
      if (option != JOptionPane.YES_OPTION) {
        return;
      }
      System.exit(0);
    });
    m_backupNow.addActionListener(e -> Database.backup());
  }

  private void setWidgetActivation() {}

  private void debugPlayer() {
    final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    final JComboBox<String> combo = new JComboBox<>(model);
    model.addElement("");
    for (final INode node : new TreeSet<>(m_server.getMessenger().getNodes())) {
      if (!node.equals(m_server.getMessenger().getLocalNode())) {
        model.addElement(node.getName());
      }
    }
    if (model.getSize() == 1) {
      JOptionPane.showMessageDialog(this, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final int rVal = JOptionPane.showConfirmDialog(LobbyAdminConsole.this, combo, "Select player to debug",
        JOptionPane.OK_CANCEL_OPTION);
    if (rVal != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    for (final INode node : m_server.getMessenger().getNodes()) {
      if (node.getName().equals(name)) {
        // run in a seperate thread
        // if it doesnt return because the
        // remote computer is blocked, we don't want to
        // kill the swing thread
        final Runnable r = () -> {
          s_logger.info("Getting debug info for:" + node);
          final RemoteName remoteName = HeartBeat.getHeartBeatName(node);
          final IHeartBeat heartBeat =
              (IHeartBeat) m_server.getMessengers().getRemoteMessenger().getRemote(remoteName);
          s_logger.info("Debug info for:" + node);
          s_logger.info(heartBeat.getDebugInfo());
          s_logger.info("Debug info finished");
        };
        final Thread t = new Thread(r, "Debug player called at " + new Date());
        t.setDaemon(true);
        t.start();
        return;
      }
    }
    s_logger.info("No node found named:" + name);
  }

  private void remoteHostActions() {
    final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    final JComboBox<String> combo = new JComboBox<>(model);
    model.addElement("");
    for (final INode node : new TreeSet<>(m_server.getMessenger().getNodes())) {
      if (!node.equals(m_server.getMessenger().getLocalNode())) {
        model.addElement(node.getName());
      }
    }
    if (model.getSize() == 1) {
      JOptionPane.showMessageDialog(this, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final int rVal = JOptionPane.showConfirmDialog(LobbyAdminConsole.this, combo, "Select player to debug",
        JOptionPane.OK_CANCEL_OPTION);
    if (rVal != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    final String password = JOptionPane.showInputDialog(null, "Host Remote Access Password?",
        "Host Remote Access Password?", JOptionPane.QUESTION_MESSAGE);
    for (final INode node : m_server.getMessenger().getNodes()) {
      if (node.getName().equals(name)) {
        // run in a seperate thread
        // if it doesnt return because the
        // remote computer is blocked, we don't want to
        // kill the swing thread
        final Runnable r = () -> {
          s_logger.info("Starting Remote Host Action for: " + node);
          final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
          final IRemoteHostUtils hostUtils =
              (IRemoteHostUtils) m_server.getMessengers().getRemoteMessenger().getRemote(remoteName);
          s_logger.info("Remote Host Action for:" + node);
          final String salt = hostUtils.getSalt();
          final String hashedPassword = MD5Crypt.crypt(password, salt);
          final String response = hostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
          s_logger.info(response == null ? "Successfull Remote Action" : "Failed: " + response);
          s_logger.info("Remote Host Action finished");
        };
        final Thread t = new Thread(r, "Debug player called at " + new Date());
        t.setDaemon(true);
        t.start();
        return;
      }
    }
    s_logger.info("No node found named:" + name);
  }
}
