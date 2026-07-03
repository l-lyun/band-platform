package band.platform.domain.board.dto;

import band.platform.domain.board.entity.PostMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PostMediaRequest(
	@NotNull
	PostMediaType mediaType,

	@NotBlank
	@Size(max = 2048)
	String mediaUrl,

	@Size(max = 2048)
	String thumbnailUrl,

	@Size(max = 255)
	String originalFileName,

	@Size(max = 100)
	String contentType,

	@PositiveOrZero
	Long fileSizeBytes,

	Integer sortOrder
) {
}
