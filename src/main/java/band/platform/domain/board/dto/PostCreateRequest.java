package band.platform.domain.board.dto;

import java.util.List;

import band.platform.domain.board.entity.BoardType;
import jakarta.validation.Valid;
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
	@Size(max = 65535)
	String content,

	@Size(max = 20)
	List<@Valid PostMediaRequest> mediaItems
) {

	public PostCreateRequest(BoardType boardType, String title, String content) {
		this(boardType, title, content, List.of());
	}

	public PostCreateRequest {
		mediaItems = mediaItems == null ? List.of() : List.copyOf(mediaItems);
	}
}
