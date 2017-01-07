package games.strategy.engine.framework.startup.login;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import games.strategy.engine.ClientContext;
import games.strategy.net.IConnectionLogin;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.MD5Crypt;

public class ClientLogin implements IConnectionLogin {
  public static final String ENGINE_VERSION_PROPERTY = "Engine.Version";
  public static final String JDK_VERSION_PROPERTY = "JDK.Version";
  public static final String PASSWORD_PROPERTY = "Password";
  private final Component m_parent;

  public ClientLogin(final Component parent) {
    m_parent = parent;
  }

  @Override
  public Map<String, String> getProperties(final Map<String, String> challengProperties) {
    final Map<String, String> rVal = new HashMap<>();
    if (challengProperties.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString())) {
      final JPasswordField passwordField = new JPasswordField();
      passwordField.setColumns(15);
      JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent), passwordField,
          "Enter a password to join the game", JOptionPane.QUESTION_MESSAGE);
      final String password = new String(passwordField.getPassword());
      rVal.put(PASSWORD_PROPERTY, MD5Crypt.crypt(password, challengProperties.get(ClientLoginValidator.SALT_PROPERTY)));
    }
    rVal.put(ENGINE_VERSION_PROPERTY, ClientContext.engineVersion().toString());
    rVal.put(JDK_VERSION_PROPERTY, System.getProperty("java.runtime.version"));
    return rVal;
  }

  @Override
  public void notifyFailedLogin(final String message) {
    EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent), message,
        new CountDownLatchHandler(true));
  }
}
