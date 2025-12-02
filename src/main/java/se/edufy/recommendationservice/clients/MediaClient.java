package se.edufy.recommendationservice.clients;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.edufy.recommendationservice.dtos.MediaDetailsDTO;

import java.util.List;

@Service
public class MediaClient {

    private final RestClient restClient;

    public MediaClient(RestClient  restClient) {
        this.restClient = restClient
                .mutate()
                .baseUrl("http://gateway:4646/edufy/v1/media")
                .build();
    }

    public List<MediaDetailsDTO> getAllMedia(Jwt jwt) {
        return restClient.get()
                .uri("/details")
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .body(new ParameterizedTypeReference<List<MediaDetailsDTO>>() {});
    }
}
