package se.edufy.recommendationservice.dtos;

public record RatingDTO (
        Long id,
        String userId,
        String mediaId,
        boolean liked
){
}
