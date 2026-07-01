package band.platform.domain.board.dto;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public record PostCreateResponse(
	Long id,
	BoardType boardType,
	String title,
	String content,
	Long authorId
) {

	public static PostCreateResponse from(Post post) {
		return new PostCreateResponse(
			post.getId(),
			post.getBoardType(),
			post.getTitle(),
			post.getContent(),
			post.getAuthor().getId()
		);
	}
}
