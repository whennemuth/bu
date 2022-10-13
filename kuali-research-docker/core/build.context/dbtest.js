/**
 * Put this js in the shared folder.
 * cd shared &&
 * ../node_modules/babel-cli/bin/babel-node.js dbtest.js package=(mongodb|mongoose)
 * or...
 * ../node_modules/babel-cli/bin/babel-node.js dbtest.js package=(mongodb|mongoose) password=your_password
 * (inclusion of a password assumes atlas mongodb and ssl authentication).
 */

'use strict';

/**
 * Provided a name value, get the value portion of a string like "name=value".
 */
function NamedArg(name, arg) {
  this.hasValue = function() {
    if(!name || !arg) return false;
    var matches = false;
    eval("matches = /^" + name.trim() + "\\s*=.*$/i.test(arg.trim())");
    return matches;
  }

  this.getValue = function() {
    if(this.hasValue()) {
      var retval = arg.replace(/\s/g, ''); // remove whitespace
      retval = retval.slice(name.length+1, retval.length); // get everything to the right of "="
      return retval;
    }
    return '';
  }

  this.pickValue = function(args) {
    for(arg in args) {
      var retval = new NamedArg(name, args[arg]).getValue();
      if(retval) return retval;
    }
    return '';
  }
}

/** Get an instance of Client that reflects that package specified */
function ClientFactory(process) {
  var args = process.argv.slice(1);
  var pkg = new NamedArg('package').pickValue(args);
  var password = new NamedArg('password').pickValue(args);

  this.isValid = function() {
    if(!pkg) {
      console.log('ERROR! Cannot find package parameter');
      return false;
    }
    return true;
  }

  this.getClient = function() {

    // TODO: Parameterize cluster parameters like these as more NamedArg values that are passed in with the node call.
    //   Example: ../node_modules/babel-cli/bin/babel-node.js dbtest.js package=(mongodb|mongoose) password=your_password primaryShard=... shard2=... etc.
    //
    var primaryShard = 'sb-cluster-shard-00-01-ozx2o.mongodb.net';
    var shard2 = 'sb-cluster-shard-00-00-ozx2o.mongodb.net';
    var shard3 = 'sb-cluster-shard-00-02-ozx2o.mongodb.net';
    var replicaSet = 'sb-cluster-shard-0';

    // var primaryShard = 'ci-cluster-shard-00-00-nzjcq.mongodb.net';
    // var shard2 = 'ci-cluster-shard-00-01-nzjcq.mongodb.net';
    // var shard3 = 'ci-cluster-shard-00-02-nzjcq.mongodb.net';
    // var replicaSet = 'ci-cluster-shard-0';

    var parms = {
      password: password,
      primaryShard: primaryShard,
      shard2: shard2,
      shard3: shard3,
      localUri: 'mongodb://localhost:27017/core-development',
      opts: {
        user: "admin",
        pass : password,
        auth: { authdb: "admin" }, // or add authSource=admin as a query string arg.
        server: {
          socketOptions: { keepAlive: 1, connectTimeoutMS: 30000 },
          sslValidate: false,
          checkServerIdentity: false,
          // Need to include servername here when connecting to an atlas mongodb over ssl because mongoose does not engage TLS SNI (server name identification)
          // support unless you are specific. SEE: http://mongodb.github.io/node-mongodb-native/core/api/Server.html
          servername: primaryShard
        },
        replset: {
          name: replicaSet,
          socketOptions: { keepAlive: 1, connectTimeoutMS: 30000 },
          sslValidate: false,
          checkServerIdentity: false,
          servername: primaryShard
        }
      }  
    }

    switch(pkg) {
      case 'mongodb':
        return new MongoDb(parms);
      case 'mongoose':  
        return new Mongoose(parms);
    } 
  }
}


/**
 * Mongodb client wrapper object.
 * This is the method for data access when migrations are run.
 * NOTE: The mongo client connection uri is based on what works for the mongoose uri, but with extra querystring parameters crammed on to the end.
 * If we could pass an opts object to the mongo client connect method we would not need to do this.
 */
function MongoDb(parms) {
  var uri;
  if(parms.password) {
    // You can get away with specifying only shard1 below. (would that impact any kind of failover?)
    uri = 'mongodb://' +
      parms.opts.user + ':' +
      parms.opts.pass + '@' + 
      parms.primaryShard + ':27017,' +
      parms.shard2 + ':27017,' +
      parms.shard3 + ':27017/core-development' +
      '?replicaSet=' + parms.opts.replset.name +
      '&ssl=true' +
      '&authSource=' + parms.opts.auth.authdb +
      '&connectTimeoutMS=' + parms.opts.server.socketOptions.connectTimeoutMS
  }
  else {
    uri = parms.localUri;
  }

  this.getUri = function() { return uri; }

  var MongoClient = require('mongodb').MongoClient;

  this.logFirstInstitution = function () {
    return new Promise((resolve, reject) => {
      console.log('CONNECTING WITH: ' + uri);
  
      MongoClient.connect(uri, function(err, db) {
        if (err) {
          console.log("CONNECTION ERROR!!!");
          console.log(err);
          reject('');
          return;
        }
  
        var Institution = db.collection('institutions');
        Institution.find({}).toArray(function(error, docs) {
          if(error) {
            console.log("FINDONE ERROR!!!");
            console.log(error);
            reject('');
            return;
          }
  
          console.log("");
          console.log("--------- INSTITUTION #1 ---------");
          console.log(docs);
          console.log("");
  
          db.close(true, function(err, result) {
            if(err) {
              console.log("ERROR! Problem closing connection");
              reject('');
              return;
            }
            resolve('Operation completed successfully');
          });
        });
      });
    });
  }

  this.logCollections = function(parms) {
    return new Promise((resolve, reject) => {
      console.log('CONNECTING WITH: ' + uri);
  
      MongoClient.connect(uri, function(err, db) {
        if (err) {
          console.log("CONNECTION ERROR!!!");
          console.log(err);
          reject('');
          return;
        }
  
        var cursor = db.listCollections().toArray(function(err, colInfos) {
          if(err) {
            console.log('ERROR getting collection information!');
            reject('');
            return;
          }
          if(colInfos) {
            console.log("");
            console.log("--------- COLLECTIONS ---------");
            for(var i=0; i<colInfos.length; i++) {
              var info = colInfos[i];
              console.log(info.name);
            }
            console.log("");
          }
          else {
            console.log('ERROR colInfos has no value');
            reject('');
            return;
          }
          db.close(true, function(err, result) {
            if(err) {
              console.log("ERROR! Problem closing connection");
              reject('');
              return;
            }
            resolve('Operation completed successfully');
          });
        });
      });
    });
  }
}


