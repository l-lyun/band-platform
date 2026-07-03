package band.platform.domain.board.entity;

import org.springframework.util.StringUtils;

import band.platform.global.entity.BaseEntity;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMedia extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PostMediaType mediaType;

	@Column(nullable = false, length = 2048)
	private String mediaUrl;

	@Column(length = 2048)
	private String thumbnailUrl;

	@Column(length = 255)
	private String originalFileName;

	@Column(length = 100)
	private String contentType;

	@Column
	private Long fileSizeBytes;

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private Boolean deleted;

	private PostMedia(
		Post post,
		PostMediaType mediaType,
		String mediaUrl,
		String thumbnailUrl,
		String originalFileName,
		String contentType,
		Long fileSizeBytes,
		Integer sortOrder
	) {
		validate(post, mediaType, mediaUrl, sortOrder);
		this.post = post;
		this.mediaType = mediaType;
		this.mediaUrl = mediaUrl;
		this.thumbnailUrl = thumbnailUrl;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
		this.fileSizeBytes = fileSizeBytes;
		this.sortOrder = sortOrder;
		this.deleted = false;
	}

	public static PostMedia create(
		Post post,
		PostMediaType mediaType,
		String mediaUrl,
		String thumbnailUrl,
		String originalFileName,
		String contentType,
		Long fileSizeBytes,
		Integer sortOrder
	) {
		return new PostMedia(
			post,
			mediaType,
			mediaUrl,
			thumbnailUrl,
			originalFileName,
			contentType,
			fileSizeBytes,
			sortOrder
		);
	}

	public void markDeleted() {
		this.deleted = true;
	}

	private static void validate(Post post, PostMediaType mediaType, String mediaUrl, Integer sortOrder) {
		if (post == null || mediaType == null || !StringUtils.hasText(mediaUrl) || sortOrder == null || sortOrder < 0) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}
}
