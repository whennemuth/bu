import os
import subprocess
import re
from batchrpt import parms

def getTnsString(host, port, sn):
    tns = """
    (DESCRIPTION = 
        (ADDRESS_LIST =   
            (FAILOVER = ON) 
            (LOAD_BALANCE = OFF) 
            (ADDRESS = 
                (PROTOCOL = TCP) 
                (HOST = {host}) 
                (PORT = {port})
            )
            (ADDRESS =
                (PROTOCOL = TCP) 
                (HOST = {host}) 
                (PORT = {port})
            )
        ) 
        (CONNECT_DATA = 
            (SERVER = DEDICATED) 
            (SERVICE_NAME = {sn}) 
            (INSTANCE_ROLE = PRIMARY)
            (FAILOVER_MODE = 
                (TYPE = SELECT)
                (METHOD = PRECONNECT)
            )
        )
    )"""
    tns = tns.format(host=host, port=port, sn=sn)
    tns = re.sub('[\n\t\x20]+', '', tns)
    return tns

def defineDbConnection():
    user = parms.get('PARMSTORE_DB_USER') 
    password = parms.get('PARMSTORE_DB_PSWD') 
    host = parms.get('PARMSTORE_DB_HOST')
    tnsstring = getTnsString(host, '1521', 'kuali')
    connection = "{u}/{p}@\"{t}\"".format(u=user, p=password, t=tnsstring)
    print("============== DB Connection String ==============")
    print("     {u}/{p}@\"{t}\"".format(u=user, p='*********', t=tnsstring))
    print("==================================================")
    return connection

def runReportSql(scriptname,filename,dbconnect):
    print("""======= Ready to run report sql: 
        scriptname: {sn}, 
        filename: {fn}, 
        dbconnect: {dbc}""".format(sn=scriptname, fn=filename, dbc=dbconnect[ 0 : 10 ]+'...'))
    s = '@' + scriptname
    f = open(filename, 'w')
    subprocess.run(['sqlcl', '-S', dbconnect, s], stdout=f)
    if os.environ.get('PUBLISH_REPORTS', 'true') == 'false':
        # Since we are not publishing the report, print its content out here.
        print("============================== Contents of {csv} ==============================".format(csv=filename))
        with open(filename, 'r') as f:
            print(f.read()) 
        print("=======================================================================================")
    else:
        print("======= Report process started")

# Generate a CSV report file for each SQL script in list.
def generateCsvReports(sql_scripts, csv_files):
    db = defineDbConnection()
    for s, f in zip(sql_scripts, csv_files):
        runReportSql(s,f,db)