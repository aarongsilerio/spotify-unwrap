import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

public class StreamingHistoryEntry {
    private LocalDateTime ts;
    private long msPlayed;
    private String trackName;
    private String artistName;
    private String albumName;
    private String spotifyTrackUri;
    private String reasonStart;
    private String reasonEnd;

    public StreamingHistoryEntry(String ts, String msPlayed, String trackName, String artistName,
                                 String albumName, String spotifyTrackUri, String reasonStart,
                                 String reasonEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
        this.ts = LocalDateTime.parse(ts, formatter);
        this.msPlayed = Long.parseLong(msPlayed) / 1000 / 60;
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.spotifyTrackUri = spotifyTrackUri;
        this.reasonStart = reasonStart;
        this.reasonEnd = reasonEnd;
    }

    // Getters
    public LocalDateTime getTs() {
        return ts;
    }

    public long getMsPlayed() {
        return msPlayed;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getSpotifyTrackUri() {
        return spotifyTrackUri;
    }

    public String getReasonStart() {
        return reasonStart;
    }

    public String getReasonEnd() {
        return reasonEnd;
    }
}