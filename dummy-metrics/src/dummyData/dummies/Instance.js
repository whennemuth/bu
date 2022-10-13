import DummySource from '../DummySource'

export default class Instance extends DummySource {
    constructor() {
        super()
    }
    getData(parms) {
        super.getData()        
        console.log("Getting data for " + parms)

        let instances = []
        let ids = ['i-A1B2C3D4', 'i-L7Y4B6D2', 'i-K5J8S2M9', 'i-Q6R0P3L1']
        ids.forEach(id => {
            instances.push(
                {
                    "status": "ACTIVE",
                    "registeredResources": [
                        {
                            "integerValue": 2048,
                            "longValue": 0,
                            "type": "INTEGER",
                            "name": "CPU",
                            "doubleValue": 0.0
                        },
                        {
                            "integerValue": 3955,
                            "longValue": 0,
                            "type": "INTEGER",
                            "name": "MEMORY",
                            "doubleValue": 0.0
                        },
                        {
                            "name": "PORTS",
                            "longValue": 0,
                            "doubleValue": 0.0,
                            "stringSetValue": [
                                "22",
                                "2376",
                                "2375",
                                "51678"
                            ],
                            "type": "STRINGSET",
                            "integerValue": 0
                        }
                    ],
                    "ec2InstanceId": `${id}`,
                    "agentConnected": true,
                    "containerInstanceArn": "arn:aws:ecs:us-west-2:123456789012:container-instance/a1b2c3d4-5678-90ab-cdef-11111EXAMPLE",
                    "pendingTasksCount": 0,
                    "remainingResources": [
                        {
                            "integerValue": 2048,
                            "longValue": 0,
                            "type": "INTEGER",
                            "name": "CPU",
                            "doubleValue": 0.0
                        },
                        {
                            "integerValue": 3955,
                            "longValue": 0,
                            "type": "INTEGER",
                            "name": "MEMORY",
                            "doubleValue": 0.0
                        },
                        {
                            "name": "PORTS",
                            "longValue": 0,
                            "doubleValue": 0.0,
                            "stringSetValue": [
                                "22",
                                "2376",
                                "2375",
                                "51678"
                            ],
                            "type": "STRINGSET",
                            "integerValue": 0
                        }
                    ],
                    "runningTasksCount": 0,
                    "versionInfo": {
                        "agentVersion": "1.0.0",
                        "agentHash": "4023248",
                        "dockerVersion": "DockerVersion: 1.5.0"
                    }
                }
            )
        })
        return instances
    }
}
