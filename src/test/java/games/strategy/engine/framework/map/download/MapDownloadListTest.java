package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.util.Version;


@RunWith(MockitoJUnitRunner.class)
public class MapDownloadListTest {


  private static final String MAP_NAME = "new_test_order";
  private static final Version MAP_VERSION = new Version(10, 10);
  private static final Version lowVersion = new Version(0, 0);

  private static final DownloadFileDescription TEST_MAP = new DownloadFileDescription("", "", MAP_NAME, MAP_VERSION,
      DownloadFileDescription.DownloadType.MAP, DownloadFileDescription.MapCategory.EXPERIMENTAL);

  @Mock
  private FileSystemAccessStrategy strategy;

  private final List<DownloadFileDescription> descriptions = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    descriptions.add(TEST_MAP);
  }

  @Test
  public void testAvailable() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.empty());
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(1));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(0));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  public void testInstalled() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(MAP_VERSION));
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  public void testOutOfDate() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(1));
  }
}
