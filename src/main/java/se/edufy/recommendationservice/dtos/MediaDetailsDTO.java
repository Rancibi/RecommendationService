package se.edufy.recommendationservice.dtos;

import java.util.List;

public record MediaDetailsDTO(
        Long id,
        String title,
        List<String> genres,
        List<ArtistDTO> artists
) {}
