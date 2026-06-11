package band.platform.global.security;

final class PublicEndpoints {

	static final String[] USER_POST_ENDPOINTS = {
		"/api/users/sign-*"
	};

	static final String[] AUTH_POST_ENDPOINTS = {
		"/api/auth/signup",
		"/api/auth/login"
	};

	static final String[] OPTIONS_ENDPOINTS = {
		"/**"
	};

	static final String[] ERROR_ENDPOINTS = {
		"/error"
	};

	private PublicEndpoints() {
	}

}
