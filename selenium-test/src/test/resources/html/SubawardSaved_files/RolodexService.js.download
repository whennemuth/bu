if (typeof dwr == 'undefined' || dwr.engine == undefined) throw new Error('You must include DWR engine before including this file');

(function() {
  if (dwr.engine._getObject("RolodexService") == undefined) {
    var p;
    
    p = {};

    /**
     * @param {class java.lang.Integer} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.getRolodex = function(p0, callback) {
      return dwr.engine._execute(p._path, 'RolodexService', 'getRolodex', arguments);
    };
    
    dwr.engine._setObject("RolodexService", p);
  }
})();

