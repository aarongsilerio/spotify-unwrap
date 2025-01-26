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
        app.post("/analyze", this::analyzeSpotifyData);
    }

    private void analyzeSpotifyData(Context ctx) {
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.status(400).result("No file uploaded");
            return;
        }

        try {
            List<StreamingHistoryEntry> entries = spotifyDataService.parseCsv(file.content());

            // Calculate features
            Map<String, Long> topTracks = spotifyDataService.getTopTracks(entries, 10);
            Map<String, Long> topArtists = spotifyDataService.getTopArtists(entries, 10);
            Map<String, Long> topAlbums = spotifyDataService.getTopAlbums(entries, 10);
            long totalListeningTime = spotifyDataService.getTotalListeningTime(entries);
            Map<DayOfWeek, Long> mostListenedToDays = spotifyDataService.getMostListenedToDays(entries);

            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("topTracks", topTracks);
            response.put("topArtists", topArtists);
            response.put("topAlbums", topAlbums);
            response.put("totalListeningTime", totalListeningTime);
            response.put("mostListenedToDays", mostListenedToDays);

            ctx.json(response);

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Error processing file");
        }
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