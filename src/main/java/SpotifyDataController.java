import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.jetbrains.annotations.NotNull;

public class SpotifyDataController {
    private final Javalin app;
    SpotifyAPIService api;
    public SpotifyDataController() {
        this.api = new SpotifyAPIService("07747c1af7e84fad9f7f388f0af8d068", "c614891da8834905b108304928a4525c");
        this.app = Javalin.create(config -> {
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.directory = "/public";
                        staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
                    });
                    config.jsonMapper(createGsonMapper());
                    // Add CORS configuration:
                    config.bundledPlugins.enableCors(cors -> {
                        cors.addRule(CorsPluginConfig.CorsRule::anyHost);
                    });
                })

                .start(7070);
        setupEndpoints();
    }

    private void setupEndpoints() {
        app.post("/analyze/top-songs", this::getTopSongs);
        app.post("/analyze/top-songs/year/{year}", this::getTopSongsByYear);
        app.post("/analyze/top-songs/month/{month}", this::getTopSongsByMonth);
        app.post("/analyze/top-songs/year/{year}/month/{month}", this::getTopSongsByYearAndMonth);
        app.post("/analyze/top-artists", this::getTopArtists);
        app.post("/analyze/top-artists/year/{year}", this::getTopArtistsByYear);
        app.post("/analyze/top-artists/month/{month}", this::getTopArtistsByMonth);
        app.post("/analyze/top-artists/year/{year}/month/{month}", this::getTopArtistsByYearAndMonth);
        app.post("/analyze/top-albums", this::getTopAlbums);
        app.post("/analyze/top-albums/year/{year}", this::getTopAlbumsByYear);
        app.post("/analyze/top-albums/month/{month}", this::getTopAlbumsByMonth);
        app.post("/analyze/top-albums/year/{year}/month/{month}", this::getTopAlbumsByYearAndMonth);
        app.post("/analyze/played-songs/date/{date}", this::getPlayedSongs);
    }

    // Top Songs
    private void getTopSongs(Context ctx) {
        handleAnalysisRequest(ctx, new TopSongsAnalysis(), null, null, null);
    }
    private void getTopSongsByYear(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            handleAnalysisRequest(ctx, new TopSongsAnalysis(year, null), year, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }    private void getTopSongsByMonth(Context ctx) {
        try {
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopSongsAnalysis(null, month), month, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid month format");
        }
    }

    private void getTopSongsByYearAndMonth(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopSongsAnalysis(year, month), year, month, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year or month format");
        }
    }

    // Top Artists
    private void getTopArtists(Context ctx) {
        handleAnalysisRequest(ctx, new TopArtistsAnalysis(), null, null, null);
    }

    private void getTopArtistsByYear(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            handleAnalysisRequest(ctx, new TopArtistsAnalysis(year, null), year, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }    private void getTopArtistsByMonth(Context ctx) {
        try {
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopArtistsAnalysis(null, month), month, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid month format");
        }
    }

    private void getTopArtistsByYearAndMonth(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopArtistsAnalysis(year, month), year, month, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year or month format");
        }
    }

    // Top Albums
    private void getTopAlbums(Context ctx) {
        handleAnalysisRequest(ctx, new TopAlbumsAnalysis(), null, null, null);
    }

    private void getTopAlbumsByYear(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            handleAnalysisRequest(ctx, new TopAlbumsAnalysis(year, null), year, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }    private void getTopAlbumsByMonth(Context ctx) {
        try {
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopAlbumsAnalysis(null, month), month, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid month format");
        }
    }

    private void getTopAlbumsByYearAndMonth(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, new TopAlbumsAnalysis(year, month), year, month, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year or month format");
        }
    }

    private void getPlayedSongs(Context ctx) {
        try {
            String date =  ctx.pathParam("date");
            handleAnalysisRequest(ctx, new PlayedSongsByDayAnalysis(date), null, null, date);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }
    private void handleAnalysisRequest(Context ctx, Analysis analysis, Integer year, Integer month, String date) {
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.status(400).result("No file uploaded");
            return;
        }

        try {
            List<StreamingHistoryEntry> entries = SpotifyDataService.parseCsv(file.content());
            Object result = SpotifyDataService.analyzeData(entries, analysis, api);

            if (result != null) {
                ctx.json(result);
            } else {
                ctx.status(500).result("Error processing request");
            }

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Error processing file");
        }
    }

    private JsonMapper createGsonMapper() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();

        return new JsonMapper() {
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return gson.fromJson(json, targetType);
            }

            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return gson.toJson(obj, type);
            }
        };
    }

    public static void main(String[] args) {
        new SpotifyDataController();
    }
}