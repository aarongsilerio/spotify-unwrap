import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.json.JsonMapper;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

public class SpotifyDataController {

    private final SpotifyDataService spotifyDataService;
    private final Javalin app;

    public SpotifyDataController(SpotifyDataService spotifyDataService) {
        this.spotifyDataService = spotifyDataService;
        this.app = Javalin.create(config -> {
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.directory = "/public";
                        staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
                    });
                    config.jsonMapper(createGsonMapper());
                })
                .start(7070);
        setupEndpoints();
    }

    private void setupEndpoints() {
        app.post("/analyze/top-songs", this::getTopSongs);
        app.post("/analyze/top-artists", this::getTopArtists);
        app.post("/analyze/top-albums", this::getTopAlbums);
    }

    private void getTopSongs(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_SONGS);
    }

    private void getTopArtists(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_ARTISTS);
    }

    private void getTopAlbums(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_ALBUMS);
    }

    private void handleAnalysisRequest(Context ctx, AnalysisType type) {
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.status(400).result("No file uploaded");
            return;
        }

        try {
            List<StreamingHistoryEntry> entries = spotifyDataService.parseCsv(file.content());
            Object result = null;

            switch (type) {
                case TOP_SONGS:
                    List<String> topTrackUris = spotifyDataService.getTopTrackUris(entries, 10);
                    ctx.json(topTrackUris);
                    return; // Return directly for top songs
                case TOP_ARTISTS:
                    result = spotifyDataService.getTopArtists(entries, 10);
                    break;
                case TOP_ALBUMS:
                    result = spotifyDataService.getTopAlbums(entries, 10);
                    break;
            }

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
    private enum AnalysisType {
        TOP_SONGS,
        TOP_ARTISTS,
        TOP_ALBUMS
    }

    private JsonMapper createGsonMapper() {
        Gson gson = new GsonBuilder().create();
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