package band.platform.domain.board.entity;

import band.platform.domain.user.entity.User;
import band.platform.global.entity.BaseEntity;
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
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BoardType boardType;

	private Post(String title, String content, User author, BoardType boardType) {
		this.title = title;
		this.content = content;
		this.author = author;
		this.boardType = boardType;
	}

	public static Post create(String title, String content, User author, BoardType boardType) {
		return new Post(title, content, author, boardType);
	}
}
