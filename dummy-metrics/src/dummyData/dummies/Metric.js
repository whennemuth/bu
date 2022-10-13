import DummySource from '../DummySource'

export default class Metric extends DummySource {
    constructor() {
        super()
    }

    getData(query) {
        super.getData()        
        console.log("Getting data for " + query)

        switch(query.metricType) {
            case 'cpu': 
                return this.getCPU();
            case 'memory': 
                return this.getMemory();
        }
    }

    getCPU() {
        return (
            {
                awscli: "aws cloudwatch get-metric-statistics --namespace AWS/EC2 --metric-name CPUUtilization \
                --dimensions Name=InstanceId,Value=i-065f00b8c7b08c324 --statistics Average \
                --start-time 2019-07-11T00:00:00 --end-time 2019-07-11T00:12:00 --period 360",

                awscli2: "aws cloudwatch get-metric-statistics --namespace AWS/EC2 --metric-name DiskWriteBytes \
                --dimensions Name=AutoScalingGroupName,Value=my-asg --statistics \"Sum\" \"SampleCount\" \
                --start-time 2019-07-11T00:00:00 --end-time 2019-07-11T00:12:00 --period 360",

                "Datapoints": [
                    {
                        "Timestamp": "2019-07-11T22:18:00Z",
                        "Average": 3.1833333333333202,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T11:42:00Z",
                        "Average": 3.31158423636195,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T14:06:00Z",
                        "Average": 3.0833333333333344,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T20:00:00Z",
                        "Average": 3.379577660461192,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T12:54:00Z",
                        "Average": 3.5718116143373138,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T18:48:00Z",
                        "Average": 32.97465499675842,
                        "Unit": "Percent"
                    },
                    {
                        "Timestamp": "2019-07-11T16:12:00Z",
                        "Average": 3.263165694174319,
                        "Unit": "Percent"
                    }
                ],
                "Label": "CPUUtilization"
            }
        )
    }

    getMemory() {

        return (
            {
                awscli: "aws cloudwatch get-metric-statistics --namespace AWS/ECS --metric-name MemoryUtilization \
                --dimensions Name=ClusterName,Value=KualiResearchProject Name=ServiceName,Value=tomcat-webserver-service --statistics Average \
                --start-time 2019-07-07T00:00:00 --end-time 2019-07-12T23:18:00 --period 360 --region us-east-1",

                "Datapoints": [
                {
                    "Timestamp": "2019-07-10T13:12:00Z",
                    "Average": 11.03515625,
                    "Unit": "Percent"
                },
                {
                    "Timestamp": "2019-07-11T23:24:00Z",
                    "Average": 10.9619140625,
                    "Unit": "Percent"
                },
                {
                    "Timestamp": "2019-07-09T07:42:00Z",
                    "Average": 11.059705946180555,
                    "Unit": "Percent"
                },
                {
                    "Timestamp": "2019-07-10T17:54:00Z",
                    "Average": 11.03515625,
                    "Unit": "Percent"
                }
                ],
                "Label": "MemoryUtilization"
        
            }
        )
    }
}
