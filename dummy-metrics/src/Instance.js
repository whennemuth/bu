import React, { Component } from 'react';
import './css/index.css';
import { ContainerInstanceLookup } from './Lookup'
import TabStrip from './Tab'
import JsonToTable from './TabularView'
import Metrics from './Metric'
import DummyMetrics from './DummyMetrics'
import { tsImportEqualsDeclaration } from '@babel/types';

const Heading = ({title}) => { return (<div className="separator">{title}</div>) };

class Instances extends Component {

    constructor(props) {
        super(props)
        this.state = { tabdefs: []} 
        let instances = (new ContainerInstanceLookup()).getData({
            dummy: true,
            query: null
        })
    
        // The first tab represents all instances in the cluster collectively. Each remaining tab represents an individual instance.
        this.state.tabdefs.push({
            data:"cluster-config",
            label:"CLUSTER",
            active:true
        })

        instances.forEach(inst => {
            this.state.tabdefs.push({ 
                data:inst, 
                label:inst.ec2InstanceId,
                active:false 
            })
        });
    }

    switchTabs = (index, refreshState) => {
        let defs = this.state.tabdefs.map((def, idx) => {
            return {
                data : def.data,
                label : def.label,
                active : (idx === index)
            }
        })
        if(refreshState) {
            this.setState({tabdefs: defs})
        }
        return defs
    }

    getData = () => {
        let activeTabdef = this.state.tabdefs.find(tabdef => {
            if(tabdef.active) {
                return tabdef
            }
        })
        // All tabs except for the first are for container instance information
        return activeTabdef.data == "cluster-config" ? "CLUSTER" : activeTabdef.data
    }

    render() {
        let currentTab = null;
        this.state.tabdefs.forEach(tabdef => {
            if(tabdef.active) {
                let data = this.getData()
                let title = data == "CLUSTER" ?
                    'Dummy Metrics for the Cluster' :
                    "Container Instance ID: " + tabdef.data.ec2InstanceId
                
                currentTab = {
                    heading: (<Heading title={title} />),
                    content: () => {
                        return <Instance getData={this.getData} />
                    }
                }
            }
        });
        
        return (
            <div className='instance'>
                <TabStrip 
                    defaultTabdefs={this.state.tabdefs} 
                    refreshTabdefs={this.switchTabs} 
                    getTabLabel={this.getTabLabel} />

                { currentTab.heading } 

                { currentTab.content() }
            </div>
        )        
    }
}

// TODO: Fix problem where content flips down to the bottom of the screen when you reduce the width of the screen  
class Instance extends Component {

    constructor(props) {
        super(props)
        this.state = this.getState([])
    }

    getState = (defs) => {
        if(defs == undefined) {
            defs = this.state.tabdefs
        }
        return {
            parentData: this.props.getData(),
            tabdefs: defs
        }
    }

    loadData = () => {

        // Find out which is the active tab before the tabdefs get flushed.
        let activeTabIdx = this.state.tabdefs.findIndex(tabdef => {
            return tabdef.active
        })
        if(activeTabIdx == -1) {
            activeTabIdx = 0
        }

        // A different instance may have been clicked, which means the instance state would have changed, so get it again, and empty tabdefs.
        this.state = this.getState([])
 
        // Reconstruct the tabdefs
        if(this.state.parentData == "CLUSTER") {
            this.state.tabdefs.push({
                data: "CLUSTER METADATA",
                label: "Cluster Metadata",
                active: activeTabIdx==0
            })
    
            this.state.tabdefs.push({
                data : {
                    dummy: true,
                    query: {
                        scope: 'cluster',
                        filter: {
                            clusterId: 'TODO: get cluster id/arn'
                        }
                    }
                },
                label : "Cluster metrics",
                active: activeTabIdx==1
            })
        }
        else {
            this.state.tabdefs.push({
                data: this.state.parentData,
                label: "Metadata",
                active: activeTabIdx==0
            })

            this.state.tabdefs.push({
                data: { // This will get passed in as <Metric metricParms={data} /> It's not really content
                    dummy: true,
                    query: {
                        scope: 'instance',
                        filter: {
                            instanceId: this.state.parentData.ec2InstanceId
                        }
                    }
                },
                label: "Metrics",
                active: activeTabIdx==1
            })
        }

        this.state.tabdefs.push({
            data: "Dummy metrics content here",
            label: "Dummy Metrics",
            active: activeTabIdx==2
        })
    }

    switchTabs = (index, refreshState) => {
        let defs = this.state.tabdefs.map((def, idx) => {
            return {
                data : def.data,
                label : def.label,
                active : (idx === index)
            }
        })
        if(refreshState) {
            this.setState(this.getState(defs))
        }
        return defs
    }

    render() {

        this.loadData()

        function tabdefUtil (tabdef) {
            return {
                tabdef: tabdef,
                isMetaData: () => {
                    return /meta/i.test(tabdef.label)
                },
                isMetricData: () => {
                    return /metric/i.test(tabdef.label) && ! /dummy/i.test(tabdef.label)
                },
                isDummyData: () => {
                    return /dummy/i.test(tabdef.label)
                }
            }
        }

        let currentTab = null;
        this.state.tabdefs.forEach(tabdef => {
            if(tabdef.active) {
                currentTab = {
                    heading: (<Heading title={tabdef.label} />),
                    content: () => {
                        let tabutil = new tabdefUtil(tabdef)
                        if(tabutil.isMetaData()) {
                            return <JsonToTable json={tabdef.data} />
                        }
                        else if(tabutil.isMetricData()) {
                            return <Metrics metricParms={tabdef.data} />
                        }
                        else if(tabutil.isDummyData()) {
                            return <DummyMetrics />
                        }
                        
                    }
                }
            }
        });
         
        return (
            <div style={{paddingLeft:20, paddingTop:30}}>
                <TabStrip 
                    defaultTabdefs={this.state.tabdefs} 
                    refreshTabdefs={this.switchTabs} 
                    getTabLabel={this.getTabLabel} />

                {/* { currentTab.heading }  */}

                <div className="instanceinfo">
                    { currentTab.content() }
                </div>
            </div>
        )
    }
}

export default Instances