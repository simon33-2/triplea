package games.strategy.triplea.printgenerator;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import games.strategy.engine.data.GameData;
import games.strategy.ui.SwingComponents;

public class SetupFrame extends JPanel {
  private static final long serialVersionUID = 7308943603423170303L;
  private final JTextField m_outField;
  private final JFileChooser m_outChooser;
  private final JRadioButton m_originalState;
  private final GameData m_data;
  private File m_outDir;

  /**
   * Creates a new SetupFrame.
   */
  public SetupFrame(final GameData data) {
    super(new BorderLayout());
    final JLabel m_info1 = new JLabel();
    final JLabel m_info2 = new JLabel();
    final JLabel m_info3 = new JLabel();
    m_data = data;
    final JButton m_outDirButton = new JButton();
    final JButton m_runButton = new JButton();
    m_outField = new JTextField(15);
    m_outChooser = new JFileChooser();
    m_outChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    final JRadioButton m_currentState = new JRadioButton();
    m_originalState = new JRadioButton();
    final ButtonGroup m_radioButtonGroup = new ButtonGroup();
    m_info1.setText("This utility will export the map's either current or ");
    m_info2.setText("beginning state exactly like the boardgame, so you ");
    m_info3.setText("will get Setup Charts, Unit Information, etc.");
    m_currentState.setText("Current Position/State");
    m_originalState.setText("Starting Position/State");
    m_radioButtonGroup.add(m_currentState);
    m_radioButtonGroup.add(m_originalState);
    m_originalState.setSelected(true);
    m_outDirButton.setText("Choose the Output Directory");
    m_outDirButton.addActionListener(e -> {
      final int returnVal = m_outChooser.showOpenDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File outDir = m_outChooser.getSelectedFile();
        m_outField.setText(outDir.getAbsolutePath());
      }
    });
    m_runButton.setText("Generate the Files");
    m_runButton.addActionListener(e -> {
      if (!m_outField.getText().equals("")) {
        m_outDir = new File(m_outField.getText());
        final PrintGenerationData printData = new PrintGenerationData();
        printData.setOutDir(m_outDir);
        printData.setData(m_data);
        new InitialSetup().run(printData, m_originalState.isSelected());
        JOptionPane.showMessageDialog(null, "Done!", "Done!", JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(null, "You need to select an Output Directory.", "Select an Output Directory!",
            JOptionPane.ERROR_MESSAGE);
      }
    });
    final JPanel m_infoPanel = SwingComponents.gridPanel(3, 1);
    final JPanel m_textButtonRadioPanel = new JPanel(new BorderLayout());
    m_infoPanel.add(m_info1);
    m_infoPanel.add(m_info2);
    m_infoPanel.add(m_info3);
    super.add(m_infoPanel, BorderLayout.NORTH);
    m_textButtonRadioPanel.add(m_outField, BorderLayout.WEST);
    m_textButtonRadioPanel.add(m_outDirButton, BorderLayout.EAST);
    final JPanel panel = SwingComponents.gridPanel(1, 2);
    panel.add(m_originalState);
    panel.add(m_currentState);
    m_textButtonRadioPanel.add(panel, BorderLayout.SOUTH);
    super.add(m_textButtonRadioPanel, BorderLayout.CENTER);
    super.add(m_runButton, BorderLayout.SOUTH);
  }
}
