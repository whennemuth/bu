import DummySource from '../DummySource'

export default class Cluster extends DummySource {
    constructor() {
        super()
    }
    getData(parms) {
        super.getData()        
        console.log("Getting data for " + parms)

        return (
            {
                "status": "ACTIVE",
                "clusterName": "default",
                "registeredContainerInstancesCount": 0,
                "pendingTasksCount": 0,
                "runningTasksCount": 0,
                "activeServicesCount": 1,
                "clusterArn": "arn:aws:ecs:us-west-2:123456789012:cluster/default",
                "runningEC2TasksCount": 2,
                "pendingEC2TasksCount": 1,
                "activeEC2ServiceCount": 3,
                "drainingEC2ServiceCount": 0
            }
         )
    }
}
