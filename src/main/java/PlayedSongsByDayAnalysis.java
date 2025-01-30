import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PlayedSongsByDayAnalysis implements Analysis {
    private final String date;

    public PlayedSongsByDayAnalysis(String date) {
        this.date = date;
    }

    @Override
    public List<String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api, int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, formatter);

        return entries.stream()
                .filter(entry -> entry.ts().toLocalDate().isEqual(localDate))
                .map(StreamingHistoryEntry::spotifyTrackUri)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
}