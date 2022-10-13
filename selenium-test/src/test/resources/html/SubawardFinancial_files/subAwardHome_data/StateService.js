if (typeof dwr == 'undefined' || dwr.engine == undefined) throw new Error('You must include DWR engine before including this file');

(function() {
  if (dwr.engine._getObject("StateService") == undefined) {
    var p;
    
    p = {};

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.findAllStatesInCountry = function(p0, callback) {
      return dwr.engine._execute(p._path, 'StateService', 'findAllStatesInCountry', arguments);
    };

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.findAllStatesInCountryByAltCode = function(p0, callback) {
      return dwr.engine._execute(p._path, 'StateService', 'findAllStatesInCountryByAltCode', arguments);
    };
    
    dwr.engine._setObject("StateService", p);
  }
})();

