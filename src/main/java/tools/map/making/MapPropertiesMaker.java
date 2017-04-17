package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.ui.DoubleTextField;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;
import games.strategy.util.Tuple;
import tools.image.FileOpen;
import tools.image.FileSave;

/**
 * This is the MapPropertiesMaker, it will create a map.properties file for you. <br>
 * The map.properties is located in the map's directory, and it will tell TripleA various
 * display related information about your map. <br>
 * Such things as the dimensions of your map, the colors of each of the players,
 * the size of the unit images, and how zoomed out they are, etc. <br>
 * To use, just fill in the information in the fields below, and click on 'Show More' to
 * show other, optional, fields.
 */
public class MapPropertiesMaker extends JFrame {
  private static final long serialVersionUID = 8182821091131994702L;
  private static File s_mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
  private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
  private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
  private static final MapProperties s_mapProperties = new MapProperties();
  private static JPanel s_playerColorChooser = new JPanel();

  public static String[] getProperties() {
    return new String[] {TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT};
  }

  public static void main(final String[] args) {
    handleCommandLineArgs(args);
    // JOptionPane.showMessageDialog(null, new JLabel("<html>" + "This is the MapPropertiesMaker, it will create a
    // map.properties file for
    // you. " + "</html>"));
    if (s_mapFolderLocation == null) {
      System.out.println("Select the map folder");
      final String path = new FileSave("Where is your map's folder?", null, s_mapFolderLocation).getPathString();
      if (path != null) {
        final File mapFolder = new File(path);
        if (mapFolder.exists()) {
          s_mapFolderLocation = mapFolder;
          System.setProperty(TRIPLEA_MAP_FOLDER, s_mapFolderLocation.getPath());
        }
      }
    }
    if (s_mapFolderLocation != null) {
      final MapPropertiesMaker maker = new MapPropertiesMaker();
      maker.setSize(800, 800);
      maker.setLocationRelativeTo(null);
      maker.setVisible(true);
    } else {
      System.out.println("No Map Folder Selected. Shutting down.");
      System.exit(0);
    }
  }// end main

