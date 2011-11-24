package controllers

import models.*

class Security extends Secure.Security {
	
	static boolean authentify(String username, String password) {
		User.connect(username, password) != null
	}

	static boolean check(String profile) {
		if ('admin' == profile) {
			return User.find('byEmail', connected()).first().isAdmin
		}
		return false
	}

	static void onDisconnected() {
		Application.index()
	}

	static void onAuthenticated() {
		Admin.index()
	}
}