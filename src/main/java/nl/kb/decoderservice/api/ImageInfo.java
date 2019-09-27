package nl.kb.decoderservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import nl.kb.jp2.DimReducer;
import nl.kb.jp2.Jp2Header;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public class ImageInfo {

    @JsonProperty("protocol")
    private final String protocol = "http://iiif.io/api/image";
    @JsonProperty("@context")
    private final String context = "http://iiif.io/api/image/2/context.json";
    @JsonProperty("@id")
    private final String id;
    @JsonProperty
    private final Integer width;
    @JsonProperty
    private final Integer height;
    @JsonProperty
    private final List<Size> sizes;
    @JsonProperty
    private final List<Tile> tiles;

    @JsonProperty
    private final List<String> profile = Lists.newArrayList("http://iiif.io/api/image/2/level1.json");


    public ImageInfo(Jp2Header jp2Header, UriInfo uriInfo) {
        this.id = uriInfo.getRequestUri().toString().replaceAll("/info.json$", "");
        this.width = jp2Header.getX1();
        this.height = jp2Header.getY1();
        this.sizes = makeSizes(jp2Header);
        this.tiles = makeTiles(jp2Header);
    }

    private List<Tile> makeTiles(Jp2Header jp2Header) {
        return Lists.newArrayList(
                new Tile(jp2Header.getTdx(), jp2Header.getTdy(), jp2Header.getNumRes())
        );
    }

    private List<Size> makeSizes(Jp2Header jp2Header) {
        final List<Size> result = new ArrayList<>();
        for (int i = 0; i < jp2Header.getNumRes(); i++) {
            result.add(new Size(DimReducer
                    .reduce(jp2Header.getX1(), i), DimReducer.reduce(jp2Header.getY1(), i)));
        }

        return result;
    }

    public class Size {
        @JsonProperty
        Integer width;
        @JsonProperty
        Integer height;

        public Size(int w, int h) {
            this.width = w;
            this.height = h;
        }
    }

    public class Tile {
        @JsonProperty
        Integer width;
        @JsonProperty
        Integer height;
        @JsonProperty
        List<Integer> scaleFactors = new ArrayList<>();

        Tile(int tdx, int tdy, int numRes) {
            width = tdx;
            height = tdy;
            for (int i = 0, sf = 1; i < numRes; i++, sf *= 2) {
                scaleFactors.add(sf);
            }
        }
    }
}
