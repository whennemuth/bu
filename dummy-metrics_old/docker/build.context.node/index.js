// Create a default page for node website that lists all environment variables

'use strict';

const express = require('express');
var middleware = require('./middleware.js');
const PORT = 8080;
const HOST = '0.0.0.0';
const app = express();

app.get('/testing', (req, res, next) => {
  console.log('Start Index...');
  res.contentType('application/json');
  res.write("Index.js...\n");
  next();
  res.end();
  console.log("End Index...\n\n");
});

switch(process.env.NODE_ENV) {
  case 'dev': case undefined:
    app.use('/testing', middleware({ msgLabel: 'The message is: ' }));

    app.use('/testing', (req, res) => {
      console.log('Start Middleware2...');
      res.write("\nMiddleware2...");
      console.log("End Middleware2...");
    });
    break;
  case 'stg':
    console.log("Staging environment configuration not created yet.");
    break;
  case 'prod':
    console.log("Production environment configuration not created yet.");
    break;
}

app.listen(PORT, HOST);

console.log(new Date() + `Running on http://${HOST}:${PORT}`);
