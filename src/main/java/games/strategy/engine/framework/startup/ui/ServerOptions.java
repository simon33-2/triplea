package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;

/**
 * UI for choosing server options.
 */
public class ServerOptions extends JDialog {
  private static final long serialVersionUID = -9074816386666798281L;
  private JTextField m_nameField;
  private IntTextField m_portField;
  private JPasswordField m_passwordField;
  private boolean m_okPressed;
  private JCheckBox m_requirePasswordCheckBox;
  private JTextField m_comment;
  private boolean m_showComment = false;

  /**
   * Creates a new instance of ServerOptions.
   */
  public ServerOptions(final Component owner, final String defaultName, final int defaultPort,
      final boolean showComment) {
    super(owner == null ? null : JOptionPane.getFrameForComponent(owner), "Server options", true);
    m_showComment = showComment;
    initComponents();
    layoutComponents();
    setupActions();
    m_nameField.setText(defaultName);
    m_portField.setValue(defaultPort);
    setWidgetActivation();
    pack();
  }

  public void setNameEditable(final boolean editable) {
    m_nameField.setEditable(editable);
  }

  private void setupActions() {
    m_requirePasswordCheckBox.addActionListener(e -> setWidgetActivation());
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

  public String getPassword() {
    if (!m_requirePasswordCheckBox.isSelected()) {
      return null;
    }
    final String password = new String(m_passwordField.getPassword());
    if (password.trim().length() == 0) {
      return null;
    }
    return password;
  }

  public int getPort() {
    return m_portField.getValue();
  }

  private void initComponents() {
    m_nameField = new JTextField(10);
    m_portField = new IntTextField(0, Integer.MAX_VALUE);
    m_portField.setColumns(7);
    m_passwordField = new JPasswordField();
    m_passwordField.setColumns(10);
    m_comment = new JTextField();
    m_comment.setColumns(20);
  }

  private void layoutComponents() {
    final Container content = getContentPane();
    content.setLayout(new BorderLayout());
    final JPanel title = new JPanel();
    title.add(new JLabel("Select server options"));
    content.add(title, BorderLayout.NORTH);
    final Insets labelSpacing = new Insets(3, 7, 0, 0);
    final Insets fieldSpacing = new Insets(3, 5, 0, 7);
    final GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.anchor = GridBagConstraints.WEST;
    labelConstraints.gridx = 0;
    labelConstraints.insets = labelSpacing;
    final GridBagConstraints fieldConstraints = new GridBagConstraints();
    fieldConstraints.anchor = GridBagConstraints.WEST;
    fieldConstraints.gridx = 1;
    fieldConstraints.insets = fieldSpacing;
    m_requirePasswordCheckBox = new JCheckBox("");
    final JLabel passwordRequiredLabel = new JLabel("Require Password:");
    final JPanel fields = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    fields.setLayout(layout);
    final JLabel nameLabel = new JLabel("Name:");
    final JLabel portLabel = new JLabel("Port:");
    final JLabel passwordLabel = new JLabel("Password:");
    final JLabel commentLabel = new JLabel("Comments:");
    layout.setConstraints(portLabel, labelConstraints);
    layout.setConstraints(nameLabel, labelConstraints);
    layout.setConstraints(passwordLabel, labelConstraints);
    layout.setConstraints(m_portField, fieldConstraints);
    layout.setConstraints(m_nameField, fieldConstraints);
    layout.setConstraints(m_passwordField, fieldConstraints);
    layout.setConstraints(m_requirePasswordCheckBox, fieldConstraints);
    layout.setConstraints(passwordRequiredLabel, labelConstraints);
    fields.add(nameLabel);
    fields.add(m_nameField);
    fields.add(portLabel);
    fields.add(m_portField);
    fields.add(passwordRequiredLabel);
    fields.add(m_requirePasswordCheckBox);
    fields.add(passwordLabel);
    fields.add(m_passwordField);
    if (m_showComment) {
      layout.setConstraints(commentLabel, labelConstraints);
      layout.setConstraints(m_comment, fieldConstraints);
      fields.add(commentLabel);
      fields.add(m_comment);
    }
    content.add(fields, BorderLayout.CENTER);
    final JPanel buttons = new JPanel();
    buttons.add(new JButton(m_okAction));
    buttons.add(new JButton(m_cancelAction));
    content.add(buttons, BorderLayout.SOUTH);
  }

  public boolean getOKPressed() {
    return m_okPressed;
  }

  private void setWidgetActivation() {
    m_passwordField.setEnabled(m_requirePasswordCheckBox.isSelected());
    final Color backGround = m_passwordField.isEnabled() ? m_portField.getBackground() : getBackground();
    m_passwordField.setBackground(backGround);
    if (ClientFileSystemHelper.areWeOldExtraJar()
        && System.getProperty(GameRunner.TRIPLEA_SERVER_PROPERTY, "false").equalsIgnoreCase("true")) {
      setNameEditable(false);
    }
  }

  private final Action m_okAction = SwingAction.of("OK", e -> {
    setVisible(false);
    m_okPressed = true;
  });

  private final Action m_cancelAction = SwingAction.of("Cancel", e -> setVisible(false));

  public String getComments() {
    return m_comment.getText();
  }
}
