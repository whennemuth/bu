/**
 * Provide services for navigation.
 */
var navigationFactory = function($http, $q) {
	
	var _scope;
	var configCache;
	var tabCache;
	var tabs = function() { };
	tabs.prototype.configClass = 'navigation';
	tabs.prototype.homeClass =   'navigation';
	tabs.prototype.helpClass =   'navigation';
	
	return {
		setScope: function(scope) {
			_scope = scope;
		},
		showJson: function() {
			return _scope.jsoncbx;
		},
		getTabs : function() {
			if(tabCache) {
				return tabCache;
			}
			tabCache = new tabs();
			return tabCache;
		},
		setTabs : function(path) {
			var p = path.replace(/\//g, "");
			tabCache = new tabs();
			eval("tabCache." + p + "Class = 'navigation navigated'");
		}
	};
};