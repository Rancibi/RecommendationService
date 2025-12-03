package se.edufy.recommendationservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import se.edufy.recommendationservice.clients.*;
import se.edufy.recommendationservice.dtos.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger("RecommendationLogger");

    private final RatingClient ratingClient;
    private final PlayClient playClient;
    private final MediaClient mediaClient;

    public RecommendationService(
            RatingClient ratingClient,
            PlayClient playClient,
            MediaClient mediaClient
    ) {
        this.ratingClient = ratingClient;
        this.playClient = playClient;
        this.mediaClient = mediaClient;
    }

    public List<MediaDetailsDTO> recommend(Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("Generating recommendations for user {}", userId);

        // 1. FETCH PLAYS
        List<PlayDTO> plays = playClient.getUserPlays(jwt);
        Set<Long> playedMedia = plays.stream()
                .map(PlayDTO::mediaId)
                .collect(Collectors.toSet());

        log.debug("User {} has played {} media items", playedMedia.size());

        // 2. FETCH RATINGS (DISLIKES)
        List<RatingDTO> ratings = ratingClient.getRatingsForUser(userId, jwt);

        Set<Long> dislikedMedia = ratings.stream()
                .filter(r -> !r.liked())
                .map(r -> Long.valueOf(r.mediaId()))
                .collect(Collectors.toSet());

        log.debug("User {} has disliked {} media items", userId, dislikedMedia.size());

        // 3. FETCH ALL MEDIA
        List<MediaDetailsDTO> allMedia = mediaClient.getAllMedia(jwt);

        // 4. FILTER OUT played + disliked
        List<MediaDetailsDTO> available = allMedia.stream()
                .filter(m -> !playedMedia.contains(m.id()))
                .filter(m -> !dislikedMedia.contains(m.id()))
                .collect(Collectors.toList());

        log.info("Found {} available media for recommendation", available.size());

        // 5. FALLBACK IF NOTHING AVAILABLE
        if (available.isEmpty()) {
            log.warn("User {} has played all media. Falling back (ignoring disliked)", userId);

            available = allMedia.stream()
                    .filter(m -> !dislikedMedia.contains(m.id()))
                    .collect(Collectors.toList());
        }

        // 6. DETERMINE TOP 3 GENRES FROM PLAY HISTORY
        Map<String, Integer> genreCounts = new HashMap<>();

        for (PlayDTO play : plays) {
            allMedia.stream()
                    .filter(m -> m.id().equals(play.mediaId()))
                    .findFirst()
                    .ifPresent(media -> {
                        for (String genre : media.genres()) {
                            genreCounts.merge(genre, 1, Integer::sum);
                        }
                    });
        }

        List<String> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        log.info("User {} top genres: {}", userId, topGenres);

        // 7. IF NO GENRES → return random 10
        if (topGenres.isEmpty()) {
            log.info("User {} has no play history → generating random 10", userId);

            Collections.shuffle(available);
            return available.stream().limit(10).toList();
        }

        // 8. SPLIT INTO preferred + other genres
        List<MediaDetailsDTO> preferred = new ArrayList<>();
        List<MediaDetailsDTO> others = new ArrayList<>();

        for (MediaDetailsDTO media : available) {
            if (media.genres().stream().anyMatch(topGenres::contains)) {
                preferred.add(media);
            } else {
                others.add(media);
            }
        }

        Collections.shuffle(preferred);
        Collections.shuffle(others);

        // 9. 80/20 SPLIT (8 preferred, 2 others)
        int preferredCount = Math.min(8, preferred.size());
        int otherCount = Math.min(2, others.size());

        List<MediaDetailsDTO> result = new ArrayList<>();
        result.addAll(preferred.subList(0, preferredCount));
        result.addAll(others.subList(0, otherCount));

        log.info("Returning {} final recommendations ({} preferred, {} others)",
                result.size(), preferredCount, otherCount);

        return result;
    }
}
