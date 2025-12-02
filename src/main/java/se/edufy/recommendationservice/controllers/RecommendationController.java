package se.edufy.recommendationservice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import se.edufy.recommendationservice.dtos.MediaDetailsDTO;
import se.edufy.recommendationservice.services.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/edufy/v1/recommendations")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<MediaDetailsDTO>> getRecommendations(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            log.warn("Missing JWT when requesting recommendations");
            return ResponseEntity.status(401).build();
        }

        String userId = jwt.getSubject();
        log.info("Recommendation request for Keycloak user: {}", userId);

        List<MediaDetailsDTO> results = recommendationService.recommend(jwt);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
