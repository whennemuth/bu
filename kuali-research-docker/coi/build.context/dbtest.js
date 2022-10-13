var knex = require('./knexfile.js');

if(knex.kc_coi.client == 'oracledb') {
	var oracledb = require('oracledb');
	oracledb.getConnection(
	  {
	    user          : knex.kc_coi.connection.user,
	    password      : knex.kc_coi.connection.password,
	    connectString : knex.kc_coi.connection.host + ':' + knex.kc_coi.connection.port + '/' + knex.kc_coi.connection.database
	  },
	  function(err, connection)
	  {
	    if (err) {
	      console.error(err.message);
	      return;
	    }
	    connection.execute(
	      "SELECT * FROM \"COI\".\"knex_migrations\"",
	      // The callback function handles the SQL execution results
	      function(err, result)
	      {
	        if (err) {
	          console.error(err.message);
	          doRelease(connection);
	          return;
	        }
	        console.log(result.metaData);
	        console.log(result.rows);
	        doRelease(connection);
	      });
	  });
	
	// Note: connections should always be released when not needed
	function doRelease(connection)
	{
	  connection.close(
	    function(err) {
	      if (err) {
	        console.error(err.message);
	      }
	    });
	}
}
else {
	var mysql = require('mysql');
	var connection = mysql.createConnection({
	  host     : knex.kc_coi.connection.host,
	  user     : knex.kc_coi.connection.user,
	  password : knex.kc_coi.connection.password,
	  database : knex.kc_coi.connection.database
	});
	
	connection.connect();
	
	connection.query('SELECT * FROM knex_migrations', function (error, results, fields) {
	  if (error) {
		console.log('ERROR: ' + error);
		throw error;
	  }
	for(var i=0; i<results.length; i++) {
	  console.log('Row1: ', results[i]);
	}
	});
	
	connection.end();
}
