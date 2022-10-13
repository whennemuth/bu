import React, { Component } from 'react';
import './css/tabs.css';

class Tab extends Component {
    render() {
        const {tabdef, index, zindex, switchTabs} = this.props;
        const active = tabdef.active===undefined?false:tabdef.active;
        const style = {
            left : ('-'+(index*20)+'px'),
            top : (active ? '-10px' : '0px'),
            zIndex : zindex
        }
         
        return (
            <div 
                className="tab" 
                style={style} 
                onClick={() => { switchTabs(index) }} 
                >{tabdef.label}</div>
        )
    }
}


class TabStrip extends Component {
    
    constructor(props) {
        super(props)
        this.state = { tabdefs: this.props.defaultTabdefs}
    }

    switchTabs = (index) => {
        let refreshedTabdefs = this.props.refreshTabdefs(index, true)
        this.stateChangeTriggeredRender = true
        this.setState({ tabdefs : refreshedTabdefs })
    }

    render() {
        const stateChanged = this.stateChangeTriggeredRender
        this.stateChangeTriggeredRender = false
        if(!stateChanged) {
            let idx = this.state.tabdefs.findIndex(tabdef => {
                return tabdef.active
            })
            this.state.tabdefs = this.props.refreshTabdefs(idx, false)
        }
        const tabs = this.state.tabdefs.map((tabdef, index, defs) => {
            return (
                <Tab 
                    key={index} 
                    tabdef={tabdef}
                    index={index}
                    zindex={tabdef.active ? defs.length+1 : defs.length-index}
                    switchTabs={this.switchTabs} />
            )
        })
        
        tabs.push(<div 
            key={this.state.tabdefs.length} 
            style={{zIndex:this.state.tabdefs.length}} 
            className='tabshield'></div>);

        tabs.push(<div 
            key={this.state.tabdefs.length+1} 
            style={{zIndex:this.state.tabdefs.length+2}} 
            className='tabshield tabshieldCover'></div>);

            // return <div style={{width:'1200px'}}>{tabs}</div>
            return <div>{tabs}</div>
        }
}

export default TabStrip