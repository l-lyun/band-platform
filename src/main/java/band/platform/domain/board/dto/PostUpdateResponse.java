package band.platform.domain.board.dto;

import java.time.LocalDateTime;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public record PostUpdateResponse(
	Long id,
	BoardType boardType,
	String title,
	String content,
	Long authorId,
	LocalDateTime updatedAt
) {

	public static PostUpdateResponse from(Post post) {
		return new PostUpdateResponse(
			post.getId(),
			post.getBoardType(),
			post.getTitle(),
			post.getContent(),
			post.getAuthor().getId(),
			post.getUpdatedAt()
		);
	}
}
