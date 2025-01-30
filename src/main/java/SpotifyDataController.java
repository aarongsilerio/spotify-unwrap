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

    private final SpotifyDataService spotifyDataService;
    private final Javalin app;
    SpotifyAPIService api;
    public SpotifyDataController(SpotifyDataService spotifyDataService) {
        this.api = new SpotifyAPIService("07747c1af7e84fad9f7f388f0af8d068", "c614891da8834905b108304928a4525c");
        this.spotifyDataService = spotifyDataService;
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
        app.post("/analyze/top-artists", this::getTopArtists);
        app.post("/analyze/top-albums", this::getTopAlbums);
        app.post("/analyze/top-songs/year/{year}", this::getTopSongsByYear);
        app.post("/analyze/top-songs/year/{year}/month/{month}", this::getTopSongsByYearAndMonth);
        app.post("/analyze/played-songs/date/{date}", this::getPlayedSongs);
    }

    private void getTopSongs(Context ctx) {
        handleAnalysisRequest(ctx, new TopSongsAnalysis(), null, null, null);
    }
    private void getTopSongsByYear(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            handleAnalysisRequest(ctx, new TopSongsAnalysis(year), year, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
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

    private void getTopArtists(Context ctx) {
        handleAnalysisRequest(ctx, new TopArtistsAnalysis(), null, null, null);
    }

    private void getTopAlbums(Context ctx) {
        handleAnalysisRequest(ctx, new TopAlbumsAnalysis(), null, null, null);
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
            Object result = spotifyDataService.analyzeData(entries, analysis, api);

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
        SpotifyDataService service = new SpotifyDataService();
        new SpotifyDataController(service);
    }
}