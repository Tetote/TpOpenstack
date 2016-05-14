package util;

public enum ColorUtil {

	RED((char)27 + "[31m"),
	GREEN((char)27 + "[32m"),
	YELLOW((char)27 + "[33m"),
	BLUE((char)27 + "[34m"),
	MAGENTA((char)27 + "[35m"),
	CYAN((char)27 + "[36m");

	private final String color;

	private ColorUtil(String color) {
		this.color = color;
	}

	@Override
	public String toString() {
		return color;
	}

}
