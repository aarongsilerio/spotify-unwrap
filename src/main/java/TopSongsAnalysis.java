import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TopSongsAnalysis implements Analysis {
    private final Integer year;
    private final Integer month;

    public TopSongsAnalysis() {
        this.year = null;
        this.month = null;
    }

    public TopSongsAnalysis(Integer year) {
        this.year = year;
        this.month = null;
    }

    public TopSongsAnalysis(Integer year, Integer month) {
        this.year = year;
        this.month = month;
    }

    @Override
    public List<String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api, int limit) {
        Stream<StreamingHistoryEntry> filteredEntries = entries.stream();

        if (year != null) {
            filteredEntries = filteredEntries.filter(entry -> entry.ts().getYear() == year);
        }

        if (month != null) {
            filteredEntries = filteredEntries.filter(entry -> entry.ts().getMonthValue() == month);
        }

        return filteredEntries
                .collect(Collectors.groupingBy(
                        StreamingHistoryEntry::spotifyTrackUri,
                        Collectors.summingLong(StreamingHistoryEntry::minutesPlayed)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
