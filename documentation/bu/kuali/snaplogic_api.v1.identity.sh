getUser() {
  cat <<EOF >> $outfile
  curl --insecure \\
      -X GET \\
      -H "Authorization: Bearer $(getCoreAuthToken)" \\
    https://$(getHostDomain)/kc/research-common/api/v1/identity/entity/$BUID
EOF

  cat $outfile
  [ "$DRYRUN" == 'true' ] && exit 0
  sh $outfile
}

getInstProp() {
  cat <<EOF >> $outfile
  curl --insecure \\
      -X GET \\
      -H "Authorization: Bearer $(getCoreAuthToken)" \\
    https://$(getHostDomain)/kc/instprop/api/v1/institutional-proposals?proposalId=$INSTPROP_ID
EOF

  cat $outfile
  [ "$DRYRUN" == 'true' ] && exit 0
  sh $outfile
}

createUser() {
  cat <<EOF >> $outfile
  curl --insecure \\
      -X POST \\
      -H "Authorization: Bearer $(getCoreAuthToken)" \\
      -H 'Content-Type: application/json' \\
      -d '{
    "principals": {
      "principal": [
        {
          "active": true,
          "entityId": "U18000000",
          "principalId": "U18000000",
          "principalName": "TST002"
        }
      ]
    },
    "entityTypeContactInfos": {
      "entityTypeContactInfo": [
        {
          "active": true,
          "emailAddresses": {
            "emailAddress": [
              {
                "active": true,
                "defaultValue": true,
                "emailAddress": "TST002@bu-dev.com",
                "emailAddressUnmasked": "TST002@bu-dev.com",
                "emailType": {
                  "active": true,
                  "code": "WRK"
                },
                "entityId": "U18000000",
                "entityTypeCode": "PERSON",
                "suppressEmail": false
              }
            ]
          },
          "entityId": "U18000000",
          "entityType": {
            "active": true,
            "code": "PERSON"
          },
          "entityTypeCode": "PERSON"
        }
      ]
    },
    "names": {
      "name": [
        {
          "active": true,
          "compositeName": "Three, One, Two",
          "compositeNameUnmasked": "Three, One, Two",
          "defaultValue": true,
          "entityId": "U18000000",
          "firstName": "One",
          "firstNameUnmasked": "One",
          "lastName": "Three",
          "lastNameUnmasked": "Three",
          "middleName": "Two",
          "nameType": {
            "active": true,
            "code": "PRFR"
          },
          "suppressName": false
        }
      ]
    },
    "active": true,
    "employmentInformation": {
      "employment": [
        {
          "active": true,
          "baseSalaryAmount": "0.00",
          "employeeId": "U18000000",
          "employeeStatus": {
            "active": true,
            "code": "3"
          },
          "employeeType": {
            "active": true,
            "code": "9"
          },
          "employmentRecordId": null,
          "entityAffiliation": {
            "active": true,
            "affiliationType": {
              "active": true,
              "code": "0888",
              "employmentAffiliationType": false
            },
            "campusCode": "10",
            "defaultValue": true,
            "entityId": "U18000000"
          },
          "entityId": "U18000000",
          "primary": true,
          "primaryDepartmentCode": "1202330000"
        }
      ]
    }
  }' \\
    https://$(getHostDomain)/kc/research-common/api/v1/identity/create
EOF
  cat $outfile

  [ "$DRYRUN" == 'true' ] && exit 0

  sh $outfile
}


createUser_old() {
  cat <<EOF >> $outfile
  curl --insecure \\
      -X POST \\
      -H "Authorization: Bearer $(getCoreAuthToken)" \\
      -H 'Content-Type: application/json' \\
      -d '{
      "principals": {
        "principal": [
          {
            "active": true,
            "entityId": "'$BUID'",
            "principalId": "'$BUID'",
            "principalName": "TST001"
          }
        ]
      },
      "entityTypeContactInfos": {
        "entityTypeContactInfo": [
          {
            "active": true,
            "emailAddresses": {
              "emailAddress": [
                {
                  "active": true,
                  "defaultValue": true,
                  "emailAddress": "TST001@bu-dev.com",
                  "emailAddressUnmasked": "TST001@bu-dev.com",
                  "emailType": {
                    "active": true,
                    "code": "WRK"
                  },
                  "entityId": "'$BUID'",
                  "entityTypeCode": "PERSON",
                  "suppressEmail": false
                }
              ]
            },
            "entityId": "'$BUID'",
            "entityType": {
              "active": true,
              "code": "PERSON"
            },
            "entityTypeCode": "PERSON"
          }
        ]
      },
      "names": {
        "name": [
          {
            "active": true,
            "compositeName": "Record, Tester, Fake",
            "compositeNameUnmasked": "Record, Tester, Fake",
            "defaultValue": true,
            "entityId": "'$BUID'",
            "firstName": "Tester",
            "firstNameUnmasked": "Tester",
            "lastName": "Record",
            "lastNameUnmasked": "Record",
            "middleName": "Fake",
            "nameType": {
              "active": true,
              "code": "PRFR"
            },
            "suppressName": false
          }
        ]
      },
      "active": true,
      "employmentInformation": {
        "employment": [
          {
            "active": true,
            "baseSalaryAmount": "0.00",
            "employeeId": "'$BUID'",
            "employeeStatus": {
              "active": true,
              "code": "3"
            },
            "employeeType": {
              "active": true,
              "code": "9"
            },
            "employmentRecordId": null,
            "entityAffiliation": {
              "active": true,
              "affiliationType": {
                "active": true,
                "code": "0888",
                "employmentAffiliationType": false
              },
              "campusCode": "10",
              "defaultValue": true,
              "entityId": "'$BUID'"
            },
            "entityId": "'$BUID'",
            "primary": true,
            "primaryDepartmentCode": "1202330000"
          }
        ]
      }
    }' \\
    https://$(getHostDomain)/kc/research-common/api/v1/identity/create
EOF
  cat $outfile

  [ "$DRYRUN" == 'true' ] && exit 0

  sh $outfile
}