/**
 * Object wrapper for a mongodb connection through mongoose.
 * This is the method for most of the core application data access.
 * NOTE: You can pass an options parameter to the connect method to avoid lengthy querystrings in the uri.
 * Also there are parameters that can be included in the options object that wouldn't be recognized as a querystring parameter.
 */
function Mongoose(parms) {
  var uri;
  var opts;
  if(parms.password) {
    // You can get away with specifying only shard1 below. (would that impact any kind of failover?)
    uri = 'mongodb://' +
      parms.primaryShard + ':27017,' + 
      parms.shard2 + ':27017,' + 
      parms.shard3 + ':27017/core-development' + 
      '?replicaSet=' + parms.opts.replset.name + 
      '&ssl=true'

    opts = parms.opts;

    // The above uri will work if opts is also passed to the connect method.
    // However, here I want to test passing the opts object while having it's fields ALSO reflected in the querystring
    // Using the MongoDb uri will accomplish this.
    // This approach becomes necessary when you want to specify a shared uri in locals.js - a uri that works for both 
    // migration and mongoose related db access.
    //    NOTE: Even though you use a common URI, don't forget to pass the opts object to mongoose because mongoose 
    //    ignores a lot of parameters you put on the URI querystring and only reads them from the opts object.
    uri = new MongoDb(parms).getUri()

  }
  else {
    uri = parms.localUri;
    // Reduce the opts object
    opts = {
      server: parms.opts.server,
      replset: parms.opts.replset
    };
  }

  this.getUri = function() { return uri; }

  this.getOpts = function() { return opts; }

  var MongooseClient = require('mongoose');

  this.logFirstInstitution = function () {
    return new Promise((resolve, reject) => {
      console.log('CONNECTING WITH: ' + uri);    
      MongooseClient.connect(uri, opts).then(
        () => {
          var Schema = MongooseClient.Schema;
          // NOTE: mongoose seems to be smart enough to 'intuit' the collection you mean if you misspell its name singular vs. plural and vice versa.
          var Institution = MongooseClient.model('institutions', new Schema({ name: String }));
          Institution.findOne(function(error, result) {
            if(error) {
               console.log("FINDONE ERROR!!!");
               console.log(error);
               reject('');
               return;
            }
            console.log("");
            console.log("--------- INSTITUTION #1 ---------");
            console.log(result);
            console.log("");
            MongooseClient.connection.close();
            MongooseClient.connection.close(function(){
              resolve('Operation completed successfully');
            });
          });
        },
        err => {
          console.log("CONNECTION ERROR!!!");
          console.log(err);
          reject('');
        }
      );
    });
  }

  this.logCollections = function(parms) {
    return new Promise((resolve, reject) => {
      console.log('CONNECTING WITH: ' + uri);
      MongooseClient.connect(uri, opts).then(
        () => {
          MongooseClient.connection.db.listCollections().toArray(function(err, colInfos) {
            if(err) {
              console.log('ERROR getting collection information!');
              reject(err);
              return;
            }
            if(colInfos) {
              console.log("");
              console.log("--------- COLLECTIONS ---------");
              for(var i=0; i<colInfos.length; i++) {
                var info = colInfos[i];
                console.log(info.name);
              }
              console.log("");
            }
            else {
              var msg = 'ERROR colInfos has no value';
              console.log(msg);
              reject(msg);
              return;
            }
            MongooseClient.connection.close(function(){
              resolve('Operation completed successfully');
            });
          });
        },
        err => {
          console.log("CONNECTION ERROR!!!");
          console.log(err);
          return;
        }
      );
    });
  }
}


var factory = new ClientFactory(process);
if(factory.isValid()) {
  var client = factory.getClient();
  // Gotta use promises so that the connection of the client - one based on mongoose, at least - doesn't block the connection of the other client.
  // The promise of each client is only resolved when its connection close method has returned.
  client.logCollections()
    .then((successMessage) => {
      console.log('Collection logging result: ' + successMessage);
      client.logFirstInstitution()
        .then((successMessage) => {
          console.log('Institution logging result: ' + successMessage);
        })
        .catch((reason) => {
          if(!reason) return;
          console.log('Institution logging failure: ' + reason);
        });
    })
    .catch((reason) => {
      if(!reason) return;
      console.log('Collection logging failure: ' + reason);
    });
}
