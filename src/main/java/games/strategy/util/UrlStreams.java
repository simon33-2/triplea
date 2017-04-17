package games.strategy.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Throwables;

/**
 * Utility class for opening input streams from URL and URI objects.
 */
public final class UrlStreams {

  /**
   * Opens an input stream to a given url. Returns Optional.empty() in case there is a failure.
   * The failure message is logged to the user.
   *
   * @return Optional.empty() if there was a failure opening the strema, otherwise an optional
   *         containing an input stream to the parameter uri.
   */
  public static Optional<InputStream> openStream(final URL url) {
    return new UrlStreams().newStream(url);
  }


  /**
   * Opens an input stream to a given uri.
   *
   * @return Optional.empty() if there was a failure opening the strema, otherwise an optional
   *         containing an input stream to the parameter uri.
   *
   * @throws IllegalStateException if the given uri is malformed
   */
  public static Optional<InputStream> openStream(final URI uri) {
    try {
      return UrlStreams.openStream(uri.toURL());
    } catch (final MalformedURLException e) {
      throw new IllegalStateException("Bad uri specified: " + uri, e);
    }
  }

  /** Used to obtain a connection from a given URL. */
  private final Function<URL, URLConnection> urlConnectionFactory;


  protected UrlStreams() {
    // By default just try open a connection, raise any exceptions encountered
    this.urlConnectionFactory = (url) -> {
      try {
        return url.openConnection();
      } catch (final IOException e) {
        throw Throwables.propagate(e);
      }
    };
  }

  /**
   * For test, a constructor that allows mock object injection.
   */
  protected UrlStreams(final Function<URL, URLConnection> connectionFactory) {
    this.urlConnectionFactory = connectionFactory;
  }

  protected Optional<InputStream> newStream(final URL url) {
    try {
      final URLConnection connection = urlConnectionFactory.apply(url);

      // Turn off URL connection caching to avoid open file leaks. When caching is on, the InputStream
      // returned is left open, even after you call 'InputStream.close()'
      connection.setDefaultUseCaches(false); // TODO: verify - setDefaultUseCaches(false) may not be necessary
      connection.setUseCaches(false);
      return Optional.of(connection.getInputStream());
    } catch (final IOException e) {
      return Optional.empty();
    }
  }
}
