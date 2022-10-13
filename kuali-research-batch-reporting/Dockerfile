# Install required applications to process KC batch reports in AWS Batch. Including...
#     Python3 - used to drive Python3 based packages
#     XlsxWriter - Python3 package to format batch report CSV files to Excel
#     Boto3 - Python3 package to communicate with AWS S3
#     AWScli - push completed batch report up to AWS S3 for distribution.
#     SQLcl - connect to Oracle database to run SQL query in batch and generate CSV report

# Base image installs Java JRE and sets JAVA_HOME.
# We're using java 8 as SQLcl 20.2 does not currently work with later Java versions! 
FROM openjdk:8-jre-alpine

# Symlink Python to Python3 directory so you can type "python" or "python3" for Python3.
# Upgrade pip3 to prevent warnings about running old versions of pip3.
# The ncurses library is required in Alpine to allow use of 'tput' terminal commands.
RUN apk add --no-cache bash ncurses python3 && \
    ln -sv /usr/bin/python3 /usr/bin/python && \ 
    pip3 install --upgrade pip && \
    pip3 install awscli XlsxWriter boto3 && \
    python -m pip install --upgrade debugpy
    
# The "oracle" folder is where we'll dump all Oracle related application stuff.
WORKDIR /oracle

# Copy external zip containing SQLcl to current "oracle" directory then unpack it
# and rename its binary to something more meaningful, like "sqlcl"!!
# https://www.oracle.com/tools/downloads/sqlcl-downloads-202.html
COPY sqlcl*.zip .
RUN unzip sqlcl*.zip && \
    rm sqlcl*.zip && \
    mv sqlcl/bin/sql sqlcl/bin/sqlcl

# The login.sql file sets default system variables used in all KC batch reports.
# SQLcl looks for login.sql in directory pointed to by SQLPATH environment variable. 
COPY login.sql sqlcl/bin/

# We're done installing oracle stuff so we can reset workdir.
WORKDIR /

# Add environment variables needed by Oracle SQLcl to find stuff.
# Extend existing PATH environment variable to include SQLPATH and LD_LIBRARY_PATH.
# Include non-sensitive AWS settings. Sensitive AWS credential values will be loaded at runtime.
# Sensitive variables are: AWS_ACCESS_KEY_ID, and AWS_SECRET_ACCESS_KEY. 
ENV ORACLE_HOME=/oracle \ 
    TNS_ADMIN=/oracle/network/admin \
    LD_LIBRARY_PATH=/oracle/sqlcl/lib \
    SQLPATH=/oracle/sqlcl/bin \ 
    PATH=/oracle/sqlcl/bin:/oracle/sqlcl/lib:$PATH

# We'll dump all the KC batch job scripts in the "automated" folder.
# It is also directory we want set as current when the container runs. 
WORKDIR /automated
COPY automated .

# Bash script runs when docker container starts. AWS Cloudwatch is used to schedule batch jobs.
# Each Cloudwatch batch job definition includes a Command overriding the Docker CMD to 
# specify the particular batch job to run as a parameter to the loader script.
CMD ["sh", "_loader.sh"]