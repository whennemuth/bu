import React, { Component } from 'react';
import ReactDOM from 'react-dom';
//import './index.css';
import MyClass1 from './widget1';
import MyClass2 from './widget2';
import MyClass3 from './widget3';
import MyClass4 from './widget4';
import MyClass5 from './widget5';
import TabStrip from './Tab'
import * as serviceWorker from './serviceWorker';

const Heading = ({title}) => { return (<div className="separator">{title}</div>) };

class Wrapper extends Component {

    state = {
        tabdefs : [
            { type: MyClass1, label : MyClass1.name, active:true},
            { type: MyClass2, label : MyClass2.name },
            { type: MyClass3, label : MyClass3.name },
            { type: MyClass4, label : MyClass4.name },
            { type: MyClass5, label : MyClass5.name }
        ]
    }

    refreshTabs = (index) => {
        let defs = this.state.tabdefs.map((def, idx) => {
            return {
                label : def.label,
                type : def.type,
                active : (idx === index)
            }
        })
        this.setState({tabdefs: defs})
        return defs
    }

    render() {
        let currentTab = null;
        this.state.tabdefs.forEach(tabdef => {
            if(tabdef.active) {
                currentTab = {
                    heading: (<Heading title={tabdef.type.title} />),
                    content: () => {
                        switch(tabdef.label) {
                            case MyClass2.name :
                                return <div><MyClass2 /></div>
                            case MyClass3.name :
                                return <MyClass3 />
                            case MyClass4.name :
                                return <MyClass4 />
                            case MyClass5.name :
                                return <MyClass5 />
                            default : 
                                return <div className="left"><MyClass1 /></div>
                        }
                    }
                }
            }
        });
        
        return (
            <div>
                <TabStrip defaultTabdefs={this.state.tabdefs} refreshTabdefs={this.refreshTabs} />

                { currentTab.heading }

                { currentTab.content() }
                
            </div>
        )        
    }
}

ReactDOM.render([<Wrapper />], document.getElementById('root'));

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
