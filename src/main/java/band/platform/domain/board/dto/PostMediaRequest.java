package band.platform.domain.board.dto;

import band.platform.domain.board.entity.PostMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostMediaRequest(
	@NotNull
	PostMediaType mediaType,

	@NotBlank
	String mediaUrl,

	String thumbnailUrl,
	String originalFileName,
	String contentType,
	Long fileSizeBytes,
	Integer sortOrder
) {
}
