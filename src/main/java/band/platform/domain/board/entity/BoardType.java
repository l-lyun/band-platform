package band.platform.domain.board.entity;

public enum BoardType {
	FREE,
	RECRUIT,
	EQUIPMENT,
	SECRET,
	PROMOTION,
	AUDIO;

	public boolean isSecret() {
		return this == SECRET;
	}
}
