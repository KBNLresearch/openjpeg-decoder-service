package nl.kb.decoderservice.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import nl.kb.decoderservice.CacheConfig;
import nl.kb.decoderservice.api.Region;
import nl.kb.decoderservice.api.ScaleDims;
import nl.kb.decoderservice.core.ImageDecoder;
import nl.kb.decoderservice.core.ImageFetcher;
import nl.kb.jp2.Jp2Header;
import nl.kb.utils.NativeUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImagingServiceResourceTest {


    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final ImageFetcher imageFetcher = mock(ImageFetcher.class);
    private static final CacheConfig cacheConfig = new CacheConfig(0, 0);
    private static final ImageDecoder imageDecoder = new ImageDecoder(executorService, 2, cacheConfig);


    @ClassRule
    public static final ResourceTestRule resourceUnderTest = ResourceTestRule.builder()
            .addResource(new ImagingServiceResource(imageFetcher, imageDecoder, cacheConfig))
            .build();


    @Before
    public void setUp() throws IOException {
        when(imageFetcher.fetch("test-jp2")).thenReturn(
                new File("src/test/resources/MMATR01_001881004_00030_access.jp2")
        );

        when(imageFetcher.fetch("test-jpf")).thenReturn(
                new File("src/test/resources/dts_10702_0021_access.jpf")
        );
    }

    @Test
    public void itReturnsTheImageHeaderAsPlainText() {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("c", "imghead")
                .queryParam("f", "plain")
                .request().get();

        assertThat(response.readEntity(String.class), equalTo(
                "width: 1338\n" +
                        "height: 1778\n" +
                        "levels: 5\n" +
                        "tilex: 0\n" +
                        "tiley: 0\n" +
                        "tilewidth: 1024\n" +
                        "tileheight: 1024\n" +
                        "colorspace: RGB\n"
        ));
    }


    @Test
    public void itShouldDecodeAfullImage() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(1778));
        assertThat(bufferedImage.getWidth(), equalTo(1338));
    }


    @Test
    public void itShouldScaleUpAfullImage() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("s", "1.1")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(1956));
        assertThat(bufferedImage.getWidth(), equalTo(1472));
    }


    @Test
    public void itShouldScaleDownAfullImage() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("s", "0.125")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }


    @Test
    public void itShouldScaleTheImageBasedOnHeight() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("h", "222")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldScaleTheImageBasedOnWidth() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "167")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldScaleTheImageBasedOnSmallestOfWidthAndHeight() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "167")
                .queryParam("h", "1024")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldScaleTheImageBasedOnSmallestOfHeightAndWidth() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "1024")
                .queryParam("h", "222")

                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldRotateAfullImage() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("r", "90")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(1338));
        assertThat(bufferedImage.getWidth(), equalTo(1778));
    }

    @Test
    public void itShouldScaleTheImageBasedOnHeightWhenRotated90Degrees() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("h", "222")
                .queryParam("r", "90")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(295));
    }

    @Test
    public void itShouldScaleTheImageBasedOnWidthWhenRotated90Degrees() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "167")
                .queryParam("r", "90")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(126));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldScaleTheImageBasedOnSmallestOfWidthAndHeightWhenRotated90Degrees() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "167")
                .queryParam("h", "1024")
                .queryParam("r", "90")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(126));
        assertThat(bufferedImage.getWidth(), equalTo(167));
    }

    @Test
    public void itShouldScaleTheImageBasedOnSmallestOfHeightAndWidthWhenRotated90Degrees() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jp2")
                .queryParam("w", "1024")
                .queryParam("h", "222")
                .queryParam("r", "90")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(222));
        assertThat(bufferedImage.getWidth(), equalTo(295));
    }


    @Test
    public void itShouldDecodeAJpfImage() throws IOException {
        final Response response = resourceUnderTest.target("/imagingService")
                .queryParam("id", "test-jpf")
                .request().get();

        final BufferedImage bufferedImage = ImageIO.read(response.readEntity(InputStream.class));

        assertThat(bufferedImage.getHeight(), equalTo(1912));
        assertThat(bufferedImage.getWidth(), equalTo(1508));
    }

    @Test
    public void itShouldCropAndScaleTheImage() throws IOException, ExecutionException, InterruptedException {
        final ImageDecoder imageDecoderSpy = Mockito.spy(new ImageDecoder(executorService, 2, cacheConfig));
        final ImagingServiceResource localUnderTest = new ImagingServiceResource(imageFetcher, imageDecoderSpy, cacheConfig);

        localUnderTest.get(
                "test-jp2",
                50d,
                50d,
                512d,
                512d,
                0.5,
                null,
                null,
                null,
                null
        );


        final ArgumentCaptor<Jp2Header> jp2HeaderCaptor = ArgumentCaptor.forClass(Jp2Header.class);
        final ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
        final ArgumentCaptor<ScaleDims> scaleDimsCaptor = ArgumentCaptor.forClass(ScaleDims.class);
        final ArgumentCaptor<Integer> degCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> cp_reduceCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(imageDecoderSpy).getBufferedImageFromJp2File(
                jp2HeaderCaptor.capture(),
                regionCaptor.capture(),
                scaleDimsCaptor.capture(),
                degCaptor.capture(),
                cp_reduceCaptor.capture()
        );

        assertThat(regionCaptor.getValue().getX(), equalTo(100));
        assertThat(regionCaptor.getValue().getY(), equalTo(100));
        assertThat(regionCaptor.getValue().getW(), equalTo(1024));
        assertThat(regionCaptor.getValue().getH(), equalTo(1024));
        assertThat(scaleDimsCaptor.getValue().getW(), equalTo(512));
        assertThat(scaleDimsCaptor.getValue().getH(), equalTo(512));
        assertThat(cp_reduceCaptor.getValue(), equalTo(1));
    }

}
