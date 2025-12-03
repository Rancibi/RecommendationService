package se.edufy.recommendationservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import se.edufy.recommendationservice.clients.MediaClient;
import se.edufy.recommendationservice.clients.PlayClient;
import se.edufy.recommendationservice.clients.RatingClient;
import se.edufy.recommendationservice.dtos.ArtistDTO;
import se.edufy.recommendationservice.dtos.MediaDetailsDTO;
import se.edufy.recommendationservice.dtos.PlayDTO;
import se.edufy.recommendationservice.dtos.RatingDTO;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RatingClient ratingClient;

    @Mock
    private PlayClient playClient;

    @Mock
    private MediaClient mediaClient;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private RecommendationService recommendationService;

    private List<MediaDetailsDTO> allMedia;

    @BeforeEach
    void setUp() {
        allMedia = List.of(
                new MediaDetailsDTO(1L, "Media 1", List.of("Action"), List.of(new ArtistDTO(1L, "Artist A"))),
                new MediaDetailsDTO(2L, "Media 2", List.of("Comedy"), List.of(new ArtistDTO(2L, "Artist B"))),
                new MediaDetailsDTO(3L, "Media 3", List.of("Action", "Thriller"), List.of(new ArtistDTO(3L, "Artist C"))),
                new MediaDetailsDTO(4L, "Media 4", List.of("Horror"), List.of(new ArtistDTO(4L, "Artist D"))),
                new MediaDetailsDTO(5L, "Media 5", List.of("Comedy"), List.of(new ArtistDTO(5L, "Artist E"))),
                new MediaDetailsDTO(6L, "Media 6", List.of("Drama"), List.of(new ArtistDTO(6L, "Artist F"))),
                new MediaDetailsDTO(7L, "Media 7", List.of("Action"), List.of(new ArtistDTO(7L, "Artist G"))),
                new MediaDetailsDTO(8L, "Media 8", List.of("Thriller"), List.of(new ArtistDTO(8L, "Artist H"))),
                new MediaDetailsDTO(9L, "Media 9", List.of("Action"), List.of(new ArtistDTO(9L, "Artist I"))),
                new MediaDetailsDTO(10L, "Media 10", List.of("Comedy"), List.of(new ArtistDTO(10L, "Artist J")))
        );
    }


    @Test
    void testRecommendations_basicFlow() {
        when(jwt.getSubject()).thenReturn("user1");

        when(playClient.getUserPlays(jwt)).thenReturn(List.of(
                new PlayDTO(1L, 1),
                new PlayDTO(3L, 2)
        ));

        when(ratingClient.getRatingsForUser("user1", jwt)).thenReturn(List.of(
                new RatingDTO(2L, "user1", "2", false)
        ));

        when(mediaClient.getAllMedia(jwt)).thenReturn(allMedia);

        List<MediaDetailsDTO> recommendations = recommendationService.recommend(jwt);

        assertNotNull(recommendations);
        assertTrue(recommendations.size() <= 10);
        assertFalse(recommendations.stream().anyMatch(m -> m.id() == 1L || m.id() == 2L || m.id() == 3L));
    }

    @Test
    void testRecommendations_noPlays_someDislikes() {
        when(jwt.getSubject()).thenReturn("user2");

        when(playClient.getUserPlays(jwt)).thenReturn(Collections.emptyList());
        when(ratingClient.getRatingsForUser("user2", jwt)).thenReturn(List.of(
                new RatingDTO(1L, "user2", "1", false),
                new RatingDTO(2L, "user2", "2", false)
        ));
        when(mediaClient.getAllMedia(jwt)).thenReturn(allMedia);

        List<MediaDetailsDTO> recommendations = recommendationService.recommend(jwt);

        assertNotNull(recommendations);
        assertFalse(recommendations.stream().anyMatch(m -> m.id() == 1L || m.id() == 2L));
        assertTrue(recommendations.size() <= 10);
    }

    @Test
    void testRecommendations_allPlayed() {
        when(jwt.getSubject()).thenReturn("user3");

        List<PlayDTO> plays = new ArrayList<>();
        for (MediaDetailsDTO m : allMedia) {
            plays.add(new PlayDTO(m.id(), 1));
        }

        when(playClient.getUserPlays(jwt)).thenReturn(plays);
        when(ratingClient.getRatingsForUser("user3", jwt)).thenReturn(List.of(
                new RatingDTO(1L, "user3", "1", false)
        ));
        when(mediaClient.getAllMedia(jwt)).thenReturn(allMedia);

        List<MediaDetailsDTO> recommendations = recommendationService.recommend(jwt);

        assertNotNull(recommendations);
        // fallback ignores disliked media
        assertFalse(recommendations.stream().anyMatch(m -> m.id() == 1L));
    }

    @Test
    void testRecommendations_noPlays_noDislikes() {
        when(jwt.getSubject()).thenReturn("user4");

        when(playClient.getUserPlays(jwt)).thenReturn(Collections.emptyList());
        when(ratingClient.getRatingsForUser("user4", jwt)).thenReturn(Collections.emptyList());
        when(mediaClient.getAllMedia(jwt)).thenReturn(allMedia);

        List<MediaDetailsDTO> recommendations = recommendationService.recommend(jwt);

        assertNotNull(recommendations);
        assertTrue(recommendations.size() <= 10);
    }

    @Test
    void testRecommendations_limitedPreferredAndOther() {
        when(jwt.getSubject()).thenReturn("user5");

        when(playClient.getUserPlays(jwt)).thenReturn(List.of(
                new PlayDTO(1L, 1),
                new PlayDTO(3L, 2)
        ));
        when(ratingClient.getRatingsForUser("user5", jwt)).thenReturn(Collections.emptyList());
        when(mediaClient.getAllMedia(jwt)).thenReturn(allMedia);

        List<MediaDetailsDTO> recommendations = recommendationService.recommend(jwt);

        long preferredCount = recommendations.stream()
                .filter(m -> List.of("Action", "Thriller").stream().anyMatch(g -> m.genres().contains(g)))
                .count();

        assertTrue(preferredCount <= 8);
        assertTrue(recommendations.size() - preferredCount <= 2);
    }
}
