import DummyData from './dummyData/DummyData';

class Lookup {
    constructor(lookupType) {
        console.log('Performing ' + lookupType + ' lookup...')
        this.lookupType = lookupType
    }
    getData(lookupParms) {
        if(lookupParms.dummy) {
            return (new DummyData(this.lookupType)).getData(lookupParms.query)
        }
        
        return (
            { /* Call aws cli from here */ }
        )
    }
}

class ClusterLookup extends Lookup {
    constructor() {
        super('cluster')
    }
    getData(lookupParms) {
        return super.getData(lookupParms)
    }
}

class ServiceLookup extends Lookup {
    constructor() {
        super('service')
    }
    getData(lookupParms) {
        return super.getData(lookupParms)
    }
}

class ContainerInstanceLookup extends Lookup {
    constructor() {
        super('instance')
    }
    getData(lookupParms) {
        return super.getData(lookupParms)
    }
}

class TaskLookup extends Lookup {
    constructor() {
        super('task')
    }
    getData(lookupParms) {
        return super.getData(lookupParms)
    }
}

class MetricLookup extends Lookup {
    constructor() {
        super('metric')
    }
    getData(lookupParms) {
        return super.getData(lookupParms)
    }    
}

export {
    ClusterLookup,
    ServiceLookup,
    ContainerInstanceLookup,
    TaskLookup,
    MetricLookup
}