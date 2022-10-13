import React, { Component } from 'react';
import Services from './Service'
import Instances from './Instance'
import { ClusterLookup } from './Lookup'
import JsonToTable from './TabularView'
import './css/index.css';

class Cluster extends Component {

    render() {
        return (
            <div className="cluster">
                <div className="clusterHeading">
                    My Cluster
                </div>

                <div className="pageLeft">
                    <ClusterInfo />
                    <Services />
                </div>

                <div className="pageRight">
                    <Instances />
                </div>
            </div>
        )
    }
}

class ClusterInfo extends Component {

    render() {
        let json = (new ClusterLookup()).getData({
            dummy: true,
            query: null
        })

        return (
            <div className="clusterinfo">
                <JsonToTable json={json} />
            </div>
        )
    }
}

export default Cluster