package band.platform.domain.board.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostMediaRequest;
import band.platform.domain.board.dto.PostMediaResponse;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.entity.PostMedia;
import band.platform.domain.board.repository.PostMediaRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostMediaService {

	private static final int MAX_MEDIA_COUNT = 20;

	private final PostMediaRepository postMediaRepository;

	@Transactional
	public List<PostMediaResponse> attach(Post post, List<PostMediaRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return List.of();
		}

		validateRequests(requests);

		List<PostMedia> mediaItems = requests.stream()
			.sorted(Comparator.comparing(PostMediaRequest::sortOrder))
			.map(request -> PostMedia.create(
				post,
				request.mediaType(),
				request.mediaUrl(),
				request.thumbnailUrl(),
				request.originalFileName(),
				request.contentType(),
				request.fileSizeBytes(),
				request.sortOrder()
			))
			.toList();

		return postMediaRepository.saveAll(mediaItems).stream()
			.sorted(Comparator.comparing(PostMedia::getSortOrder))
			.map(PostMediaResponse::from)
			.toList();
	}

	private void validateRequests(List<PostMediaRequest> requests) {
		if (requests.size() > MAX_MEDIA_COUNT) {
			throwInvalidInput();
		}

		Set<Integer> sortOrders = requests.stream()
			.map(PostMediaRequest::sortOrder)
			.collect(Collectors.toSet());

		if (sortOrders.size() != requests.size()) {
			throwInvalidInput();
		}

		for (int index = 0; index < requests.size(); index++) {
			if (!sortOrders.contains(index)) {
				throwInvalidInput();
			}
		}
	}

	private void throwInvalidInput() {
		throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
	}
}
