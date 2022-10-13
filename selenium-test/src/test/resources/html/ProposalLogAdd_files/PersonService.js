if (typeof dwr == 'undefined' || dwr.engine == undefined) throw new Error('You must include DWR engine before including this file');

(function() {
  if (dwr.engine._getObject("PersonService") == undefined) {
    var p;
    
    p = {};

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.getPersonByPrincipalName = function(p0, callback) {
      return dwr.engine._execute(p._path, 'PersonService', 'getPersonByPrincipalName', arguments);
    };

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.getPersonByEmployeeId = function(p0, callback) {
      return dwr.engine._execute(p._path, 'PersonService', 'getPersonByEmployeeId', arguments);
    };
    
    dwr.engine._setObject("PersonService", p);
  }
})();

