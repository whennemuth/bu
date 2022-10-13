<#
    KUALI-BUID-Create - Invoke a rest API call to snaplogic to transmit the BUID of a newly created Affliate account.
    Author: Henry Louis-Charles, IS&T Systems Engineering
    Date: Sept 17th, 2018
    Project: Kuali PD-COI SnapLogic Real Time Integration Technical Recommendations PROJECT # 947966

    Error Handling: Email send to $KUALINotification, Warnings written to Windows event log.
#>
param 
( 
    [parameter(mandatory = $true)] $BUID
)

# Grab ADUser object
$BUClient = $null
$BUClient = get-aduser -Filter 'bu-ph-id -eq $BUID' -Properties 'bu-ph-id','bu-ph-type','bu-ph-cost-center','mail','sn','bu-ph-employee-group','bu-ph-affiliation','bu-ph-personnel-area','bu-ph-personnel-sub-area','primarygroupid'

# Read Sysconfig file
TRY
{
    $output = (get-date -Format "dd-MMM-yyyy hh:mm:ss ") + "Loading SysConfigs... "
    Write-output $output
    
Get-Content C:\FimConfig\Portal\KUALI-BUID-Create.config | 
? {-not($_ -match "#")} |
Foreach-Object{
   $var = $_.Split('=')
   New-Variable -Scope script -Name $var[0] -Value $var[1]
   write-host $var[0] ::= $var[1]
}

$output = (get-date -Format "dd-MMM-yyyy hh:mm:ss ") +  "Done Loading SysConfigs..."
Write-Output $output

} ## TRY
CATCH
{
    $output = (get-date -Format "dd-MMM-yyyy hh:mm:ss ") +  "Error reading sysconfig file."
    Write-Output $output    
    exit 1
}

# Start Transcript logging if set to true
if($LoggingEnabled -eq "true")
{
    $transcriptpath =  "C:\Logs\KUALI-BUID-Create\KUALI-BUID-Create-" + $BUClient.SamAccountName + ".log"
    Start-Transcript -Append -Path $transcriptpath
}


[Net.ServicePointManager]::SecurityProtocol = 'TLS12'

# Collect Kuali CI Token
try
{
    $error.Clear()    
    Write-Host "Collecting Token from Kuali: " -ForegroundColor Green
    $Kuali_headers = @{}
    $Kuali_headers.Add("Authorization","$Kuali_BasicAuthValue")
    $Kuali_headers.Add("Content-Type","application/json")
    $proxy = 'http://webproxy.bu.edu:8900'
    $Kuali_response = Invoke-RestMethod -Headers $Kuali_headers -Method Post -Uri $Kuali_URL -ContentType "application/json" -Proxy $proxy
    $Kuali_response[0].token
}
catch
{
    
    $message = @"
Good Morning,
     The following error was encountered while collecting the KUALI token for KUALI real=time integration:
     $error
     DO NOT reply to this email.
Thank you
$user
"@

    Send-MailMessage -to $KUALINotification -Subject "Error Collecting KUALI Token - $Landscape" -Body $message -From $user -SmtpServer $SMTPServer
    if($LoggingEnabled -eq "true")
    {
        Stop-Transcript
    }

    exit 1
}

