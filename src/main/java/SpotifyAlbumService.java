import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchAlbumsRequest;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpotifyAlbumService {

    private final SpotifyApi spotifyApi;

    public SpotifyAlbumService(String clientId, String clientSecret) {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();

        try {
            String accessToken = getAccessToken();
            spotifyApi.setAccessToken(accessToken);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Error getting access token", e);
        }
    }

    private String getAccessToken() throws IOException, SpotifyWebApiException, ParseException {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        ClientCredentials credentials = clientCredentialsRequest.execute();
        return credentials.getAccessToken();
    }

    public List<String> getAlbumUris(List<Map.Entry<String, Long>> albumArtistPairs) {
        List<String> albumUris = new ArrayList<>();
        for (Map.Entry<String, Long> entry : albumArtistPairs) {
            String[] parts = entry.getKey().split(" - ");
            if (parts.length == 2) {
                String albumName = parts[0];
                String artistName = parts[1];
                String albumUri = searchAlbumUri(albumName, artistName);
                if (albumUri != null) {
                    albumUris.add(albumUri);
                }
            }
        }
        return albumUris;
    }

    private String searchAlbumUri(String albumName, String artistName) {
        String q = "album:" + albumName + " artist:" + artistName;
        SearchAlbumsRequest searchAlbumsRequest = spotifyApi.searchAlbums(q)
                .limit(1)
                .build();

        try {
            final Paging<AlbumSimplified> albumSimplifiedPaging = searchAlbumsRequest.execute();

            if (albumSimplifiedPaging.getTotal() > 0) {
                return albumSimplifiedPaging.getItems()[0].getId();
            } else {
                System.out.println("Album not found: " + albumName + " - " + artistName);
                return null;
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error searching for album: " + e.getMessage());
            return null;
        }
    }
}