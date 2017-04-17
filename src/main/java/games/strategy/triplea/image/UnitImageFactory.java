package games.strategy.triplea.image;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.ImageIcon;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.ui.Util;

public class UnitImageFactory {
  public static final int DEFAULT_UNIT_ICON_SIZE = 48;
  /**
   * Width of all icons.
   * You probably want getUnitImageWidth(), which takes scale factor into account.
   */
  private static int UNIT_ICON_WIDTH = DEFAULT_UNIT_ICON_SIZE;
  /**
   * Height of all icons.
   * You probably want getUnitImageHeight(), which takes scale factor into account.
   **/
  private static int UNIT_ICON_HEIGHT = DEFAULT_UNIT_ICON_SIZE;
  private static int UNIT_COUNTER_OFFSET_WIDTH = DEFAULT_UNIT_ICON_SIZE / 4;
  private static int UNIT_COUNTER_OFFSET_HEIGHT = UNIT_ICON_HEIGHT;
  private static final String FILE_NAME_BASE = "units/";
  // maps Point -> image
  private final Map<String, Image> m_images = new HashMap<>();
  // maps Point -> Icon
  private final Map<String, ImageIcon> m_icons = new HashMap<>();
  // Scaling factor for unit images
  private double m_scaleFactor;
  private ResourceLoader m_resourceLoader;

  /** Creates new UnitImageFactory. */
  public UnitImageFactory() {}

  public void setResourceLoader(final ResourceLoader loader, final double scaleFactor, final int initialUnitWidth,
      final int initialUnitHeight, final int initialUnitCounterOffsetWidth, final int initialUnitCounterOffsetHeight) {
    UNIT_ICON_WIDTH = initialUnitWidth;
    UNIT_ICON_HEIGHT = initialUnitHeight;
    UNIT_COUNTER_OFFSET_WIDTH = initialUnitCounterOffsetWidth;
    UNIT_COUNTER_OFFSET_HEIGHT = initialUnitCounterOffsetHeight;
    m_scaleFactor = scaleFactor;
    m_resourceLoader = loader;
    clearImageCache();
  }

  /**
   * Set the unitScaling factor.
   */
  public void setScaleFactor(final double scaleFactor) {
    if (m_scaleFactor != scaleFactor) {
      m_scaleFactor = scaleFactor;
      clearImageCache();
    }
  }

  /**
   * Return the unit scaling factor.
   */
  public double getScaleFactor() {
    return m_scaleFactor;
  }

  /**
   * Return the width of scaled units.
   */
  public int getUnitImageWidth() {
    return (int) (m_scaleFactor * UNIT_ICON_WIDTH);
  }

  /**
   * Return the height of scaled units.
   */
  public int getUnitImageHeight() {
    return (int) (m_scaleFactor * UNIT_ICON_HEIGHT);
  }

  public int getUnitCounterOffsetWidth() {
    return (int) (m_scaleFactor * UNIT_COUNTER_OFFSET_WIDTH);
  }

  public int getUnitCounterOffsetHeight() {
    return (int) (m_scaleFactor * UNIT_COUNTER_OFFSET_HEIGHT);
  }

  // Clear the image and icon cache
  private void clearImageCache() {
    m_images.clear();
    m_icons.clear();
  }

  /**
   * Return the appropriate unit image.
   */
  public Optional<Image> getImage(final UnitType type, final PlayerID player, final GameData data,
      final boolean damaged,
      final boolean disabled) {
    final String baseName = getBaseImageName(type, player, damaged, disabled);
    final String fullName = baseName + player.getName();
    if (m_images.containsKey(fullName)) {
      return Optional.of(m_images.get(fullName));
    }
    final Optional<Image> image = getBaseImage(baseName, player);
    if (!image.isPresent()) {
      return Optional.empty();
    }
    final Image baseImage = image.get();


    // We want to scale units according to the given scale factor.
    // We use smooth scaling since the images are cached to allow
    // to take our time in doing the scaling.
    // Image observer is null, since the image should have been
    // guaranteed to be loaded.
    final int width = (int) (baseImage.getWidth(null) * m_scaleFactor);
    final int height = (int) (baseImage.getHeight(null) * m_scaleFactor);
    final Image scaledImage = baseImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    // Ensure the scaling is completed.
    Util.ensureImageLoaded(scaledImage);
    m_images.put(fullName, scaledImage);
    return Optional.of(scaledImage);
  }

  public Optional<URL> getBaseImageURL(final String baseImageName, final PlayerID id) {
    return getBaseImageURL(baseImageName, id, m_resourceLoader);
  }

  private static Optional<URL> getBaseImageURL(final String baseImageName, final PlayerID id,
      final ResourceLoader resourceLoader) {
    // URL uses '/' not '\'
    final String fileName = FILE_NAME_BASE + id.getName() + "/" + baseImageName + ".png";
    final URL url = resourceLoader.getResource(fileName);
    return Optional.ofNullable(url);
  }

