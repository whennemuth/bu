import React, { Component } from 'react';
import './css/index.css';
import { ServiceLookup } from './Lookup'
import TabStrip from './Tab'
import JsonToTable from './TabularView'


const Heading = ({title}) => { return (<div className="separator">{title}</div>) };

class Services extends Component {

    constructor(props) {
        super(props)
        this.state = { tabdefs: []} 
        let services = (new ServiceLookup()).getData({
            dummy: true,
            query: null
        })
        
        let active = true;
        services.forEach(svc => {
            this.state.tabdefs.push({ 
                data:svc, 
                label:this.getTaskName(svc.taskDefinition),
                active:active 
            })
            active = false
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

    getTaskName(taskDefinition) {
        return taskDefinition.split('/')[1].split(':')[0]
    }

    render() {
        let currentTab = null;
        this.state.tabdefs.forEach(tabdef => {
            if(tabdef.active) {
                currentTab = {
                    heading: (<Heading title={this.getTaskName(tabdef.data.taskDefinition)} />),
                    content: () => {
                        return <Service data={tabdef.data} />
                    }
                }
            }
        });
        
        return (
            <div className='service'>
                <TabStrip 
                    defaultTabdefs={this.state.tabdefs} 
                    refreshTabdefs={this.switchTabs} 
                    getTabLabel={this.getTabLabel} />

                {/* { currentTab.heading } */}

                { currentTab.content() }
            </div>
        )        
    }
}

class Service extends Component {
    
    render() {
        let json = this.props.data
        return (
            <div className="serviceinfo">
                <JsonToTable json={json} />                
            </div>
        )
    }
}


export default Services