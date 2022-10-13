import React, { Component } from 'react';
import './css/index.css';

class JsonToTable extends Component {

    getRows(parms) {
        let rows = []
        let obj = parms.json
        if(Array.isArray(parms.obj)) {
            parms.obj.forEach(o => {
                let newparms = parms.clone()
                newparms.obj = o
                newparms.objName = null
                rows.push(this.getRows(newparms))             
            })
            return rows
        }
    
        for(var item in parms.obj) {
            if(typeof parms.obj[item] === 'object') {
                rows.push(
                    <tr>
                        <td colSpan="2" style={{paddingLeft:parms.indentPixels}}>
                            {item}:
                        </td>
                    </tr>
                )
                let newparms = parms.clone()
                newparms.obj = parms.obj[item]
                newparms.objName = item
                newparms.increaseIndent()
                rows.push(this.getRows(newparms))
            }
            else {
                if(typeof parms.obj !== 'object') {
                    rows.push(
                        <tr>
                            <td colSpan="2" style={{paddingLeft:parms.indentPixels}}>
                                {parms.obj}:
                            </td>
                        </tr>
                    )
                    break;
                }
                rows.push(
                    <tr>
                        <td style={{paddingLeft:parms.indentPixels}}>
                            {item}:
                        </td>
                        <td>
                            {parms.obj[item]}
                        </td>
                    </tr>
                )
            }
        }
        return rows
    }

    render() {
        return (
            <table border="0">
                <tbody>
                    {this.getRows(new TableParms(this.props))}
                </tbody>                   
            </table>                
        )
    }
}

class TableParms {
    
    constructor(parms) {
        if(parms.json) {
            this.obj = parms.json
        }
        else {
            this.obj = parms.obj
        }
        this.objName = parms.objName
        this.offset = parms.offset || 0
        this.indentUnitSize = parms.indentUnitSize || 15
        this.padding = parms.padding || 0
    }

    increaseIndent = () => {
        this.offset += 1
    }

    decreaseIndent = () => {
        this.offset -= 1
     }

    get indentPixels() {
        return (this.indentUnitSize * this.offset) + 'px'
    }

    clone() {
        return new TableParms(this)
    }
}

export default JsonToTable