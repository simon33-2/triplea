package games.strategy.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

public final class Util {
  public static final String TERRITORY_SEA_ZONE_INFIX = "Sea Zone";

  // all we have is static methods
  private Util() {}

  public interface Task<T> {
    T run();
  }

  public static <T> T runInSwingEventThread(final Task<T> task) {
    if (SwingUtilities.isEventDispatchThread()) {
      return task.run();
    }
    final AtomicReference<T> results = new AtomicReference<>();
    SwingAction.invokeAndWait(() -> results.set(task.run()));
    return results.get();
  }

  private static final Component c = new Component() {
    private static final long serialVersionUID = 1800075529163275600L;
  };

  public static void ensureImageLoaded(final Image anImage) {
    final MediaTracker tracker = new MediaTracker(c);
    tracker.addImage(anImage, 1);
    try {
      tracker.waitForAll();
      tracker.removeImage(anImage);
    } catch (final InterruptedException ignored) {
      // ignore interrupted
    }
  }

  public static Image copyImage(final BufferedImage img) {
    final BufferedImage copy = createImage(img.getWidth(), img.getHeight(), false);
    final Graphics2D g = (Graphics2D) copy.getGraphics();
    g.drawImage(img, 0, 0, null);
    g.dispose();
    return copy;
  }

  public static void notifyError(final Component parent, final String message) {
    EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), message, "Error",
        JOptionPane.ERROR_MESSAGE, new CountDownLatchHandler(true));
  }

  /**
   * Previously used to use TYPE_INT_BGR and TYPE_INT_ABGR but caused memory
   * problems. Fix is to use 3Byte rather than INT.
   */
  public static BufferedImage createImage(final int width, final int height, final boolean needAlpha) {
    if (needAlpha) {
      return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    } else {
      return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
  }

  public static Dimension getDimension(final Image anImage, final ImageObserver obs) {
    return new Dimension(anImage.getWidth(obs), anImage.getHeight(obs));
  }

  public static void center(final Window w) {
    final int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    final int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
    final int windowWidth = w.getWidth();
    final int windowHeight = w.getHeight();
    if (windowHeight > screenHeight) {
      return;
    }
    if (windowWidth > screenWidth) {
      return;
    }
    final int x = (screenWidth - windowWidth) / 2;
    final int y = (screenHeight - windowHeight) / 2;
    w.setLocation(x, y);
  }

  // code stolen from swingx
  // swingx is lgpl, so no problems with copyright
  public static Image getBanner(final String text) {
    final int w = 400;
    final int h = 60;
    final float loginStringX = w * .05f;
    final float loginStringY = h * .75f;
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = img.createGraphics();
    final Font font = new Font("Arial Bold", Font.PLAIN, 36);
    g2.setFont(font);
    final Graphics2D originalGraphics = g2;
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    // draw a big square
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, w, h);
    // create the curve shape
    final GeneralPath curveShape = new GeneralPath(GeneralPath.WIND_NON_ZERO);
    curveShape.moveTo(0, h * .6f);
    curveShape.curveTo(w * .167f, h * 1.2f, w * .667f, h * -.5f, w, h * .75f);
    curveShape.lineTo(w, h);
    curveShape.lineTo(0, h);
    curveShape.lineTo(0, h * .8f);
    curveShape.closePath();
    // draw into the buffer a gradient (bottom to top), and the text "Login"
    final GradientPaint gp = new GradientPaint(0, h, Color.GRAY, 0, 0, Color.LIGHT_GRAY);
    g2.setPaint(gp);
    g2.fill(curveShape);
    // g2.setPaint(Color.white);
    originalGraphics.setColor(Color.WHITE);
    originalGraphics.drawString(text, loginStringX, loginStringY);
    return img;
  }

  /**
   * java.lang.String findTerritoryName(java.awt.Point)
   * Finds a land territory name or some sea zone name where the point is contained in according to the territory name
   * -> polygons map.
   *
   * @param java.awt.point p - a point on the map
   * @param terrPolygons a map territory name -> polygons
   * @return Optional&lt;String>
   */
  public static Optional<String> findTerritoryName(final Point p, final Map<String, List<Polygon>> terrPolygons) {
    return Optional.ofNullable(findTerritoryName(p, terrPolygons, null));
  }

  /**
   * java.lang.String findTerritoryName(java.awt.Point)
   * Finds a land territory name or some sea zone name where the point is contained in according to the territory name
   * -> polygons map. If no land or sea territory has been found a default name is returned.
   *
   * @param java.awt.point p - a point on the map
   * @param terrPolygons a map territory name -> polygons
   * @param String defaultTerrName - default territory name that gets returns if nothing was found
   * @return found territory name of defaultTerrName
   */
  public static String findTerritoryName(final Point p, final Map<String, List<Polygon>> terrPolygons,
      final String defaultTerrName) {
    String lastWaterTerrName = defaultTerrName;
    // try to find a land territory.
    // sea zones often surround a land territory
    for (final String terrName : terrPolygons.keySet()) {
      final Collection<Polygon> polygons = terrPolygons.get(terrName);
      for (final Polygon poly : polygons) {
        if (poly.contains(p)) {
          if (Util.isTerritoryNameIndicatingWater(terrName)) {
            lastWaterTerrName = terrName;
          } else {
            return terrName;
          }
        } // if p is contained
      } // polygons collection loop
    } // terrPolygons map loop
    return lastWaterTerrName;
  }

  /**
   * Checks whether name indicates water or not (meaning name starts or ends with default text).
   *
   * @param territoryName - territory name
   * @return true if yes, false otherwise
   */
  public static boolean isTerritoryNameIndicatingWater(final String territoryName) {
    return territoryName.endsWith(TERRITORY_SEA_ZONE_INFIX) || territoryName.startsWith(TERRITORY_SEA_ZONE_INFIX);
  }
}
