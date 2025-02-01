import java.util.List;

public interface Analysis {
    Object analyze(List<StreamingHistoryEntry> entries, SpotifyAPIService api);
}