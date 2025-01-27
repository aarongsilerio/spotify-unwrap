import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
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
        app.post("/analyze/top-songs/year/{year}", this::getTopSongsByYear);
        app.post("/analyze/top-songs/year/{year}/month/{month}", this::getTopSongsByYearAndMonth);
        app.post("/analyze/played-songs/date/{date}", this::getPlayedSongs);
    }

    private void getTopSongs(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_SONGS, null, null, null);
    }
    private void getTopSongsByYear(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            handleAnalysisRequest(ctx, AnalysisType.TOP_SONGS_BY_YEAR, year, null, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }

    private void getTopSongsByYearAndMonth(Context ctx) {
        try {
            Integer year = Integer.parseInt(ctx.pathParam("year"));
            Integer month = Integer.parseInt(ctx.pathParam("month"));
            handleAnalysisRequest(ctx, AnalysisType.TOP_SONGS_BY_YEAR_MONTH, year, month, null);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year or month format");
        }
    }

    private void getTopArtists(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_ARTISTS, null, null, null);
    }

    private void getTopAlbums(Context ctx) {
        handleAnalysisRequest(ctx, AnalysisType.TOP_ALBUMS, null, null, null);
    }

    private void getPlayedSongs(Context ctx) {
        try {
            String date =  ctx.pathParam("date");
            handleAnalysisRequest(ctx, AnalysisType.PLAYED_SONGS_DATE, null, null, date);
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid year format");
        }
    }
    private void handleAnalysisRequest(Context ctx, AnalysisType type, Integer year, Integer month, String date) {
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
                    List<String> topTrackUris = spotifyDataService.getTopTracks(entries, 15);
                    ctx.json(topTrackUris);
                    return;
                case TOP_SONGS_BY_YEAR:
                    if (year == null) {
                        ctx.status(400).result("Year parameter is required");
                        return;
                    }
                    result = spotifyDataService.getTopTracks(entries, year, 15);
                    break;
                case TOP_SONGS_BY_YEAR_MONTH:
                    if (year == null || month == null) {
                        ctx.status(400).result("Year and month parameters are required");
                        return;
                    }
                    result = spotifyDataService.getTopTracks(entries, year, month, 15);
                    break;
                case TOP_ARTISTS:
                    result = spotifyDataService.getTopArtists(entries, 15);
                    break;
                case TOP_ALBUMS:
                    result = spotifyDataService.getTopAlbums(entries, 15);
                    break;
                case PLAYED_SONGS_DATE:
                    result = spotifyDataService.getPlayedSongsByDate(entries, date, 15);
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
        TOP_SONGS_BY_YEAR,
        TOP_SONGS_BY_YEAR_MONTH,
        TOP_ARTISTS,
        TOP_ALBUMS,
        PLAYED_SONGS_DATE
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