package games.strategy.engine.framework.map.download;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class DownloadUtilsGetDownloadLengthTest {
  @Mock
  private CloseableHttpClient client;

  @Mock
  private HttpEntity entity;

  @Mock
  private CloseableHttpResponse response;

  @Mock
  private StatusLine statusLine;

  /**
   * Sets up the test fixture.
   */
  @Before
  public void setUp() throws Exception {
    when(client.execute(any())).thenReturn(response);
    when(response.getEntity()).thenReturn(entity);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
  }

  @Test
  public void getLengthOfResourceAt_ShouldReturnLengthWhenLengthIsPositive() throws Exception {
    when(entity.getContentLength()).thenReturn(42L);

    final Optional<Long> length = getLengthOfResourceAt();

    assertThat(length, is(Optional.of(42L)));
  }

  private Optional<Long> getLengthOfResourceAt() throws Exception {
    return DownloadUtils.getLengthOfResourceAt("some://uri", client);
  }

  @Test
  public void getLengthOfResourceAt_ShouldReturnLengthWhenLengthIsZero() throws Exception {
    when(entity.getContentLength()).thenReturn(0L);

    final Optional<Long> length = getLengthOfResourceAt();

    assertThat(length, is(Optional.of(0L)));
  }

  @Test
  public void getLengthOfResourceAt_ShouldThrowExceptionWhenStatusCodeIsNotOk() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

    catchException(() -> getLengthOfResourceAt());

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("status code"))));
  }

  @Test
  public void getLengthOfResourceAt_ShouldThrowExceptionWhenEntityIsAbsent() {
    when(response.getEntity()).thenReturn(null);

    catchException(() -> getLengthOfResourceAt());

    assertThat(caughtException(), allOf(
        is(instanceOf(IOException.class)),
        hasMessageThat(containsString("entity"))));
  }

  @Test
  public void getLengthOfResourceAt_ShouldReturnEmptyWhenLengthIsUnknown() throws Exception {
    when(entity.getContentLength()).thenReturn(-1L);

    final Optional<Long> length = getLengthOfResourceAt();

    assertThat(length, is(Optional.empty()));
  }
}