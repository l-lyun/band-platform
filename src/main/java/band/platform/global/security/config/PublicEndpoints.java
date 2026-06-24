package band.platform.global.security.config;

public final class PublicEndpoints {

	public static final String[] USER_POST_ENDPOINTS = {
		"/api/users/sign-*"
	};

	public static final String[] AUTH_POST_ENDPOINTS = {
		"/api/auth/signup",
		"/api/auth/login",
		"/api/auth/reissue",
		"/api/auth/logout"
	};

	public static final String[] OPTIONS_ENDPOINTS = {
		"/**"
	};

	public static final String[] ERROR_ENDPOINTS = {
		"/error"
	};

	private PublicEndpoints() {
	}

}
