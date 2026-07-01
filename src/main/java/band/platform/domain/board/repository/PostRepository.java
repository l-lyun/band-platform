package band.platform.domain.board.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findAllByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);
}