  private Optional<Image> getBaseImage(final String baseImageName, final PlayerID id) {
    final Optional<URL> imageLocation = getBaseImageURL(baseImageName, id);
    Image image = null;
    if (imageLocation.isPresent()) {
      image = Toolkit.getDefaultToolkit().getImage(getBaseImageURL(baseImageName, id).get());
      Util.ensureImageLoaded(image);
    }
    return Optional.ofNullable(image);
  }

  public Optional<Image> getHighlightImage(final UnitType type, final PlayerID player, final GameData data,
      final boolean damaged,
      final boolean disabled) {
    final Optional<Image> baseImage = getImage(type, player, data, damaged, disabled);
    if (!baseImage.isPresent()) {
      return Optional.empty();
    }

    final Image base = baseImage.get();
    final BufferedImage newImage = Util.createImage(base.getWidth(null), base.getHeight(null), true);
    // copy the real image
    final Graphics2D g = newImage.createGraphics();
    g.drawImage(base, 0, 0, null);
    // we want a highlight only over the area
    // that is not clear
    g.setComposite(AlphaComposite.SrcIn);
    g.setColor(new Color(240, 240, 240, 127));
    g.fillRect(0, 0, base.getWidth(null), base.getHeight(null));
    g.dispose();
    return Optional.of(newImage);
  }

  /**
   * Return a icon image for a unit.
   */
  public Optional<ImageIcon> getIcon(final UnitType type, final PlayerID player, final GameData data,
      final boolean damaged,
      final boolean disabled) {
    final String baseName = getBaseImageName(type, player, damaged, disabled);
    final String fullName = baseName + player.getName();
    if (m_icons.containsKey(fullName)) {
      return Optional.of(m_icons.get(fullName));
    }
    final Optional<Image> image = getBaseImage(baseName, player);
    if (!image.isPresent()) {
      return Optional.empty();
    }

    final ImageIcon icon = new ImageIcon(image.get());
    m_icons.put(fullName, icon);
    return Optional.of(icon);
  }

  private static String getBaseImageName(final UnitType type, final PlayerID id, final boolean damaged,
      final boolean disabled) {
    StringBuilder name = new StringBuilder(32);
    name.append(type.getName());
    if (!type.getName().endsWith("_hit") && !type.getName().endsWith("_disabled")) {
      if (type.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
        if (TechTracker.hasRocket(id) && UnitAttachment.get(type).getIsRocket()) {
          name = new StringBuilder("rockets");
        }
        if (TechTracker.hasAARadar(id) && Matches.UnitTypeIsAAforAnything.match(type)) {
          name.append("_r");
        }
      } else if (UnitAttachment.get(type).getIsRocket() && Matches.UnitTypeIsAAforAnything.match(type)) {
        if (TechTracker.hasRocket(id)) {
          name.append("_rockets");
        }
        if (TechTracker.hasAARadar(id)) {
          name.append("_r");
        }
      } else if (UnitAttachment.get(type).getIsRocket()) {
        if (TechTracker.hasRocket(id)) {
          name.append("_rockets");
        }
      } else if (Matches.UnitTypeIsAAforAnything.match(type)) {
        if (TechTracker.hasAARadar(id)) {
          name.append("_r");
        }
      }
      if (UnitAttachment.get(type).getIsAir() && !UnitAttachment.get(type).getIsStrategicBomber()) {
        if (TechTracker.hasLongRangeAir(id)) {
          name.append("_lr");
        }
        if (TechTracker.hasJetFighter(id)
            && (UnitAttachment.get(type).getAttack(id) > 0 || UnitAttachment.get(type).getDefense(id) > 0)) {
          name.append("_jp");
        }
      }
      if (UnitAttachment.get(type).getIsAir() && UnitAttachment.get(type).getIsStrategicBomber()) {
        if (TechTracker.hasLongRangeAir(id)) {
          name.append("_lr");
        }
        if (TechTracker.hasHeavyBomber(id)) {
          name.append("_hb");
        }
      }
      if (UnitAttachment.get(type).getIsSub()
          && (UnitAttachment.get(type).getAttack(id) > 0 || UnitAttachment.get(type).getDefense(id) > 0)) {
        if (TechTracker.hasSuperSubs(id)) {
          name.append("_ss");
        }
        if (TechTracker.hasRocket(id)) {
        }
      }
      if (type.getName().equals(Constants.UNIT_TYPE_FACTORY) || UnitAttachment.get(type).getCanProduceUnits()) {
        if (TechTracker.hasIndustrialTechnology(id) || TechTracker.hasIncreasedFactoryProduction(id)) {
          name.append("_it");
        }
      }
    }
    if (disabled) {
      name.append("_disabled");
    } else if (damaged) {
      name.append("_hit");
    }
    return name.toString();
  }
}
