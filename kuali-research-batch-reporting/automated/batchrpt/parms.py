import os
import boto3

# Get a parameter from the ssm parameter store and return it.
# If indications show it has been provided directly as an environment variable of the same name, return that instead.
def get(parmname):
  localparms = os.environ.get('LOCAL_PARMS', 'false')
  if localparms.lower() == 'true':
    print("======= Getting local parameter: {pn}...".format(pn=parmname))
    return os.environ.get(parmname)
  else:
    ssmParmName = os.environ.get(parmname)
    ssm = boto3.client('ssm')
    print("======= Getting value of parameterstore entry: {ssmpn}, indicated by environment variable: {pn}".format(ssmpn=ssmParmName, pn=parmname))
    parm = ssm.get_parameter(Name=ssmParmName, WithDecryption=True)
    val = parm['Parameter']['Value']
    return val
