package se.edufy.recommendationservice.clients;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.edufy.recommendationservice.dtos.RatingDTO;

import java.util.List;

@Service
public class RatingClient {

    private final RestClient restClient;

    public RatingClient(RestClient  restClient) {
        this.restClient = restClient
                .mutate()
                .baseUrl("http://gateway:4646/edufy/v1/ratings")
                .build();
    }

    public List<RatingDTO> getRatingsForUser(String userId, Jwt jwt) {
        return restClient.get()
                .uri("/user/" + userId)
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .body(new ParameterizedTypeReference<List<RatingDTO>>() {});
    }
}
