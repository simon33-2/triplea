package games.strategy.ui;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.ListenerList;

public class ScrollableTextField extends JPanel {
  private static final long serialVersionUID = 6940592988573672224L;


  private static boolean s_imagesLoaded;
  private static Icon s_up;
  private static Icon s_down;
  private static Icon s_max;
  private static Icon s_min;

  private final IntTextField m_text;
  private final JButton m_up;
  private final JButton m_down;
  private final JButton m_max;
  private final JButton m_min;
  private final ListenerList<ScrollableTextFieldListener> m_listeners = new ListenerList<>();

  /** Creates new ScrollableTextField. */
  public ScrollableTextField(final int minVal, final int maxVal) {
    super();
    loadImages();
    m_text = new IntTextField(minVal, maxVal);
    setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    add(m_text);
    Insets inset = new Insets(0, 0, 0, 0);
    if (SystemProperties.isMac()) {
      inset = new Insets(2, 0, 2, 0);
    }
    m_up = new JButton(s_up);
    final Action m_incrementAction = new AbstractAction("inc") {
      private static final long serialVersionUID = 2125871167112459475L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_text.isEnabled()) {
          m_text.setValue(m_text.getValue() + 1);
          setWidgetActivation();
        }
      }
    };
    m_up.addActionListener(m_incrementAction);
    m_up.setMargin(inset);
    m_down = new JButton(s_down);
    m_down.setMargin(inset);
    final Action m_decrementAction = new AbstractAction("dec") {
      private static final long serialVersionUID = 787758939168986726L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_text.isEnabled()) {
          m_text.setValue(m_text.getValue() - 1);
          setWidgetActivation();
        }
      }
    };
    m_down.addActionListener(m_decrementAction);
    m_max = new JButton(s_max);
    m_max.setMargin(inset);
    final Action m_maxAction = new AbstractAction("max") {
      private static final long serialVersionUID = -3899827439573519512L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_text.isEnabled()) {
          m_text.setValue(m_text.getMax());
          setWidgetActivation();
        }
      }
    };
    m_max.addActionListener(m_maxAction);
    m_min = new JButton(s_min);
    m_min.setMargin(inset);
    final Action m_minAction = new AbstractAction("min") {
      private static final long serialVersionUID = 5785321239855254848L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_text.isEnabled()) {
          m_text.setValue(m_text.getMin());
          setWidgetActivation();
        }
      }
    };
    m_min.addActionListener(m_minAction);
    final JPanel upDown = new JPanel();
    upDown.setLayout(new BoxLayout(upDown, BoxLayout.Y_AXIS));
    upDown.add(m_up);
    upDown.add(m_down);
    final JPanel maxMin = new JPanel();
    maxMin.setLayout(new BoxLayout(maxMin, BoxLayout.Y_AXIS));
    maxMin.add(m_max);
    maxMin.add(m_min);
    add(upDown);
    add(maxMin);
    final IntTextFieldChangeListener m_textListener = field -> notifyListeners();
    m_text.addChangeListener(m_textListener);
    setWidgetActivation();
  }

  private static synchronized void loadImages() {
    if (s_imagesLoaded) {
      return;
    }
    s_up = new ImageIcon(ScrollableTextField.class.getResource("images/up.gif"));
    s_down = new ImageIcon(ScrollableTextField.class.getResource("images/down.gif"));
    s_max = new ImageIcon(ScrollableTextField.class.getResource("images/max.gif"));
    s_min = new ImageIcon(ScrollableTextField.class.getResource("images/min.gif"));
    s_imagesLoaded = true;
  }


  public void setMax(final int max) {
    m_text.setMax(max);
    setWidgetActivation();
  }

  public void setTerr(final String terr) {
    m_text.setTerr(terr);
  }

  public void setShowMaxAndMin(final boolean aBool) {
    m_max.setVisible(aBool);
    m_min.setVisible(aBool);
  }

  public int getMax() {
    return m_text.getMax();
  }

  public String getTerr() {
    return m_text.getTerr();
  }

  public void setMin(final int min) {
    m_text.setMin(min);
    setWidgetActivation();
  }

  private void setWidgetActivation() {
    if (m_text.isEnabled()) {
      final int value = m_text.getValue();
      final int max = m_text.getMax();
      final boolean enableUp = (value != max);
      m_up.setEnabled(enableUp);
      m_max.setEnabled(enableUp);
      final int min = m_text.getMin();
      final boolean enableDown = (value != min);
      m_down.setEnabled(enableDown);
      m_min.setEnabled(enableDown);
    } else {
      m_up.setEnabled(false);
      m_down.setEnabled(false);
      m_max.setEnabled(false);
      m_min.setEnabled(false);
    }
  }

  public int getValue() {
    return m_text.getValue();
  }

  public void setValue(final int value) {
    m_text.setValue(value);
    setWidgetActivation();
  }

  public void addChangeListener(final ScrollableTextFieldListener listener) {
    m_listeners.add(listener);
  }

  public void removeChangeListener(final ScrollableTextFieldListener listener) {
    m_listeners.remove(listener);
  }

  private void notifyListeners() {
    for (final ScrollableTextFieldListener listener : m_listeners) {
      listener.changedValue(this);
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    m_text.setEnabled(enabled);
    setWidgetActivation();
  }
}
