import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TopArtistsAnalysis implements Analysis {
    private final Integer year;
    private final Integer month;
    private final static Integer limit = 10;

    public TopArtistsAnalysis() {
        this.year = null;
        this.month = null;
    }

    public TopArtistsAnalysis(Integer year, Integer month) {
        this.year = (year == null || year == -1) ? null : year;
        this.month = (month == null || month == -1) ? null : month;
    }

    @Override
    public List<String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api) {
        Stream<StreamingHistoryEntry> filteredEntries = entries.stream();

        if (year != null) {
            filteredEntries = filteredEntries.filter(entry -> entry.ts().getYear() == year);
        }

        if (month != null) {
            filteredEntries = filteredEntries.filter(entry -> entry.ts().getMonthValue() == month);
        }

        Map<String, Long> artistPlayCounts = filteredEntries
                .collect(Collectors.groupingBy(
                        StreamingHistoryEntry::artistName,
                        Collectors.summingLong(StreamingHistoryEntry::minutesPlayed)
                ));

        List<String> topArtists = artistPlayCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        return api.getArtistIds(topArtists);
    }
}