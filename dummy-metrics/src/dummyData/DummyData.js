import Cluster from './dummies/Cluster'
import Service from './dummies/Service'
import Instance from './dummies/Instance'
import Task from './dummies/Task'
import Metric from './dummies/Metric'

export default class DummyData {
    constructor(dummyType) {
        this.dummyType = dummyType
    }

    getData(parms) {
        switch (this.dummyType.toLowerCase()) {
            case 'cluster' :
                return (new Cluster()).getData(parms); break;
            case 'service' :
                return (new Service()).getData(parms); break;
            case 'instance' :
                return (new Instance()).getData(parms); break;
            case 'task' :
                return (new Task()).getData(parms); break;
            case 'metric' :
                return (new Metric()).getData(parms); break;
            default :
                return {} ; break;
        }
    }
}
