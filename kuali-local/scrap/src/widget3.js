import React, { Component } from 'react';
import './css/index2.css';

const Cell = ({clsname}) => {
    return (
        <div className={clsname}>
            <table cellPadding="0" cellSpacing="0">
                <tbody>
                <tr><td>one</td><td>apples</td></tr>
                <tr><td>two</td><td>oranges</td></tr>
                <tr><td>three</td><td>pears</td></tr>
                </tbody>
            </table>
        </div>
     )
}

class MyClass3 extends Component {
    render() {
        return <div>
            <Cell clsname="left" /> 
            <Cell clsname="right"/>
            <Cell clsname="right"/> 

            <Cell clsname="left"/>
            <Cell clsname="right"/> 
            <Cell clsname="right"/>
        </div>
    }
}

MyClass3.title = 'React Component with simple component as class component'

export default MyClass3;