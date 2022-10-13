'use strict';

module.exports = function(myparm) {
  return function(req, res, next) {
    var msg = 'apples';
    console.log('Start Middleware1, msg = ' + msg + '...');
    res.write(myparm.msgLabel + msg + JSON.stringify(process.env, null, 4));
    console.log('End Middleware1...');
    next();
  }
};
