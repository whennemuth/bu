import DummySource from '../DummySource'

export default class Service extends DummySource {
    constructor() {
        super()
    }

    getData(parms) {
        super.getData()        
        console.log("Getting data for " + parms)

        let services = []
        let servicenames = [ 'kuali', 'core', 'coi', 'dashboard' ]
        servicenames.forEach(taskname => {
            services.push(
                {
                    "status": "ACTIVE",
                    "taskDefinition": `arn:aws:ecs:us-west-2:123456789012:task-definition/${taskname}:1`,
                    "pendingCount": 0,
                    "loadBalancers": [],
                    "desiredCount": 10,
                    "createdAt": 1466801808.595,
                    "serviceName": "my-http-service",
                    "clusterArn": "arn:aws:ecs:us-west-2:123456789012:cluster/default",
                    "serviceArn": "arn:aws:ecs:us-west-2:123456789012:service/my-http-service",
                    "deployments": [
                        {
                            "status": "PRIMARY",
                            "pendingCount": 0,
                            "createdAt": 1466801808.595,
                            "desiredCount": 10,
                            "taskDefinition": `arn:aws:ecs:us-west-2:123456789012:task-definition/${taskname}:1`,
                            "updatedAt": 1428326312.703,
                            "id": "ecs-svc/1234567890123456789",
                            "runningCount": 10
                        }
                    ],
                    "events": [
                        {
                            "message": "(service my-http-service) has reached a steady state.",
                            "id": "a1b2c3d4-5678-90ab-cdef-11111EXAMPLE",
                            "createdAt": 1466801812.435
                        }
                    ],
                    "runningCount": 10
                }
            )
        });

        return services
    }
}
