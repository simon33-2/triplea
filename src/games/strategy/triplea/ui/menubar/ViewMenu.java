package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.PurchasePanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.ui.SwingAction;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;

public class ViewMenu {
  private JCheckBoxMenuItem showMapDetails;
  private JCheckBoxMenuItem showMapBlends;

  private final GameData gameData;
  private final TripleAFrame frame;
  private final IUIContext uiContext;

  public ViewMenu(final JMenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    this.uiContext = frame.getUIContext();
    gameData = frame.getGame().getData();


    final JMenu menuView = new JMenu("View");
    menuView.setMnemonic(KeyEvent.VK_V);
    menuBar.add(menuView);
    addZoomMenu(menuView);
    addUnitSizeMenu(menuView);
    addLockMap(menuView);
    addShowUnits(menuView);
    addUnitNationDrawMenu(menuView);
    if (uiContext.getMapData().useTerritoryEffectMarkers()) {
      addShowTerritoryEffects(menuView);
    }
    addMapSkinsMenu(menuView);
    addShowMapDetails(menuView);
    addShowMapBlends(menuView);
    addDrawTerritoryBordersAgain(menuView);
    addMapFontAndColorEditorMenu(menuView);
    addChatTimeMenu(menuView);
    addShowCommentLog(menuView);
    // The menuItem to turn TabbedProduction on or off
    addTabbedProduction(menuView);
    addShowGameUuid(menuView);
    addSetLookAndFeel(menuView);

    showMapDetails.setEnabled(uiContext.getMapData().getHasRelief());

  }

  private void addShowCommentLog(final JMenu parentMenu) {
    final JCheckBoxMenuItem showCommentLog = new JCheckBoxMenuItem("Show Comment Log");
    showCommentLog.setModel(frame.getShowCommentLogButtonModel());
    parentMenu.add(showCommentLog).setMnemonic(KeyEvent.VK_L);
  }

  private static void addTabbedProduction(final JMenu parentMenu) {
    final JCheckBoxMenuItem tabbedProduction = new JCheckBoxMenuItem("Show Production Tabs");
    tabbedProduction.setMnemonic(KeyEvent.VK_P);
    tabbedProduction.setSelected(PurchasePanel.isTabbedProduction());
    tabbedProduction
        .addActionListener(SwingAction.of(e -> PurchasePanel.setTabbedProduction(tabbedProduction.isSelected())));
    parentMenu.add(tabbedProduction);
  }

