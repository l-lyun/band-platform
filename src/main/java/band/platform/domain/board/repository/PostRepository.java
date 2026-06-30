package band.platform.domain.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	List<Post> findAllByBoardType(BoardType boardType);
}