# 1. Validate $BUID for attributes
# 2. Send $BUID to snaplogic
try
{
    Write-host "Checking $BUID for attributes..." -ForegroundColor Green
    if($BUClient.'bu-ph-id' -ne $null -and `
       $BUClient.SamAccountName -ne $null -and `
       $BUClient.mail -ne $null -and`
       $BUClient.GivenName -ne $null -and`
       $BUClient.sn -ne $null)
    {
        write-host "          SamAccountName: " $BUClient.SamAccountName
        write-host "                    mail: " $BUClient.mail
        write-host "               GivenName: " $BUClient.GivenName
        write-host "                      SN: " $BUClient.sn
        write-host "                bu-ph-id: " $BUClient.'bu-ph-id'
        write-host "              bu-ph-type: " $BUClient.'bu-ph-type'
        write-host "       bu-ph-cost-center: " $BUClient.'bu-ph-cost-center'
        write-host "    bu-ph-employee-group: " $BUClient.'bu-ph-employee-group'
        write-host "       bu-ph-affiliation: " $BUClient.'bu-ph-affiliation'
        write-host "    bu-ph-personnel-area: " $BUClient.'bu-ph-personnel-area'
        write-host "bu-ph-personnel-sub-area: " $BUClient.'bu-ph-personnel-sub-area'
        write-host "          primarygroupid: " $BUClient.'primarygroupid'

        Write-Host "Validation complete, sending $BUID to snaplogic..." -ForegroundColor Green

        $params = "" | Select UNIVERSITY_ID,Token
        $params.UNIVERSITY_ID = $BUID
        $params.Token = $Kuali_response[0].token
                        
        $headers = @{}
        $headers.Add("Authorization",$SnapLogicBearer)
        $headers.Add("Content-Type","application/json")
        
        $proxy = 'http://webproxy.bu.edu:8900'
        
        $retry = $false
        $count = 0

        # 1. make three attempts to send $BUID to Snaplogic
        # 2. check for errors send email notification on third failure
        do
        {                                                 
            $response = Invoke-RestMethod -Headers $headers -Uri $SnapLogicUrl -Method Post -Body ($params | ConvertTo-Json) -ContentType "application/json" -Proxy $Proxy
            ++$count

            # DEBUG - list responses to log to transcript
            Write-Host "AD: " -ForegroundColor Yellow
            $response[0].response.Ad.response | fl
            Write-Host "KualiCore: " -ForegroundColor Yellow
            $response[0].response.KualiCore.response | fl
            Write-Host "KualiKIM: " -ForegroundColor Yellow
            $response[0].response.KualiKIM.response | Select-Object -Property message,statusCode,error,reason,resolution | fl
            # this line left for testing Kuali KIM original json, uncomment to view payload
            # $response[0].response.KualiKIM.response.original | ConvertTo-Json
            
            # Check for error
            if(($response[0].response.Ad.response.statusCode -gt 499) -or `
            ($response[0].response.KualiCore.response.statusCode -gt 499) -or `
            ($response[0].response.KualiKIM.response.statusCode -gt 499))
            {
                $retry = $true
                    
                if($count -ne 3) 
                {
                    Write-Host "Error encountered, retry in 15 seconds..." -ForegroundColor Yellow
                    Start-Sleep -s 15
                }
            }
            else
            {
                $retry = $false
            }

            # On third failure write out error stack to event Log
            if(($retry -eq $true) -and ($count -eq 3))
            {
               $message = "$BUClient - Error while creating Kuali Account..."         
               $message += "`nSnapLogic:"
               $message += ($response[0].response.Ad.response | fl) | Out-String
               $message += "KualiCore:"
               $message += ($response[0].response.KualiCore.response | fl) | Out-String               
               $message += "KualiKIM:"
               $message += ($response[0].response.KualiKIM.response | Select-Object -Property message,statusCode,error,reason,resolution | fl) | Out-String
               
               Write-Host "******** DEBUG ********" -ForegroundColor Green
               $message
               Write-Host "******** DEBUG *********" -ForegroundColor Green

               # write error trace to event Log
               Write-Eventlog -LogName "Application" -Source "BU IAM Portal" -EventID 203 -EntryType Warning -Message $message

               $mailmessage = @"
Good Morning,
     The following error was encountered while transmitting the UID to KUALI real-time integration:
     $message
     DO NOT reply to this email.
Thank you
$user
"@

               #Send-MailMessage -to $KUALINotification -Subject "Error Creating KUALI account - $Landscape" -Body $mailmessage -From $user -SmtpServer $SMTPServer
               if($LoggingEnabled -eq "true")
               {
                    Stop-Transcript
               }

               exit 1
            }

        } while (($retry -eq $true) -and ($count -ne 3)) 
    }
    else
    {
        $message = "$BUID - attributes are not populated yet...."
        $message += "`nbu-ph-id:       " + $BUClient.'bu-ph-id'
        $message += "`nSamAccountName: " + $BUClient.SamAccountName
        $message += "`nMail:           " + $BUClient.mail
        $message += "`nGivenname:        " + $BUClient.GivenName
        $message += "`nSurname:          " + $BUClient.sn
        Write-Eventlog -LogName "Application" -Source "BU IAM Portal" -EventID 203 -EntryType Warning -Message $message

        if($LoggingEnabled -eq "true")
        {
            Stop-Transcript
        }

        exit 1
    }    
}
catch
{

    write-host "Exception while adding $BUClient.SamAccountName to Kuali:  " -ForegroundColor Green
    $error

    foreach($e in $error)
    {
        $message += "`n" + $e.InvocationInfo.MyCommand + " : " + $e + "`n" + $e.InvocationInfo.PositionMessage + "`n`n"
        Write-Error $message
    }

    Write-Eventlog -LogName "Application" -Source "BU IAM Portal" -EventID 203 -EntryType Error -Message $message

    if($LoggingEnabled -eq "true")
    {
        Stop-Transcript
    }

    exit 1
}

if($LoggingEnabled -eq "true")
{
    Stop-Transcript
}