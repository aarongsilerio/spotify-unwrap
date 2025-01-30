import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopArtistsAnalysis implements Analysis {
    @Override
    public List<String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api, int limit) {
        Map<String, Long> artistPlayCounts = entries.stream()
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