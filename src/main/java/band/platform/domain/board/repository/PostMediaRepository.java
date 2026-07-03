package band.platform.domain.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.board.entity.PostMedia;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

	List<PostMedia> findAllByPostIdAndDeletedFalseOrderBySortOrderAsc(Long postId);

	long countByPostIdAndDeletedFalse(Long postId);
}