  private void addShowGameUuid(final JMenu menuView) {
    menuView.add(SwingAction.of("Game UUID", e -> {
      final String id = (String) gameData.getProperties().get(GameData.GAME_UUID);
      final JTextField text = new JTextField();
      text.setText(id);
      final JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.add(new JLabel("Game UUID:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0));
      JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(menuView), panel, "Game UUID",
          JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"}, "OK");
    })).setMnemonic(KeyEvent.VK_U);
  }

  private void addSetLookAndFeel(final JMenu menuView) {
    final String lookAndFeelTitle = "Set Look and Feel";
    menuView.add(SwingAction.of(lookAndFeelTitle, e -> {
      final Triple<JList<String>, Map<String, String>, String> lookAndFeel = TripleAMenuBar.getLookAndFeelList();
      final JList<String> list = lookAndFeel.getFirst();
      final String currentKey = lookAndFeel.getThird();
      final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
      if (JOptionPane.showConfirmDialog(frame, list, lookAndFeelTitle,
          JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        final String selectedValue = list.getSelectedValue();
        if (selectedValue == null) {
          return;
        }
        if (selectedValue.equals(currentKey)) {
          return;
        }
        LookAndFeel.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
        EventThreadJOptionPane.showMessageDialog(frame, "The look and feel will update when you restart TripleA",
            new CountDownLatchHandler(true));
      }
    })).setMnemonic(KeyEvent.VK_F);
  }



  private void addZoomMenu(final JMenu menuGame) {
    final Action mapZoom = SwingAction.of("Map Zoom", e -> {
      final SpinnerNumberModel model = new SpinnerNumberModel();
      model.setMaximum(100);
      model.setMinimum(15);
      model.setStepSize(1);
      model.setValue((int) (frame.getMapPanel().getScale() * 100));
      final JSpinner spinner = new JSpinner(model);
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(new JLabel("Choose Map Scale Percentage"), BorderLayout.NORTH);
      panel.add(spinner, BorderLayout.CENTER);
      final JPanel buttons = new JPanel();
      final JButton fitWidth = new JButton("Fit Width");
      buttons.add(fitWidth);
      final JButton fitHeight = new JButton("Fit Height");
      buttons.add(fitHeight);
      final JButton reset = new JButton("Reset");
      buttons.add(reset);
      panel.add(buttons, BorderLayout.SOUTH);
      fitWidth.addActionListener(event -> {
        final double screenWidth = frame.getMapPanel().getWidth();
        final double mapWidth = frame.getMapPanel().getImageWidth();
        double ratio = screenWidth / mapWidth;
        ratio = Math.max(0.15, ratio);
        ratio = Math.min(1, ratio);
        model.setValue((int) (ratio * 100));
      });
      fitHeight.addActionListener(event -> {
        final double screenHeight = frame.getMapPanel().getHeight();
        final double mapHeight = frame.getMapPanel().getImageHeight();
        double ratio = screenHeight / mapHeight;
        ratio = Math.max(0.15, ratio);
        model.setValue((int) (ratio * 100));
      });
      reset.addActionListener(event -> model.setValue(100));
      final int result = JOptionPane.showOptionDialog(frame, panel, "Choose Map Scale", JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE, null, new String[] {"OK", "Cancel"}, 0);
      if (result != 0) {
        return;
      }
      final Number value = (Number) model.getValue();
      frame.setScale(value.doubleValue());

    });
    menuGame.add(mapZoom).setMnemonic(KeyEvent.VK_Z);
  }

  private void addUnitSizeMenu(final JMenu parentMenu) {
    final NumberFormat s_decimalFormat = new DecimalFormat("00.##");
    // This is the action listener used
    class UnitSizeAction extends AbstractAction {
      private static final long serialVersionUID = -6280511505686687867L;
      private final double scaleFactor;

      public UnitSizeAction(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
        putValue(Action.NAME, s_decimalFormat.format(scaleFactor * 100) + "%");
      }

      @Override
      public void actionPerformed(final ActionEvent e) {
        uiContext.setUnitScaleFactor(scaleFactor);
        frame.getMapPanel().resetMap();
      }
    }
    final JMenu unitSizeMenu = new JMenu();
    unitSizeMenu.setMnemonic(KeyEvent.VK_S);
    unitSizeMenu.setText("Unit Size");
    final ButtonGroup unitSizeGroup = new ButtonGroup();
    final JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));
    final JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
    radioItem100.setMnemonic(KeyEvent.VK_1);
    final JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
    final JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
    radioItem83.setMnemonic(KeyEvent.VK_8);
    final JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
    radioItem75.setMnemonic(KeyEvent.VK_7);
    final JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
    radioItem66.setMnemonic(KeyEvent.VK_6);
    final JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
    final JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));
    radioItem50.setMnemonic(KeyEvent.VK_5);
    unitSizeGroup.add(radioItem125);
    unitSizeGroup.add(radioItem100);
    unitSizeGroup.add(radioItem87);
    unitSizeGroup.add(radioItem83);
    unitSizeGroup.add(radioItem75);
    unitSizeGroup.add(radioItem66);
    unitSizeGroup.add(radioItem56);
    unitSizeGroup.add(radioItem50);
    radioItem100.setSelected(true);
    // select the closest to to the default size
    final Enumeration<AbstractButton> enum1 = unitSizeGroup.getElements();
    boolean matchFound = false;
    while (enum1.hasMoreElements()) {
      final JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) enum1.nextElement();
      final UnitSizeAction action = (UnitSizeAction) menuItem.getAction();
      if (Math.abs(action.scaleFactor - uiContext.getUnitImageFactory().getScaleFactor()) < 0.01) {
        menuItem.setSelected(true);
        matchFound = true;
        break;
      }
    }
    if (!matchFound) {
      System.err.println("default unit size does not match any menu item");
    }
    unitSizeMenu.add(radioItem125);
    unitSizeMenu.add(radioItem100);
    unitSizeMenu.add(radioItem87);
    unitSizeMenu.add(radioItem83);
    unitSizeMenu.add(radioItem75);
    unitSizeMenu.add(radioItem66);
    unitSizeMenu.add(radioItem56);
    unitSizeMenu.add(radioItem50);
    parentMenu.add(unitSizeMenu);
  }

  private void addMapSkinsMenu(final JMenu menuGame) {
    final JMenu mapSubMenu = new JMenu("Map Skins");
    mapSubMenu.setMnemonic(KeyEvent.VK_K);
    menuGame.add(mapSubMenu);
    final ButtonGroup mapButtonGroup = new ButtonGroup();
    final Map<String, String> skins = AbstractUIContext.getSkins(frame.getGame().getData());
    mapSubMenu.setEnabled(skins.size() > 1);
    for (final String key : skins.keySet()) {
      final JMenuItem mapMenuItem = new JRadioButtonMenuItem(key);
      mapButtonGroup.add(mapMenuItem);
      mapSubMenu.add(mapMenuItem);
      if (skins.get(key).equals(AbstractUIContext.getMapDir())) {
        mapMenuItem.setSelected(true);
      }
      mapMenuItem.addActionListener(e -> {
        try {
          frame.updateMap(skins.get(key));
          if (uiContext.getMapData().getHasRelief()) {
            showMapDetails.setSelected(true);
          }
          showMapDetails.setEnabled(uiContext.getMapData().getHasRelief());
        } catch (final Exception exception) {
          ClientLogger.logError("Error Changing Map Skin2", exception);
        }
      });
    }
  }

  private void addShowMapDetails(final JMenu menuGame) {
    showMapDetails = new JCheckBoxMenuItem("Show Map Details");
    showMapDetails.setMnemonic(KeyEvent.VK_D);
    showMapDetails.setSelected(TileImageFactory.getShowReliefImages());
    showMapDetails.addActionListener(e -> {
      if (TileImageFactory.getShowReliefImages() == showMapDetails.isSelected()) {
        return;
      }
      TileImageFactory.setShowReliefImages(showMapDetails.isSelected());
      final Thread t = new Thread("Triplea : Show map details thread") {
        @Override
        public void run() {
          yield();
          frame.getMapPanel().updateCountries(gameData.getMap().getTerritories());
        }
      };
      t.start();
    });
    menuGame.add(showMapDetails);
  }

  private void addShowMapBlends(final JMenu menuGame) {
    showMapBlends = new JCheckBoxMenuItem("Show Map Blends");
    showMapBlends.setMnemonic(KeyEvent.VK_B);
    if (uiContext.getMapData().getHasRelief() && showMapDetails.isEnabled() && showMapDetails.isSelected()) {
      showMapBlends.setEnabled(true);
      showMapBlends.setSelected(TileImageFactory.getShowMapBlends());
    } else {
      showMapBlends.setSelected(false);
      showMapBlends.setEnabled(false);
    }
    showMapBlends.addActionListener(e -> {
      if (TileImageFactory.getShowMapBlends() == showMapBlends.isSelected()) {
        return;
      }
      TileImageFactory.setShowMapBlends(showMapBlends.isSelected());
      TileImageFactory.setShowMapBlendMode(uiContext.getMapData().getMapBlendMode());
      TileImageFactory.setShowMapBlendAlpha(uiContext.getMapData().getMapBlendAlpha());
      final Thread t = new Thread("Triplea : Show map Blends thread") {
        @Override
        public void run() {
          frame.setScale(uiContext.getScale() * 100);
          yield();
          frame.getMapPanel().updateCountries(gameData.getMap().getTerritories());
        }
      };
      t.start();
    });
    menuGame.add(showMapBlends);
  }

  private void addShowUnits(final JMenu parentMenu) {
    final JCheckBoxMenuItem showUnitsBox = new JCheckBoxMenuItem("Show Units");
    showUnitsBox.setMnemonic(KeyEvent.VK_U);
    showUnitsBox.setSelected(true);
    showUnitsBox.addActionListener(e -> {
      final boolean tfselected = showUnitsBox.isSelected();
      uiContext.setShowUnits(tfselected);
      frame.getMapPanel().resetMap();
    });
    parentMenu.add(showUnitsBox);
  }

  private void addDrawTerritoryBordersAgain(final JMenu parentMenu) {
    final JMenu drawBordersMenu = new JMenu();
    drawBordersMenu.setMnemonic(KeyEvent.VK_O);
    drawBordersMenu.setText("Draw Borders On Top");
    final JRadioButton noneButton = new JRadioButton("Low");
    noneButton.setMnemonic(KeyEvent.VK_L);
    final JRadioButton mediumButton = new JRadioButton("Medium");
    mediumButton.setMnemonic(KeyEvent.VK_M);
    final JRadioButton highButton = new JRadioButton("High");
    highButton.setMnemonic(KeyEvent.VK_H);
    final ButtonGroup group = new ButtonGroup();
    group.add(noneButton);
    group.add(mediumButton);
    group.add(highButton);
    drawBordersMenu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(final MenuEvent e) {
        final IDrawable.OptionalExtraBorderLevel current = uiContext.getDrawTerritoryBordersAgain();
        if (current == IDrawable.OptionalExtraBorderLevel.LOW) {
          noneButton.setSelected(true);
        } else if (current == IDrawable.OptionalExtraBorderLevel.MEDIUM) {
          mediumButton.setSelected(true);
        } else if (current == IDrawable.OptionalExtraBorderLevel.HIGH) {
          highButton.setSelected(true);
        }
      }

      @Override
      public void menuDeselected(final MenuEvent e) {}

      @Override
      public void menuCanceled(final MenuEvent e) {}
    });
    noneButton.addActionListener(e -> {
      if (noneButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.LOW) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.LOW);
        frame.getMapPanel().resetMap();
      }
    });
    mediumButton.addActionListener(e -> {
      if (mediumButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.MEDIUM) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.MEDIUM);
        frame.getMapPanel().resetMap();
      }
    });
    highButton.addActionListener(e -> {
      if (highButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.HIGH) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.HIGH);
        frame.getMapPanel().resetMap();
      }
    });
    drawBordersMenu.add(noneButton);
    drawBordersMenu.add(mediumButton);
    drawBordersMenu.add(highButton);
    parentMenu.add(drawBordersMenu);
  }

  private void addMapFontAndColorEditorMenu(final JMenu parentMenu) {
    final Action mapFontOptions = SwingAction.of("Edit Map Font and Color", e -> {
      final List<IEditableProperty> properties = new ArrayList<>();
      final NumberProperty fontsize =
          new NumberProperty("Font Size", null, 60, 0, MapImage.getPropertyMapFont().getSize());
      final ColorProperty territoryNameColor = new ColorProperty("Territory Name and PU Color", null,
          MapImage.getPropertyTerritoryNameAndPUAndCommentcolor());
      final ColorProperty unitCountColor =
          new ColorProperty("Unit Count Color", null, MapImage.getPropertyUnitCountColor());
      final ColorProperty factoryDamageColor =
          new ColorProperty("Factory Damage Color", null, MapImage.getPropertyUnitFactoryDamageColor());
      final ColorProperty hitDamageColor =
          new ColorProperty("Hit Damage Color", null, MapImage.getPropertyUnitHitDamageColor());
      properties.add(fontsize);
      properties.add(territoryNameColor);
      properties.add(unitCountColor);
      properties.add(factoryDamageColor);
      properties.add(hitDamageColor);
      final PropertiesUI pui = new PropertiesUI(properties, true);
      final JPanel ui = new JPanel();
      ui.setLayout(new BorderLayout());
      ui.add(pui, BorderLayout.CENTER);
      ui.add(
          new JLabel("<html>Change the font and color of 'text' (not pictures) on the map. "
              + "<br /><em>(Some people encounter problems with the color picker, and this "
              + "<br />is a bug outside of triplea, located in the 'look and feel' that "
              + "<br />you are using. If you have an error come up, try switching to the "
              + "<br />basic 'look and feel', then setting the color, then switching back.)</em></html>"),
          BorderLayout.NORTH);
      final Object[] options = {"Set Properties", "Reset To Default", "Cancel"};
      final int result = JOptionPane.showOptionDialog(frame, ui, "Edit Map Font and Color",
          JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, 2);
      if (result == 2) {
      } else if (result == 1) {
        MapImage.resetPropertyMapFont();
        MapImage.resetPropertyTerritoryNameAndPUAndCommentcolor();
        MapImage.resetPropertyUnitCountColor();
        MapImage.resetPropertyUnitFactoryDamageColor();
        MapImage.resetPropertyUnitHitDamageColor();
        frame.getMapPanel().resetMap();
      } else if (result == 0) {
        MapImage.setPropertyMapFont(new Font("Ariel", Font.BOLD, fontsize.getValue()));
        MapImage.setPropertyTerritoryNameAndPUAndCommentcolor((Color) territoryNameColor.getValue());
        MapImage.setPropertyUnitCountColor((Color) unitCountColor.getValue());
        MapImage.setPropertyUnitFactoryDamageColor((Color) factoryDamageColor.getValue());
        MapImage.setPropertyUnitHitDamageColor((Color) hitDamageColor.getValue());
        frame.getMapPanel().resetMap();
      }

    });
    parentMenu.add(mapFontOptions).setMnemonic(KeyEvent.VK_C);
  }

  private void addShowTerritoryEffects(final JMenu parentMenu) {
    final JCheckBoxMenuItem territoryEffectsBox = new JCheckBoxMenuItem("Show TerritoryEffects");
    territoryEffectsBox.setMnemonic(KeyEvent.VK_T);
    territoryEffectsBox.addActionListener(e -> {
      final boolean tfselected = territoryEffectsBox.isSelected();
      uiContext.setShowTerritoryEffects(tfselected);
      frame.getMapPanel().resetMap();
    });
    parentMenu.add(territoryEffectsBox);
    territoryEffectsBox.setSelected(true);
  }

  private void addLockMap(final JMenu parentMenu) {
    final JCheckBoxMenuItem lockMapBox = new JCheckBoxMenuItem("Lock Map");
    lockMapBox.setMnemonic(KeyEvent.VK_M);
    lockMapBox.setSelected(uiContext.getLockMap());
    lockMapBox.addActionListener(e -> uiContext.setLockMap(lockMapBox.isSelected()));
    parentMenu.add(lockMapBox);
  }

  private void addUnitNationDrawMenu(final JMenu parentMenu) {
    final JMenu unitSizeMenu = new JMenu();
    unitSizeMenu.setMnemonic(KeyEvent.VK_N);
    unitSizeMenu.setText("Flag Display Mode");

    final Preferences prefs = Preferences.userNodeForPackage(getClass());
    final UnitsDrawer.UnitFlagDrawMode setting = Enum.valueOf(UnitsDrawer.UnitFlagDrawMode.class,
        prefs.get(UnitsDrawer.PreferenceKeys.DRAW_MODE.name(), UnitsDrawer.UnitFlagDrawMode.NEXT_TO.toString()));
    UnitsDrawer.setUnitFlagDrawMode(setting, prefs);
    UnitsDrawer.enabledFlags =
        prefs.getBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), UnitsDrawer.enabledFlags);

    final JCheckBoxMenuItem toggleFlags = new JCheckBoxMenuItem("Show Unit Flags");
    toggleFlags.setSelected(UnitsDrawer.enabledFlags);
    toggleFlags.addActionListener(e -> {
      UnitsDrawer.enabledFlags = toggleFlags.isSelected();
      prefs.putBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), toggleFlags.isSelected());
      frame.getMapPanel().resetMap();
    });
    unitSizeMenu.add(toggleFlags);

    final ButtonGroup unitFlagSettingGroup = new ButtonGroup();
    unitSizeMenu.add(createFlagDrawModeRadionButtonItem("Small", unitFlagSettingGroup,
        UnitsDrawer.UnitFlagDrawMode.NEXT_TO, setting, prefs));
    unitSizeMenu.add(createFlagDrawModeRadionButtonItem("Large", unitFlagSettingGroup,
        UnitsDrawer.UnitFlagDrawMode.BELOW, setting, prefs));
    parentMenu.add(unitSizeMenu);
  }

  private JRadioButtonMenuItem createFlagDrawModeRadionButtonItem(final String text, final ButtonGroup group,
      final UnitsDrawer.UnitFlagDrawMode drawMode, final UnitsDrawer.UnitFlagDrawMode setting,
      final Preferences prefs) {
    return createRadioButtonItem(text, group, SwingAction.of(e -> {
      UnitsDrawer.setUnitFlagDrawMode(drawMode, prefs);
      frame.getMapPanel().resetMap();
    }), setting.equals(drawMode));
  }

  private JRadioButtonMenuItem createRadioButtonItem(final String text, final ButtonGroup group, final Action action,
      final boolean selected) {
    final JRadioButtonMenuItem buttonItem = new JRadioButtonMenuItem(text);
    buttonItem.addActionListener(action);
    buttonItem.setSelected(selected);
    group.add(buttonItem);
    return buttonItem;
  }

  private void addChatTimeMenu(final JMenu parentMenu) {
    final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
    chatTimeBox.setMnemonic(KeyEvent.VK_T);
    chatTimeBox.addActionListener(e -> frame.setShowChatTime(chatTimeBox.isSelected()));
    chatTimeBox.setSelected(false);
    parentMenu.add(chatTimeBox);
    chatTimeBox.setEnabled(MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null);
  }
}
