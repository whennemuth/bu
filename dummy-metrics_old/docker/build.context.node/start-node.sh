#!/bin/bash
if [ -f /var/env/env.vars.sh ] ; then
  source /var/env/env.vars.sh
else 
  touch /var/env/$(date +%T).missing.env.file
fi

# npm start
npm run-script dev

