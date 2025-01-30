import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpotifyDataService {
    public static List<StreamingHistoryEntry> parseCsv(InputStream inputStream) throws IOException {
        Reader reader = new InputStreamReader(inputStream);
        CSVParser csvParser = CSVParser.parse(reader, CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build());

        List<StreamingHistoryEntry> entries = new ArrayList<>();
        for (CSVRecord record : csvParser) {
            StreamingHistoryEntry entry = new StreamingHistoryEntry(
                    record.get("ts"),
                    record.get("ms_played"),
                    record.get("master_metadata_track_name"),
                    record.get("master_metadata_album_artist_name"),
                    record.get("master_metadata_album_album_name"),
                    record.get("spotify_track_uri"),
                    record.get("reason_start"),
                    record.get("reason_end")
            );
            entries.add(entry);
        }
        return entries;
    }

    //updated the summing long part in get methods
    public static List<String> getTopTracks(List<StreamingHistoryEntry> entries, int limit) {
        return entries.stream()
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

    public static List<String> getTopTracks(List<StreamingHistoryEntry> entries, int year, int limit) {
        return entries.stream()
                .filter(entry -> entry.ts().getYear() == year)
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

    public static List<String> getTopTracks(List<StreamingHistoryEntry> entries, int year, int month, int limit) {
        return entries.stream()
                .filter(entry -> entry.ts().getYear() == year && entry.ts().getMonthValue() == month)
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

    public static List<String> getTopArtists(List<StreamingHistoryEntry> entries, int limit, SpotifyAPIService api) {
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

        System.out.println(topArtists);

        return api.getArtistIds(topArtists);
    }

    public static List<String> getTopAlbums(List<StreamingHistoryEntry> entries, int limit, SpotifyAPIService api) {
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

    public static List<String> getPlayedSongsByDate(List<StreamingHistoryEntry> entries, String date, int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, formatter);

        return entries.stream()
                .filter(entry -> entry.ts().toLocalDate().isEqual(localDate))
                .map(StreamingHistoryEntry::spotifyTrackUri).distinct().collect(Collectors.toList());
    }

    //updated the map to long part
    public static long getTotalListeningTime(List<StreamingHistoryEntry> entries) {
        return entries.stream()
                .mapToLong(StreamingHistoryEntry::minutesPlayed)
                .sum();
    }

    //updated the summing long part
    public static Map<DayOfWeek, Long> getMostListenedToDays(List<StreamingHistoryEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.ts().getDayOfWeek(),
                        Collectors.summingLong(StreamingHistoryEntry::minutesPlayed)
                ));
    }
}