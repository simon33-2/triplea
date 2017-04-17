package games.strategy.triplea.ai.proAI.logging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;

/**
 * GUI class used to display logging window and logging settings.
 */
public class ProLogWindow extends javax.swing.JDialog {
  private static final long serialVersionUID = -5989598624017028122L;

  /** Creates new form ProLogWindow. */
  public ProLogWindow(final TripleAFrame frame) {
    super(frame);
    initComponents();
  }

  public void clear() {
    this.dispose();
    v_tabPaneMain = null;
    v_logHolderTabbedPane = null;
  }

  private void initComponents() {
    final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    java.awt.GridBagConstraints gridBagConstraints;
    final JPanel jPanel7 = new JPanel();
    final JButton v_restoreDefaultsButton = new JButton();
    final JButton v_settingsDetailsButton = new JButton();
    final JPanel jPanel14 = new JPanel();
    final JPanel jPanel13 = new JPanel();
    final JButton v_cancelButton = new JButton();
    final JButton v_okButton = new JButton();
    v_tabPaneMain = new javax.swing.JTabbedPane();
    final JPanel jPanel8 = new JPanel();
    v_logHolderTabbedPane = new javax.swing.JTabbedPane();
    final JPanel jPanel9 = new JPanel();
    final JScrollPane v_aiOutputLogAreaScrollPane = new JScrollPane();
    v_aiOutputLogArea = new javax.swing.JTextArea();
    v_enableAILogging = new javax.swing.JCheckBox();
    final javax.swing.JLabel jLabel15 = new javax.swing.JLabel();
    v_logDepth = new javax.swing.JComboBox<>();
    v_limitLogHistoryToSpinner = new javax.swing.JSpinner();
    v_limitLogHistoryCB = new javax.swing.JCheckBox();
    final javax.swing.JLabel jLabel46 = new javax.swing.JLabel();
    final JPanel v_pauseAIs = new JPanel();
    setTitle("Hard AI Settings");
    setMinimumSize(new java.awt.Dimension(775, 400));
    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(final java.awt.event.WindowEvent evt) {
        formWindowClosing();
      }

      @Override
      public void windowOpened(final java.awt.event.WindowEvent evt) {
        formWindowOpened();
      }
    });
    getContentPane().setLayout(new java.awt.GridBagLayout());
    jPanel7.setName("jPanel3");
    jPanel7.setPreferredSize(new java.awt.Dimension(600, 45));
    jPanel7.setLayout(new java.awt.GridBagLayout());
    v_restoreDefaultsButton.setText("Restore Defaults");
    v_restoreDefaultsButton.setMinimumSize(new java.awt.Dimension(118, 23));
    v_restoreDefaultsButton.setName("v_restoreDefaultsButton");
    v_restoreDefaultsButton.setPreferredSize(new java.awt.Dimension(118, 23));
    v_restoreDefaultsButton.addActionListener(evt -> v_restoreDefaultsButtonActionPerformed());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(11, 0, 11, 0);
    jPanel7.add(v_restoreDefaultsButton, gridBagConstraints);
    v_settingsDetailsButton.setText("Settings Details");
    v_settingsDetailsButton.setMinimumSize(new java.awt.Dimension(115, 23));
    v_settingsDetailsButton.setName("v_settingsDetailsButton");
    v_settingsDetailsButton.setPreferredSize(new java.awt.Dimension(115, 23));
    v_settingsDetailsButton.addActionListener(evt -> v_settingsDetailsButtonActionPerformed());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(11, 6, 11, 0);
    jPanel7.add(v_settingsDetailsButton, gridBagConstraints);
    jPanel14.setName("jPanel14");
    jPanel14.setLayout(new java.awt.GridBagLayout());
    jPanel13.setName("jPanel13");
    jPanel13.setLayout(new java.awt.GridBagLayout());
    v_cancelButton.setText("Cancel");
    v_cancelButton.setName("v_cancelButton");
    v_cancelButton.addActionListener(evt -> v_cancelButtonActionPerformed());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    jPanel13.add(v_cancelButton, gridBagConstraints);
    v_okButton.setText("OK");
    v_okButton.setName("v_okButton");
    v_okButton.addActionListener(evt -> v_okButtonActionPerformed());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    jPanel13.add(v_okButton, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    jPanel14.add(jPanel13, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new java.awt.Insets(11, 6, 11, 0);
    jPanel7.add(jPanel14, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 7);
    getContentPane().add(jPanel7, gridBagConstraints);
    v_tabPaneMain.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
    v_tabPaneMain.setName("v_tabPaneMain");
    v_tabPaneMain.setPreferredSize(new java.awt.Dimension(500, screenSize.height - 200));
    jPanel8.setName("jPanel8");
    jPanel8.setPreferredSize(new java.awt.Dimension(500, 314));
    jPanel8.setLayout(new java.awt.GridBagLayout());
    v_logHolderTabbedPane.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
    v_logHolderTabbedPane.setFont(new java.awt.Font("Segoe UI", 0, 10));
    v_logHolderTabbedPane.setName("v_logHolderTabbedPane");
    jPanel9.setName("jPanel9");
    jPanel9.setLayout(new java.awt.GridLayout(1, 0));
    v_aiOutputLogAreaScrollPane.setName("v_aiOutputLogAreaScrollPane");
    v_aiOutputLogArea.setColumns(20);
    v_aiOutputLogArea.setEditable(false);
    v_aiOutputLogArea.setFont(new java.awt.Font("Segoe UI", 0, 10));
    v_aiOutputLogArea.setRows(5);
    v_aiOutputLogArea.setName("v_aiOutputLogArea");
    v_aiOutputLogAreaScrollPane.setViewportView(v_aiOutputLogArea);
    jPanel9.add(v_aiOutputLogAreaScrollPane);
    v_logHolderTabbedPane.addTab("Pre-Game", jPanel9);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.weighty = 99.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 7, 7);
    jPanel8.add(v_logHolderTabbedPane, gridBagConstraints);
    v_enableAILogging.setSelected(true);
    v_enableAILogging.setText("Enable AI Logging");
    v_enableAILogging.setName("v_enableAILogging");
    v_enableAILogging.addChangeListener(evt -> v_enableAILoggingStateChanged());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 0, 0);
    jPanel8.add(v_enableAILogging, gridBagConstraints);
    jLabel15.setText("Log Depth:");
    jLabel15.setName("jLabel15");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    jPanel8.add(jLabel15, gridBagConstraints);
    v_logDepth.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Fine", "Finer", "Finest"}));
    v_logDepth.setSelectedItem(v_logDepth.getItemAt(2));
    v_logDepth.setName("v_logDepth");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(7, 5, 0, 0);
    jPanel8.add(v_logDepth, gridBagConstraints);
    v_limitLogHistoryToSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 1, 100, 1));
    v_limitLogHistoryToSpinner.setMinimumSize(new java.awt.Dimension(60, 20));
    v_limitLogHistoryToSpinner.setName("v_limitLogHistoryToSpinner");
    v_limitLogHistoryToSpinner.setPreferredSize(new java.awt.Dimension(60, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.ipadx = 10;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    jPanel8.add(v_limitLogHistoryToSpinner, gridBagConstraints);
    v_limitLogHistoryCB.setSelected(true);
    v_limitLogHistoryCB.setText("Limit Log History To:");
    v_limitLogHistoryCB.setName("v_limitLogHistoryCB");
    v_limitLogHistoryCB.addChangeListener(evt -> v_limitLogHistoryCBStateChanged());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 12);
    jPanel8.add(v_limitLogHistoryCB, gridBagConstraints);
    jLabel46.setText("rounds");
    jLabel46.setName("jLabel46");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(7, 5, 0, 7);
    jPanel8.add(jLabel46, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    jPanel8.add(v_pauseAIs, gridBagConstraints);
    v_tabPaneMain.addTab("Debugging", jPanel8);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.weighty = 99.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 0, 7);
    getContentPane().add(v_tabPaneMain, gridBagConstraints);
    setBounds((screenSize.width - 800), 25, 775, 401);
  }

  private void formWindowOpened() {
    loadSettings(ProLogSettings.loadSettings());
    this.pack();
  }

  /**
   * Loads the settings provided and displays it in this settings window.
   *
   * @param settings
   */
  private void loadSettings(final ProLogSettings settings) {
    v_enableAILogging.setSelected(settings.EnableAILogging);
    if (settings.AILoggingDepth.equals(Level.FINE)) {
      v_logDepth.setSelectedIndex(0);
    } else if (settings.AILoggingDepth.equals(Level.FINER)) {
      v_logDepth.setSelectedIndex(1);
    } else if (settings.AILoggingDepth.equals(Level.FINEST)) {
      v_logDepth.setSelectedIndex(2);
    }
    v_limitLogHistoryCB.setSelected(settings.LimitLogHistory);
    v_limitLogHistoryToSpinner.setValue(settings.LimitLogHistoryTo);
  }

  public ProLogSettings createSettings() {
    final ProLogSettings settings = new ProLogSettings();
    settings.EnableAILogging = v_enableAILogging.isSelected();
    if (v_logDepth.getSelectedIndex() == 0) {
      settings.AILoggingDepth = Level.FINE;
    } else if (v_logDepth.getSelectedIndex() == 1) {
      settings.AILoggingDepth = Level.FINER;
    } else if (v_logDepth.getSelectedIndex() == 2) {
      settings.AILoggingDepth = Level.FINEST;
    }
    settings.LimitLogHistory = v_limitLogHistoryCB.isSelected();
    settings.LimitLogHistoryTo = Integer.parseInt(v_limitLogHistoryToSpinner.getValue().toString());
    return settings;
  }

  private void v_restoreDefaultsButtonActionPerformed() {
    final int result = JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to reset all AI settings?",
        "Reset Default Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      // Default settings are already contained in a new DSettings instance
      final ProLogSettings defaultSettings = new ProLogSettings();
      loadSettings(defaultSettings);
      JOptionPane.showMessageDialog(rootPane,
          "Default settings restored.\r\n\r\n(If you don't want to keep these default settings, just hit cancel)",
          "Default Settings Restored", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void v_enableAILoggingStateChanged() {
    v_logDepth.setEnabled(v_enableAILogging.isSelected());
    v_limitLogHistoryCB.setEnabled(v_enableAILogging.isSelected());
  }

  private void v_limitLogHistoryCBStateChanged() {
    v_limitLogHistoryToSpinner.setEnabled(v_limitLogHistoryCB.isSelected() && v_enableAILogging.isSelected());
  }

  private void formWindowClosing() {
    v_cancelButtonActionPerformed();
  }

  private void v_okButtonActionPerformed() {
    final ProLogSettings settings = createSettings();
    ProLogSettings.saveSettings(settings);
    this.setVisible(false);
  }

  private void v_cancelButtonActionPerformed() {
    final ProLogSettings settings = ProLogSettings.loadSettings();
    loadSettings(settings);
    this.setVisible(false);
  }

  private void v_settingsDetailsButtonActionPerformed() {
    final JDialog dialog = new JDialog(this, "Pro AI - Settings Details");
    String message = "";
    if (v_tabPaneMain.getSelectedIndex() == 0) // Debugging
    {
      message = "Debugging\r\n" + "\r\n"
          + "AI Logging: When this is checked, the AI's will output their logs, as they come in, so you can see exactly what the AI is thinking.\r\n"
          + "Note that if you check this on, you still have to press OK then reopen the settings window for the logs to actually start displaying.\r\n"
          + "\r\n"
          + "Log Depth: This setting lets you choose how deep you want the AI logging to be. Fine only displays the high-level events, like the start of a phase, etc.\r\n"
          + "Finer displays medium-level events, such as attacks, reinforcements, etc.\r\n"
          + "Finest displays all the AI logging available. Can be used for detailed ananlysis, but is a lot harder to read through it.\r\n"
          + "\r\n"
          + "Pause AI's: This checkbox pauses all the AI's while it's checked, so you can look at the logs without the AI's outputing floods of information.\r\n"
          + "\r\n"
          + "Limit Log History To X Rounds: If this is checked, the AI log information will be limited to X rounds of information.\r\n";
    }
    final JTextArea label = new JTextArea(message);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    label.setEditable(false);
    label.setAutoscrolls(true);
    label.setLineWrap(false);
    label.setFocusable(false);
    label.setWrapStyleWord(true);
    label.setLocation(0, 0);
    dialog.setBackground(label.getBackground());
    dialog.setLayout(new BorderLayout());
    final JScrollPane pane = new JScrollPane();
    pane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    pane.setViewportView(label);
    dialog.add(pane, BorderLayout.CENTER);
    final JButton button = new JButton(SwingAction.of(e -> dialog.dispose()));
    button.setText("Close");
    button.setMinimumSize(new Dimension(100, 30));
    dialog.add(button, BorderLayout.SOUTH);
    dialog.setMinimumSize(new Dimension(500, 300));
    dialog.setSize(new Dimension(800, 600));
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(this);
    dialog.setDefaultCloseOperation(2);
    dialog.setVisible(true);
  }

  private JTextArea currentLogTextArea = null;

  public void addMessage(final Level level, final String message) {
    try {
      if (currentLogTextArea == null) {
        currentLogTextArea = v_aiOutputLogArea;
      }
      currentLogTextArea.append(message + "\r\n");
    } catch (final NullPointerException ex) // This is bad, but we don't want TripleA crashing because of this...
    {
      System.out.print("Error adding Pro log message! Level: " + level.getName() + " Message: " + message);
    }
  }

  public void notifyNewRound(final int roundNumber, final String name) {
    SwingAction.invokeAndWait(() -> {
      final JPanel newPanel = new JPanel();
      final JScrollPane newScrollPane = new JScrollPane();
      final JTextArea newTextArea = new JTextArea();
      newTextArea.setColumns(20);
      newTextArea.setRows(5);
      newTextArea.setFont(new java.awt.Font("Segoe UI", 0, 10));
      newTextArea.setEditable(false);
      newScrollPane.getHorizontalScrollBar().setEnabled(true);
      newScrollPane.setViewportView(newTextArea);
      newPanel.setLayout(new GridLayout());
      newPanel.add(newScrollPane);
      v_logHolderTabbedPane.addTab(Integer.toString(roundNumber) + "-" + name, newPanel);
      currentLogTextArea = newTextArea;
    });
    // Now remove round logging that has 'expired'.
    // Note that this method will also trim all but the first and last log panels if logging is turned off
    // (We always keep first round's log panel, and we keep last because the user might turn logging back on in the
    // middle of the round)
    trimLogRoundPanels();
  }

  private void trimLogRoundPanels() {
    // If we're logging and we have trimming enabled, or if we have logging turned off
    if ((ProLogSettings.loadSettings().EnableAILogging && ProLogSettings.loadSettings().LimitLogHistory)
        || !ProLogSettings.loadSettings().EnableAILogging) {
      final int maxHistoryRounds;
      if (ProLogSettings.loadSettings().EnableAILogging) {
        maxHistoryRounds = ProLogSettings.loadSettings().LimitLogHistoryTo;
      } else {
        maxHistoryRounds = 1; // If we're not logging, trim to 1
      }
      SwingAction.invokeAndWait(() -> {
        for (int i = 0; i < v_logHolderTabbedPane.getTabCount(); i++) {
          // Remember, we never remove last tab, in case user turns logging back on in the middle of a round
          if (i != 0 && i < v_logHolderTabbedPane.getTabCount() - maxHistoryRounds) {
            // Remove the tab and decrease i by one, so the next component will be checked
            v_logHolderTabbedPane.removeTabAt(i);
            i--;
          }
        }
      });
    }
  }

  private javax.swing.JTextArea v_aiOutputLogArea;
  private javax.swing.JCheckBox v_enableAILogging;
  private javax.swing.JCheckBox v_limitLogHistoryCB;
  private javax.swing.JSpinner v_limitLogHistoryToSpinner;
  private javax.swing.JComboBox<String> v_logDepth;
  private javax.swing.JTabbedPane v_logHolderTabbedPane;
  private javax.swing.JTabbedPane v_tabPaneMain;
}
