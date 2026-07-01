package band.platform.domain.board.dto;

import java.time.LocalDateTime;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public record PostListItemResponse(
	Long id,
	BoardType boardType,
	String title,
	Long authorId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static PostListItemResponse from(Post post) {
		return new PostListItemResponse(
			post.getId(),
			post.getBoardType(),
			post.getTitle(),
			post.getAuthor().getId(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
