package games.strategy.engine.framework.headlessGameServer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.Set;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.engine.ClientContext;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

public class HeadlessConsoleController {

  private final HeadlessGameServer server;
  private final PrintStream out;
  private final BufferedReader in;
  private boolean m_chatMode = false;

  public HeadlessConsoleController(final HeadlessGameServer server, final InputStream in, final PrintStream out) {
    this.out = checkNotNull(out);
    this.in = new BufferedReader(new InputStreamReader(checkNotNull(in)));
    this.server = checkNotNull(server);
  }

  protected void process(final String command) {
    if (command.equals("")) {
      return;
    }
    final String noun = command.split("\\s")[0];
    if (noun.equalsIgnoreCase("help")) {
      showHelp();
    } else if (noun.equalsIgnoreCase("status")) {
      showStatus();
    } else if (noun.equalsIgnoreCase("save")) {
      save(command);
    } else if (noun.equalsIgnoreCase("stop")) {
      stop();
    } else if (noun.equalsIgnoreCase("quit")) {
      quit();
    } else if (noun.equalsIgnoreCase("connections")) {
      showConnections();
    } else if (noun.equalsIgnoreCase("send")) {
      send(command);
    } else if (noun.equalsIgnoreCase("chatlog")) {
      chatlog();
    } else if (noun.equalsIgnoreCase("chatmode")) {
      chatmode();
    } else if (noun.equalsIgnoreCase("mute")) {
      mute(command);
    } else if (noun.equalsIgnoreCase("boot")) {
      boot(command);
    } else if (noun.equalsIgnoreCase("ban")) {
      ban(command);
    } else if (noun.equalsIgnoreCase("memory")) {
      memory();
    } else if (noun.equalsIgnoreCase("threads")) {
      threads();
    } else if (noun.equalsIgnoreCase("dump")) {
      printThreadDumpsAndStatus();
    } else {
      out.println("Unrecognized command:" + command);
      showHelp();
    }
  }



