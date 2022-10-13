const AWS = require('aws-sdk');
const db = new AWS.DynamoDB();

switch(process.env.DEBUG_MODE) {
  case 'mock-response':
    var response = require('./mock-cfn-response');
    break;
  default:
    var response = require('cfn-response');
    break;
}

exports.handler = function (event, context) {
  try {
    console.log(JSON.stringify(event, null, 2));
    var tablename = event.ResourceProperties.table;
    if (/^((create)|(update))$/i.test(event.RequestType)) {
      var item = event.ResourceProperties.entry;
      if(typeof item == 'string') {
        console.log("Item is of type string - converting to map...");
        item = JSON.parse(item);
      }
      db.putItem(
        { TableName: tablename, Item: item }, 
        function(err, data) {                
          sendResponse(event, context, data, err);
      });
    }
    else if(/^delete$/i.test(event.RequestType)) {
      console.log(`${tablename} will be removed during stack delete - no need to delete single item.`);
      sendResponse(event, context, { NoAction: 'true' }, null);
    }
  }
  catch(e) {
    sendResponse(event, context, null, e);
  }
}

const sendResponse = (event, context, data, err) => {
  if(err) {
    console.error(err);
    response.send(event, context, response.FAILURE, {
       Value: { error: { name: err.name, message: err.message } 
    }});
  }
  else {
    console.log(data)
    response.send(event, context, response.SUCCESS, {
      result: 'SUCCESS',
      data: `${data}`
    });
  }
}