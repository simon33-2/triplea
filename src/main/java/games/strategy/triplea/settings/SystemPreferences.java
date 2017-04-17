package games.strategy.triplea.settings;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Class for accessing and storing user 'system' preferences.
 * Preferences are stored with the 'system', and should persist between restarts and even installations
 * of the application
 *
 * <p>
 * Note: Game engine properties are similar, but different in that they are stored in the game engines config file.
 * System properties will always have a default value hardcoded in code, while game engine properties will get their
 * value from config (and presumably have error handling if the config is mangled).
 * </p>
 */
public class SystemPreferences {

  /**
   * Puts a value into system preferences, and flushes when done. Note: The 'flush' operation is very slow
   *
   * <p>
   * Note: If there is a need to do many of these one after another,
   * then call 'putNoFlush' followed by a single'flush'.
   * </p>
   */
  public static void put(SystemPreferenceKey key, String value) {
    putNoFlush(key, value);
    flush();
  }

  /**
   * @see SystemPreferences#put(SystemPreferenceKey, String).
   */
  public static void put(SystemPreferenceKey key, boolean value) {
    put(key, String.valueOf(value));
  }

  /**
   * Puts a value into system preferences (note: not actually persisted until flush is called).
   */
  private static void putNoFlush(SystemPreferenceKey key, String value) {
    getPrefs().put(key.name(), value);
  }

  public static void put(SystemPreferenceKey tripleaServerObserverJoinWaitTime, int wait) {
    put(tripleaServerObserverJoinWaitTime, String.valueOf(wait));
  }

  private static Preferences getPrefs() {
    return Preferences.userNodeForPackage(SystemPreferences.class);
  }

  /**
   * Persists preferences, calls to 'get' return the last flushed value, not the last 'put' value.
   */
  public static void flush() {
    try {
      getPrefs().flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Failed to persist", e);
    }
  }

  /**
   * Looks up a preference value by key (note: returns the last flushed value).
   *
   * @param key The preference key to look up
   * @param defaultValue A default value to use when the look up finds nothing
   */
  public static String get(SystemPreferenceKey key, String defaultValue) {
    return getPrefs().get(key.name(), defaultValue);
  }


  /**
   * @see SystemPreferences#get(SystemPreferenceKey, String).
   */
  public static boolean get(SystemPreferenceKey key, boolean defaultValue) {
    return Boolean.parseBoolean(getPrefs().get(key.name(), String.valueOf(defaultValue)));
  }

  /**
   * @see SystemPreferences#get(SystemPreferenceKey, String).
   */
  public static int get(SystemPreferenceKey key, int defaultValue) {
    return getPrefs().getInt(key.name(), defaultValue);
  }
}
