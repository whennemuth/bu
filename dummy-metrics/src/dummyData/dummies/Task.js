import DummySource from '../DummySource'

export default class Task extends DummySource {
    constructor() {
        super()
    }

    getData(parms) {
        super.getData()        
        console.log("Getting data for " + parms)
        
        return (
            {
                "taskArn": "arn:aws:ecs:us-west-2:123456789012:task/MyCluster/1234567890123456789",
                "clusterArn": "arn:aws:ecs:us-west-2:123456789012:cluster/MyCluster",
                "taskDefinitionArn": "arn:aws:ecs:us-west-2:123456789012:task-definition/sample-fargate:2",
                "overrides": {
                    "containerOverrides": [
                        {
                            "name": "fargate-app"
                        }
                    ]
                },
                "lastStatus": "RUNNING",
                "desiredStatus": "RUNNING",
                "cpu": "256",
                "memory": "512",
                "containers": [
                    {
                        "containerArn": "arn:aws:ecs:us-west-2:123456789012:container/a1b2c3d4-5678-90ab-cdef-11111EXAMPLE",
                        "taskArn": "arn:aws:ecs:us-west-2:123456789012:task/MyCluster/1234567890123456789",
                        "name": "fargate-app",
                        "lastStatus": "RUNNING",
                        "networkBindings": [],
                        "networkInterfaces": [
                            {
                                "attachmentId": "a1b2c3d4-5678-90ab-cdef-22222EXAMPLE",
                                "privateIpv4Address": "10.0.0.4"
                            }
                        ],
                        "healthStatus": "UNKNOWN",
                        "cpu": "0"
                    }
                ],
                "startedBy": "ecs-svc/1234567890123456789",
                "version": 3,
                "connectivity": "CONNECTED",
                "connectivityAt": 1557134016.971,
                "pullStartedAt": 1557134025.379,
                "pullStoppedAt": 1557134033.379,
                "createdAt": 1557134011.644,
                "startedAt": 1557134035.379,
                "group": "service:fargate-service",
                "launchType": "FARGATE",
                "platformVersion": "1.3.0",
                "attachments": [
                    {
                        "id": "a1b2c3d4-5678-90ab-cdef-33333EXAMPLE",
                        "type": "ElasticNetworkInterface",
                        "status": "ATTACHED",
                        "details": [
                            {
                                "name": "subnetId",
                                "value": "subnet-12344321"
                            },
                            {
                                "name": "networkInterfaceId",
                                "value": "eni-12344321"
                            },
                            {
                                "name": "macAddress",
                                "value": "0a:90:09:84:f9:14"
                            },
                            {
                                "name": "privateIPv4Address",
                                "value": "10.0.0.4"
                            }
                        ]
                    }
                ],
                "healthStatus": "UNKNOWN",
                "tags": []
            }
        )        
    }
}
