package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;

/**
 * UI for choosing client options.
 */
public class ClientOptions extends JDialog {
  private static final long serialVersionUID = 8036055679545539809L;
  private JTextField m_nameField;
  private JTextField m_addressField;
  private IntTextField m_portField;
  private boolean m_okPressed;

  /**
   * Creates a new instance of ClientOptions.
   */
  public ClientOptions(final Component parent, final String defaultName, final int defaultPort,
      final String defaultAddress) {
    super(JOptionPane.getFrameForComponent(parent), "Client options", true);
    initComponents();
    layoutComponents();
    m_nameField.setText(defaultName);
    m_portField.setValue(defaultPort);
    m_addressField.setText(defaultAddress);
    pack();
  }

  @Override
  public String getName() {
    // fixes crash by truncating names to 20 characters
    final String s = m_nameField.getText().trim();
    if (s.length() > 20) {
      return s.substring(0, 20);
    }
    return s;
  }

  public String getAddress() {
    return m_addressField.getText().trim();
  }

  public int getPort() {
    return m_portField.getValue();
  }

  private void initComponents() {
    m_nameField = new JTextField(10);
    m_addressField = new JTextField(10);
    m_portField = new IntTextField(0, Integer.MAX_VALUE);
    m_portField.setColumns(7);
  }

  private void layoutComponents() {
    final Container content = getContentPane();
    content.setLayout(new BorderLayout());
    final JPanel title = new JPanel();
    title.add(new JLabel("Select client options"));
    content.add(title, BorderLayout.NORTH);
    final Insets labelSpacing = new Insets(3, 7, 0, 0);
    final Insets fieldSpacing = new Insets(3, 5, 0, 7);
    final GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.anchor = GridBagConstraints.EAST;
    labelConstraints.gridx = 0;
    labelConstraints.insets = labelSpacing;
    final GridBagConstraints fieldConstraints = new GridBagConstraints();
    fieldConstraints.anchor = GridBagConstraints.WEST;
    fieldConstraints.gridx = 1;
    fieldConstraints.insets = fieldSpacing;
    final JPanel fields = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    fields.setLayout(layout);
    final JLabel nameLabel = new JLabel("Name:");
    final JLabel portLabel = new JLabel("Server Port:");
    final JLabel addressLabel = new JLabel("Server Address:");
    layout.setConstraints(portLabel, labelConstraints);
    layout.setConstraints(nameLabel, labelConstraints);
    layout.setConstraints(addressLabel, labelConstraints);
    layout.setConstraints(m_portField, fieldConstraints);
    layout.setConstraints(m_nameField, fieldConstraints);
    layout.setConstraints(m_addressField, fieldConstraints);
    fields.add(nameLabel);
    fields.add(m_nameField);
    fields.add(portLabel);
    fields.add(m_portField);
    fields.add(addressLabel);
    fields.add(m_addressField);
    content.add(fields, BorderLayout.CENTER);
    final JPanel buttons = new JPanel();
    buttons.add(new JButton(m_okAction));
    buttons.add(new JButton(m_cancelAction));
    content.add(buttons, BorderLayout.SOUTH);
  }

  public boolean getOKPressed() {
    return m_okPressed;
  }

  private final Action m_okAction = SwingAction.of("Connect", e -> {
    setVisible(false);
    m_okPressed = true;
  });
  private final Action m_cancelAction = SwingAction.of("Cancel", e -> setVisible(false));
}
