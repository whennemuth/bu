import React, { Component } from 'react';
import './css/index.css';
import { MetricLookup } from './Lookup';
import JsonToTable from './TabularView'

class Metrics extends Component {

    render() {
        // Clone metricParms once for each metric, adding the metricType to the clone.
        let cpu = Object.assign({}, this.props.metricParms)
        cpu.query = Object.assign({ metricType: 'cpu' }, cpu.query)
        let memory = Object.assign({}, this.props.metricParms)
        memory.query = Object.assign({ metricType: 'memory' }, memory.query)

        return (
            <div className='clusterDummyConfig'>
                
                <Metric metricParms={cpu} />

                <Metric metricParms={memory} />
            </div>
        )
    }
}


class Metric extends Component {

    render() {
        const { metricParms } = this.props
        let data = (new MetricLookup()).getData(metricParms)
        let datapoints = data.Datapoints.map((dp, idx) => {
            let mapified = {}
            mapified[dp.Timestamp] = dp.Average
            return mapified
        })
        
        return(
            <div>
                <div style={{fontWeight:'bold', paddingTop:'5px'}}>{data.Label} Percent</div>
                <JsonToTable json={datapoints} />
            </div>
        )
    }
    
} 

export default Metrics
