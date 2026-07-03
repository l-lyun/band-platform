package band.platform.domain.board.dto;

import java.util.List;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public record PostCreateResponse(
	Long id,
	BoardType boardType,
	String title,
	String content,
	Long authorId,
	List<PostMediaResponse> mediaItems
) {

	public PostCreateResponse(Long id, BoardType boardType, String title, String content, Long authorId) {
		this(id, boardType, title, content, authorId, List.of());
	}

	public PostCreateResponse {
		mediaItems = mediaItems == null ? List.of() : List.copyOf(mediaItems);
	}

	public static PostCreateResponse from(Post post) {
		return from(post, List.of());
	}

	public static PostCreateResponse from(Post post, List<PostMediaResponse> mediaItems) {
		return new PostCreateResponse(
			post.getId(),
			post.getBoardType(),
			post.getTitle(),
			post.getContent(),
			post.getAuthor().getId(),
			mediaItems
		);
	}
}
