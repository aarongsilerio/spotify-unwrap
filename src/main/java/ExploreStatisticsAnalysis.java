import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExploreStatisticsAnalysis implements Analysis {
    @Override
    public Map<String, String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api) {
        Map<String, String> statistics = new HashMap<>();

        // Minutes played
        int listeningTimeinDays = (int) entries.stream()
                .mapToLong(StreamingHistoryEntry::minutesPlayed) // Use the correct getter method
                .sum() / 60 / 24;

        // Play count
        int playCount = entries.size();

        // Unique songs count
        int uniqueSongsCount = (int) entries.stream()
                .map(StreamingHistoryEntry::spotifyTrackUri)
                .distinct()
                .count();

        // Album count
        long distinctAlbums = entries.stream()
                .map(StreamingHistoryEntry::albumName) // Use the correct getter method
                .distinct()
                .count();

        // Artist count
        long distinctArtist = entries.stream()
                .map(StreamingHistoryEntry::artistName) // Use the correct getter method
                .distinct()
                .count();

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

        statistics.put("listening", String.format("%,d", listeningTimeinDays));
        statistics.put("albums", String.format("%,d", distinctAlbums));
        statistics.put("artists", numberFormat.format(distinctArtist));
        statistics.put("plays", numberFormat.format(playCount));
        statistics.put("unique-songs", numberFormat.format(uniqueSongsCount));

        System.out.println(statistics);

        return statistics;
    }
}