  public MapPropertiesMaker() {
    super("Map Properties Maker");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.getContentPane().setLayout(new BorderLayout());
    final JPanel panel = createPropertiesPanel();
    this.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
    // set up the actions
    final Action openAction = SwingAction.of("Load Properties", e -> loadProperties());
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Properties File");
    final Action saveAction = SwingAction.of("Save Properties", e -> saveProperties());
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Properties To File");
    final Action exitAction = SwingAction.of("Exit", e -> System.exit(0));
    exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
    // set up the menu items
    final JMenuItem openItem = new JMenuItem(openAction);
    openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
    final JMenuItem saveItem = new JMenuItem(saveAction);
    saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
    final JMenuItem exitItem = new JMenuItem(exitAction);
    // set up the menu bar
    final JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');
    // fileMenu.add(openItem);
    fileMenu.add(saveItem);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);
    menuBar.add(fileMenu);
  }

  private JPanel createPropertiesPanel() {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    int row = 0;
    panel.add(
        new JLabel("<html>" + "This is the MapPropertiesMaker, it will create a map.properties file for you. "
            + "<br>The map.properties is located in the map's directory, and it will tell TripleA various "
            + "<br>display related information about your map. "
            + "<br>Such things as the dimensions of your map, the colors of each of the players, "
            + "<br>the size of the unit images, and how zoomed out they are, etc. "
            + "<br>To use, just fill in the information in the fields below, and click on 'Show More' to "
            + "<br>show other, optional, fields. " + "</html>"),
        new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(20, 20, 20, 20), 0, 0));
    panel.add(new JLabel("The Width in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final IntTextField widthField = new IntTextField(0, Integer.MAX_VALUE);
    widthField.setText("" + s_mapProperties.getMapWidth());
    widthField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        try {
          s_mapProperties.setMapWidth(Integer.parseInt(widthField.getText()));
        } catch (final Exception ex) {
        }
        widthField.setText("" + s_mapProperties.getMapWidth());
      }
    });
    panel.add(widthField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    panel.add(new JLabel("The Height in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final IntTextField heightField = new IntTextField(0, Integer.MAX_VALUE);
    heightField.setText("" + s_mapProperties.getMapHeight());
    heightField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        try {
          s_mapProperties.setMapHeight(Integer.parseInt(heightField.getText()));
        } catch (final Exception ex) {
        }
        heightField.setText("" + s_mapProperties.getMapHeight());
      }
    });
    panel.add(heightField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    panel.add(
        new JLabel("<html>The initial Scale (zoom) of your unit images: "
            + "<br>Must be one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5</html>"),
        new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(10, 10, 10, 10), 0, 0));
    final DoubleTextField scaleField = new DoubleTextField(0.1d, 2.0d);
    scaleField.setText("" + s_mapProperties.getUnitsScale());
    scaleField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        try {
          // s_mapProperties.setUNITS_SCALE(Double.parseDouble(scaleField.getText()));
          s_mapProperties.setUnitsScale(scaleField.getText());
        } catch (final Exception ex) {
        }
        scaleField.setText("" + s_mapProperties.getUnitsScale());
      }
    });
    panel.add(scaleField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    panel.add(new JLabel("Create Players and Click on the Color to set their Color: "), new GridBagConstraints(0, row++,
        2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 50, 20, 50), 0, 0));
    createPlayerColorChooser();
    panel.add(s_playerColorChooser, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final JButton showMore = new JButton("Show All Options");
    showMore.addActionListener(SwingAction.of("Show All Options", e -> {
      final Tuple<PropertiesUI, List<MapPropertyWrapper<?>>> propertyWrapperUI =
          MapPropertiesMaker.s_mapProperties.propertyWrapperUI(true);
      JOptionPane.showMessageDialog(MapPropertiesMaker.this, propertyWrapperUI.getFirst());
      s_mapProperties.writePropertiesToObject(propertyWrapperUI.getSecond());
      MapPropertiesMaker.this.createPlayerColorChooser();
      MapPropertiesMaker.this.validate();
      MapPropertiesMaker.this.repaint();

    }));
    panel.add(showMore, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    return panel;
  }

  private void createPlayerColorChooser() {
    s_playerColorChooser.removeAll();
    s_playerColorChooser.setLayout(new GridBagLayout());
    int row = 0;
    for (final Entry<String, Color> entry : s_mapProperties.getColorMap().entrySet()) {
      s_playerColorChooser.add(new JLabel(entry.getKey()), new GridBagConstraints(0, row, 1, 1, 1, 1,
          GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      final JLabel label = new JLabel(entry.getKey()) {
        private static final long serialVersionUID = 5624227155029721033L;

        @Override
        public void paintComponent(final Graphics g) {
          final Graphics2D g2 = (Graphics2D) g;
          g2.setColor(entry.getValue());
          g2.fill(g2.getClip());
        }
      };
      label.setBackground(entry.getValue());
      label.addMouseListener(new MouseListener() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          System.out.println(label.getBackground());
          final Color color = JColorChooser.showDialog(label, "Choose color", label.getBackground());
          s_mapProperties.getColorMap().put(label.getText(), color);
          MapPropertiesMaker.this.createPlayerColorChooser();
          MapPropertiesMaker.this.validate();
          MapPropertiesMaker.this.repaint();
        }

        @Override
        public void mouseEntered(final MouseEvent e) {}

        @Override
        public void mouseExited(final MouseEvent e) {}

        @Override
        public void mousePressed(final MouseEvent e) {}

        @Override
        public void mouseReleased(final MouseEvent e) {}
      });
      s_playerColorChooser.add(label, new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      final JButton removePlayer = new JButton("Remove " + entry.getKey());
      removePlayer.addActionListener(new AbstractAction("Remove " + entry.getKey()) {
        private static final long serialVersionUID = -3593575469168341735L;

        @Override
        public void actionPerformed(final ActionEvent e) {
          s_mapProperties.getColorMap().remove(removePlayer.getText().replaceFirst("Remove ", ""));
          MapPropertiesMaker.this.createPlayerColorChooser();
          MapPropertiesMaker.this.validate();
          MapPropertiesMaker.this.repaint();
        }
      });
      s_playerColorChooser.add(removePlayer, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      row++;
    }
    final JTextField nameTextField = new JTextField("Player" + (s_mapProperties.getColorMap().size() + 1));
    final Dimension ourMinimum = new Dimension(150, 30);
    nameTextField.setMinimumSize(ourMinimum);
    nameTextField.setPreferredSize(ourMinimum);
    final JButton addPlayer = new JButton("Add Another Player");
    addPlayer.addActionListener(SwingAction.of("Add Another Player", e -> {
      s_mapProperties.getColorMap().put(nameTextField.getText(), Color.GREEN);
      MapPropertiesMaker.this.createPlayerColorChooser();
      MapPropertiesMaker.this.validate();
      MapPropertiesMaker.this.repaint();

    }));
    s_playerColorChooser.add(addPlayer, new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    s_playerColorChooser.add(nameTextField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
  }

  private void loadProperties() {
    final Properties properties = new Properties();
    try {
      System.out.println("Load a properties file");
      final String centerName =
          new FileOpen("Load A Properties File", s_mapFolderLocation, ".properties").getPathString();
      if (centerName == null) {
        return;
      }
      final FileInputStream in = new FileInputStream(centerName);
      properties.load(in);
    } catch (final HeadlessException | IOException ex) {
      ClientLogger.logQuietly(ex);
    }
    for (final Method setter : s_mapProperties.getClass().getMethods()) {
      final boolean startsWithSet = setter.getName().startsWith("set");
      if (!startsWithSet) {
        continue;
      }

      // TODO: finish this
    }
    validate();
    repaint();
  }

  private static void saveProperties() {
    try {
      final String fileName =
          new FileSave("Where To Save map.properties ?", "map.properties", s_mapFolderLocation).getPathString();
      if (fileName == null) {
        return;
      }
      final FileOutputStream sink = new FileOutputStream(fileName);
      final String stringToWrite = getOutPutString();
      final OutputStreamWriter out = new OutputStreamWriter(sink);
      out.write(stringToWrite);
      out.flush();
      out.close();
      System.out.println("");
      System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
      System.out.println("");
      System.out.println(stringToWrite);
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static String getOutPutString() {
    final StringBuilder outString = new StringBuilder();
    for (final Method outMethod : s_mapProperties.getClass().getMethods()) {
      final boolean startsWithSet = outMethod.getName().startsWith("out");
      if (!startsWithSet) {
        continue;
      }
      try {
        outString.append(outMethod.invoke(s_mapProperties));
      } catch (final IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return outString.toString();
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    final String[] properties = getProperties();
    boolean usagePrinted = false;
    for (final String arg2 : args) {
      boolean found = false;
      String arg = arg2;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        arg = arg.substring(0, indexOf);
        for (final String propertie : properties) {
          if (arg.equals(propertie)) {
            final String value = getValue(arg2);
            System.getProperties().setProperty(propertie, value);
            System.out.println(propertie + ":" + value);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        System.out.println("Unrecogized:" + arg2);
        if (!usagePrinted) {
          usagePrinted = true;
          System.out.println("Arguments\r\n" + "   " + TRIPLEA_MAP_FOLDER + "=<FILE_PATH>\r\n" + "   "
              + TRIPLEA_UNIT_ZOOM + "=<UNIT_ZOOM_LEVEL>\r\n" + "   " + TRIPLEA_UNIT_WIDTH + "=<UNIT_WIDTH>\r\n" + "   "
              + TRIPLEA_UNIT_HEIGHT + "=<UNIT_HEIGHT>\r\n");
        }
      }
    }
    // now account for anything set by -D
    final String folderString = System.getProperty(TRIPLEA_MAP_FOLDER);
    if (folderString != null && folderString.length() > 0) {
      final File mapFolder = new File(folderString);
      if (mapFolder.exists()) {
        s_mapFolderLocation = mapFolder;
      } else {
        System.out.println("Could not find directory: " + folderString);
      }
    }
    final String zoomString = System.getProperty(TRIPLEA_UNIT_ZOOM);
    if (zoomString != null && zoomString.length() > 0) {
      try {
        final double unit_zoom_percent = Double.parseDouble(zoomString);
        // s_mapProperties.setUNITS_SCALE(unit_zoom_percent);
        s_mapProperties.setUnitsScale(zoomString);
        System.out.println("Unit Zoom Percent to use: " + unit_zoom_percent);
      } catch (final Exception ex) {
        System.err.println("Not a decimal percentage: " + zoomString);
      }
    }
    final String widthString = System.getProperty(TRIPLEA_UNIT_WIDTH);
    if (widthString != null && widthString.length() > 0) {
      try {
        final int unit_width = Integer.parseInt(widthString);
        s_mapProperties.setUnitsWidth(unit_width);
        s_mapProperties.setUnitsCounterOffsetWidth(unit_width / 4);
        System.out.println("Unit Width to use: " + unit_width);
      } catch (final Exception ex) {
        System.err.println("Not an integer: " + widthString);
      }
    }
    final String heightString = System.getProperty(TRIPLEA_UNIT_HEIGHT);
    if (heightString != null && heightString.length() > 0) {
      try {
        final int unit_height = Integer.parseInt(heightString);
        s_mapProperties.setUnitsHeight(unit_height);
        s_mapProperties.setUnitsCounterOffsetHeight(unit_height);
        System.out.println("Unit Height to use: " + unit_height);
      } catch (final Exception ex) {
        System.err.println("Not an integer: " + heightString);
      }
    }
  }
}
