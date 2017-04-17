package games.strategy.ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import games.strategy.util.ListenerList;

/**
 * Text field for entering int values.
 * Ensures valid integers are entered, and can limit the range of
 * values user can enter.
 */
public class IntTextField extends JTextField {
  private static final long serialVersionUID = -7993942326354823887L;
  private int m_max = Integer.MAX_VALUE;
  private int m_min = Integer.MIN_VALUE;
  private String m_terr = null;
  private final ListenerList<IntTextFieldChangeListener> m_listeners = new ListenerList<>();

  /** Creates new IntTextField. */
  public IntTextField() {
    super(3);
    initTextField();
  }

  public IntTextField(final int min) {
    this();
    setMin(min);
  }

  public IntTextField(final int min, final int max) {
    this();
    setMin(min);
    setMax(max);
  }

  public IntTextField(final int min, final int max, final int current) {
    this();
    setMin(min);
    setMax(max);
    setValue(current);
  }

  public IntTextField(final int min, final int max, final int current, final int columns) {
    super(columns);
    initTextField();
    setMin(min);
    setMax(max);
    setValue(current);
  }

  private void initTextField() {
    setDocument(new IntegerDocument());
    setText(String.valueOf(m_min));
    addFocusListener(new LostFocus());
  }

  public int getValue() {
    return Integer.parseInt(getText());
  }

  private void checkValue() {
    if (getText().trim().equals("-")) {
      setText(String.valueOf(m_min));
    }
    try {
      Integer.parseInt(getText());
    } catch (final NumberFormatException e) {
      setText(String.valueOf(m_min));
    }
    if (getValue() > m_max) {
      setText(String.valueOf(m_max));
    }
    if (getValue() < m_min) {
      setText(String.valueOf(m_min));
    }
  }

  public void setValue(final int value) {
    if (isGood(value)) {
      setText(String.valueOf(value));
    }
  }

  public void setMax(final int max) {
    if (max < m_min) {
      throw new IllegalArgumentException(
          "Max cant be less than min. Current Min: " + m_min + ", Current Max: " + m_max + ", New Max: " + max);
    }
    m_max = max;
    if (getValue() > m_max) {
      setText(String.valueOf(max));
    }
  }

  public void setTerr(final String terr) {
    m_terr = terr;
  }

  public void setMin(final int min) {
    if (min > m_max) {
      throw new IllegalArgumentException(
          "Min cant be greater than max. Current Max: " + m_max + ", Current Min: " + m_min + ", New Min: " + min);
    }
    m_min = min;
    if (getValue() < m_min) {
      setText(String.valueOf(min));
    }
  }

  public int getMax() {
    return m_max;
  }

  public String getTerr() {
    return m_terr;
  }

  public int getMin() {
    return m_min;
  }

  private boolean isGood(final int value) {
    return value <= m_max && value >= m_min;
  }

  /**
   * Make sure that no non numeric data is typed.
   */
  private class IntegerDocument extends PlainDocument {
    private static final long serialVersionUID = 135871239193051281L;

    @Override
    public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
      final String currentText = this.getText(0, getLength());
      final String beforeOffset = currentText.substring(0, offs);
      final String afterOffset = currentText.substring(offs, currentText.length());
      final String proposedResult = beforeOffset + str + afterOffset;
      // allow start of negative
      try {
        Integer.parseInt(proposedResult);
        super.insertString(offs, str, a);
        checkValue();
        notifyListeners();
      } catch (final NumberFormatException e) {
        // if an error dont insert
        // allow start of negative numbers
        if (offs == 0) {
          if (m_min < 0) {
            if (str.equals("-")) {
              super.insertString(offs, str, a);
            }
          }
        }
      }
    }

    @Override
    public void remove(final int offs, final int len) throws BadLocationException {
      super.remove(offs, len);
      // if its a valid number weve changed
      try {
        Integer.parseInt(IntTextField.this.getText());
        notifyListeners();
      } catch (final NumberFormatException e) {
      }
    }
  }

  public void addChangeListener(final IntTextFieldChangeListener listener) {
    m_listeners.add(listener);
  }

  public void removeChangeListener(final IntTextFieldChangeListener listener) {
    m_listeners.remove(listener);
  }

  private void notifyListeners() {
    for (final IntTextFieldChangeListener listener : m_listeners) {
      listener.changedValue(this);
    }
  }

  private class LostFocus extends FocusAdapter {
    @Override
    public void focusLost(final FocusEvent e) {
      // make sure the value is valid
      checkValue();
    }
  }
}
