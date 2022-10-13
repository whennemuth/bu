if (typeof dwr == 'undefined' || dwr.engine == undefined) throw new Error('You must include DWR engine before including this file');

(function() {
  if (dwr.engine._getObject("BudgetService") == undefined) {
    var p;
    
    p = {};
    
    dwr.engine._setObject("BudgetService", p);
  }
})();

