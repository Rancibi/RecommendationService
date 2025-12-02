package se.edufy.recommendationservice.clients;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.edufy.recommendationservice.dtos.PlayDTO;

import java.util.List;

@Service
public class PlayClient {

    private final RestClient restClient;

    public PlayClient(RestClient  restClient) {
        this.restClient = restClient
                .mutate()
                .baseUrl("http://gateway:4646/edufy/v1/users")
                .build();
    }

    public List<PlayDTO> getUserPlays(Jwt jwt) {
        return restClient.get()
                .uri("/plays")
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .body(new ParameterizedTypeReference<List<PlayDTO>>() {});
    }
}