  private void send(final String command) {
    if (command == null) {
      return;
    }
    final Chat chat = server.getChat();
    if (chat == null) {
      return;
    }
    try {
      final String message;
      if (command.length() > 5) {
        message = command.substring(5, command.length());
      } else {
        out.println("Input chat message: ");
        message = in.readLine();
      }
      chat.sendMessage(message, false);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void chatlog() {
    if (server == null) {
      return;
    }
    final IChatPanel chat = server.getServerModel().getChatPanel();
    if (chat == null) {
      return;
    }
    out.println();
    out.println(chat.getAllText());
    out.println();
  }

  private void chatmode() {
    if (server == null) {
      return;
    }
    final IChatPanel chat = server.getServerModel().getChatPanel();
    if (chat == null || !(chat instanceof HeadlessChat)) {
      return;
    }
    m_chatMode = !m_chatMode;
    out.println("chatmode is now " + (m_chatMode ? "on" : "off"));
    final HeadlessChat headlessChat = (HeadlessChat) chat;
    headlessChat.setPrintStream(m_chatMode ? out : null);
  }

  private void printThreadDumpsAndStatus() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Dump to Log:");
    sb.append("\n\nStatus:\n");
    sb.append(getStatus());
    sb.append("\n\nServer:\n");
    sb.append(server == null ? "null" : server.getServerModel());
    sb.append("\n\n");
    sb.append(DebugUtils.getThreadDumps());
    sb.append("\n\n");
    sb.append(DebugUtils.getMemory());
    sb.append("\n\nDump finished.\n");
    HeadlessGameServer.log(sb.toString());
  }

  private void threads() {
    out.println(DebugUtils.getThreadDumps());
  }

  private void memory() {
    out.println(DebugUtils.getMemory());
  }


  private void mute(final String command) {
    if (server.getServerModel() == null) {
      return;
    }
    final IServerMessenger messenger = server.getServerModel().getMessenger();
    if (messenger == null) {
      return;
    }
    final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
    if (nodes == null) {
      return;
    }
    try {
      final String name;
      if (command.length() > 4 && command.split(" ").length > 1) {
        name = command.split(" ")[1];
      } else {
        out.println("Input player name to mute: ");
        name = in.readLine();
      }
      if (name == null || name.length() < 1) {
        out.println("Invalid name");
        return;
      }
      final String minutes;
      if (command.length() > 4 && command.split(" ").length > 2) {
        minutes = command.split(" ")[2];
      } else {
        out.println("Input minutes to mute: ");
        minutes = in.readLine();
      }
      final long min;
      try {
        min = Math.max(0, Math.min(60 * 24 * 2, Long.parseLong(minutes))); // max out at 48 hours
      } catch (final NumberFormatException nfe) {
        out.println("Invalid minutes");
        return;
      }
      final long expire = System.currentTimeMillis() + (min * 1000 * 60); // milliseconds
      for (final INode node : nodes) {
        final String realName = node.getName().split(" ")[0];
        final String ip = node.getAddress().getHostAddress();
        final String mac = messenger.getPlayerMac(node.getName());
        if (realName.equals(name)) {
          messenger.NotifyUsernameMutingOfPlayer(realName, new Date(expire));
          messenger.NotifyIPMutingOfPlayer(ip, new Date(expire));
          messenger.NotifyMacMutingOfPlayer(mac, new Date(expire));
          return;
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void boot(final String command) {
    if (server.getServerModel() == null) {
      return;
    }
    final IServerMessenger messenger = server.getServerModel().getMessenger();
    if (messenger == null) {
      return;
    }
    final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
    if (nodes == null) {
      return;
    }
    try {
      final String name;
      if (command.length() > 4 && command.split(" ").length > 1) {
        name = command.split(" ")[1];
      } else {
        out.println("Input player name to boot: ");
        name = in.readLine();
      }
      if (name == null || name.length() < 1) {
        out.println("Invalid name");
        return;
      }
      for (final INode node : nodes) {
        final String realName = node.getName().split(" ")[0];
        if (realName.equals(name)) {
          messenger.removeConnection(node);
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void ban(final String command) {
    if (server.getServerModel() == null) {
      return;
    }
    final IServerMessenger messenger = server.getServerModel().getMessenger();
    if (messenger == null) {
      return;
    }
    final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
    if (nodes == null) {
      return;
    }
    try {
      final String name;
      if (command.length() > 4 && command.split(" ").length > 1) {
        name = command.split(" ")[1];
      } else {
        out.println("Input player name to ban: ");
        name = in.readLine();
      }
      if (name == null || name.length() < 1) {
        out.println("Invalid name");
        return;
      }
      final String hours;
      if (command.length() > 4 && command.split(" ").length > 2) {
        hours = command.split(" ")[2];
      } else {
        out.println("Input hours to ban: ");
        hours = in.readLine();
      }
      final long hrs;
      try {
        hrs = Math.max(0, Math.min(24 * 30, Long.parseLong(hours))); // max out at 30 days
      } catch (final NumberFormatException nfe) {
        out.println("Invalid minutes");
        return;
      }
      final long expire = System.currentTimeMillis() + (hrs * 1000 * 60 * 60); // milliseconds
      for (final INode node : nodes) {
        final String realName = node.getName().split(" ")[0];
        final String ip = node.getAddress().getHostAddress();
        final String mac = messenger.getPlayerMac(node.getName());
        if (realName.equals(name)) {
          try {
            messenger.NotifyUsernameMiniBanningOfPlayer(realName, new Date(expire));
          } catch (final Exception e) {
            ClientLogger.logQuietly(e);
          }
          try {
            messenger.NotifyIPMiniBanningOfPlayer(ip, new Date(expire));
          } catch (final Exception e) {
            ClientLogger.logQuietly(e);
          }
          try {
            messenger.NotifyMacMiniBanningOfPlayer(mac, new Date(expire));
          } catch (final Exception e) {
            ClientLogger.logQuietly(e);
          }
          messenger.removeConnection(node);
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void save(final String command) {
    final ServerGame game = server.getIGame();
    if (game == null) {
      out.println("No Game Currently Running");
    } else {
      try {
        String saveName;
        if (command.length() > 5) {
          saveName = command.substring(5, command.length());
        } else {
          out.println("Input savegame filename: ");
          saveName = in.readLine();
        }
        if (saveName == null || saveName.length() < 2) {
          out.println("Invalid save name");
          return;
        }
        if (!saveName.endsWith(".tsvg")) {
          saveName += ".tsvg";
        }
        SaveGameFileChooser.ensureMapsFolderExists();
        final File f = new File(ClientContext.folderSettings().getSaveGamePath(), saveName);
        try {
          game.saveGame(f);
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
      }
    }
  }

  private void stop() {
    final ServerGame game = server.getIGame();
    if (game == null) {
      out.println("No Game Currently Running");
      return;
    }
    out.println("Are you sure? (y/n)");
    try {
      final String readin = in.readLine();
      if (readin == null) {
        return;
      }
      final boolean stop = readin.toLowerCase().startsWith("y");
      if (stop) {
        SaveGameFileChooser.ensureMapsFolderExists();
        final File f1 =
            new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSaveFileName());
        final File f2 =
            new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSave2FileName());
        final File f;
        if (f1.lastModified() > f2.lastModified()) {
          f = f2;
        } else {
          f = f1;
        }
        try {
          game.saveGame(f);
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
        game.stopGame();
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void quit() {
    out.println("Are you sure? (y/n)");
    try {
      final String readin = in.readLine();
      if (readin != null && readin.toLowerCase().startsWith("y")) {
        server.shutdown();
        if (server.getSetupPanelModel() != null) {
          final ISetupPanel setup = server.getSetupPanelModel().getPanel();
          if (setup != null && setup instanceof ServerSetupPanel) {
            // this is causing a deadlock when in a shutdown hook, due to swing/awt. so we will shut it down here
            // instead.
            setup.shutDown();
          }
        }
        System.exit(0);
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void showConnections() {
    out.println(getConnections());
  }

  private String getConnections() {
    final StringBuilder sb = new StringBuilder();
    if (server.getServerModel() != null && server.getServerModel().getMessenger() != null) {
      sb.append("Connected: ").append(server.getServerModel().getMessenger().isConnected()).append("\n")
          .append("Nodes: \n");
      final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
      if (nodes == null) {
        sb.append("  null\n");
      } else {
        for (final INode node : nodes) {
          sb.append("  ").append(node).append("\n");
        }
      }
    } else {
      sb.append("Not Connected to Anything");
    }
    return sb.toString();
  }

  private void showStatus() {
    out.println(getStatus());
  }

  private String getStatus() {
    return server.getStatus();
  }

  private void showHelp() {
    out.println("Available commands:\n"
        + "  help - show this message\n"
        + "  status - show status information\n"
        + "  dump - prints threads, memory, status, connections, to the log file\n"
        + "  connections - show all connected players\n"
        + "  mute - mute player\n"
        + "  boot - boot player\n"
        + "  ban - ban player\n"
        + "  send - sends a chat message\n"
        + "  chatmode - toggles the showing of chat messages as they come in\n"
        + "  chatlog - shows the chat log\n"
        + "  memory - show memory usage\n"
        + "  threads - get thread dumps\n"
        + "  save - saves game to filename\n"
        + "  stop - saves then stops current game and goes back to waiting\n"
        + "  quit - quit\n");
  }

}
