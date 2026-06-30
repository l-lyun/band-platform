package band.platform.domain.board.dto;

import band.platform.domain.board.entity.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
	@NotNull
	BoardType boardType,

	@NotBlank
	@Size(max = 100)
	String title,

	@NotBlank
	String content
) {
}
