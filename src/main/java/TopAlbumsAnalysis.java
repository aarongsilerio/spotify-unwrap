import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopAlbumsAnalysis implements Analysis {
    @Override
    public List<String> analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api, int limit) {
        Map<String, Long> albumPlayCounts = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.albumName() + " - " + entry.artistName(),
                        Collectors.summingLong(StreamingHistoryEntry::minutesPlayed)
                ));

        List<Map.Entry<String, Long>> sortedAlbums = albumPlayCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());

        return api.getAlbumUris(sortedAlbums);
    }
}