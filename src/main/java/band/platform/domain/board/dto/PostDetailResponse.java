package band.platform.domain.board.dto;

import java.time.LocalDateTime;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public record PostDetailResponse(
	Long id,
	BoardType boardType,
	String title,
	String content,
	Long authorId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static PostDetailResponse from(Post post) {
		return new PostDetailResponse(
			post.getId(),
			post.getBoardType(),
			post.getTitle(),
			post.getContent(),
			post.getAuthor().getId(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
