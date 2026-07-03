package band.platform.domain.board.dto;

import band.platform.domain.board.entity.PostMedia;
import band.platform.domain.board.entity.PostMediaType;

public record PostMediaResponse(
	Long id,
	PostMediaType mediaType,
	String mediaUrl,
	String thumbnailUrl,
	String originalFileName,
	String contentType,
	Long fileSizeBytes,
	Integer sortOrder
) {

	public static PostMediaResponse from(PostMedia media) {
		return new PostMediaResponse(
			media.getId(),
			media.getMediaType(),
			media.getMediaUrl(),
			media.getThumbnailUrl(),
			media.getOriginalFileName(),
			media.getContentType(),
			media.getFileSizeBytes(),
			media.getSortOrder()
		);
	}
}
