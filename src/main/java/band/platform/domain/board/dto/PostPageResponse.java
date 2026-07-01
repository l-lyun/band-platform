package band.platform.domain.board.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import band.platform.domain.board.entity.Post;

public record PostPageResponse(
	List<PostListItemResponse> posts,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {

	public static PostPageResponse from(Page<Post> posts) {
		return new PostPageResponse(
			posts.map(PostListItemResponse::from).toList(),
			posts.getNumber(),
			posts.getSize(),
			posts.getTotalElements(),
			posts.getTotalPages(),
			posts.isFirst(),
			posts.isLast()
		);
	}
}
