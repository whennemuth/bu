import React, { Component } from 'react';
import './css/index2.css';
import { MyClass4, TableHeader, TableBody } from './widget4.js';

class Table extends Component {
    render() {
        const { table, tableColumns, tableIndex, removeTable, addFruit } = this.props;
        const clsName = tableIndex % 3 === 0 ? 'left' : 'right';

        return (           
            <div className={clsName}>
                <table cellPadding="0" cellSpacing="0">
                    <TableHeader tableColumns={tableColumns} />
                    <TableBody table={table} tableIndex={tableIndex} />
                    <tbody>
                        <tr>
                            <td colSpan={tableColumns.length}>
                                <button onClick={() => removeTable(tableIndex)}>Delete</button>
                                <button onClick={() => addFruit(tableIndex)}>Add Fruit</button>
                            </td>
                        </tr>
                    </tbody>
                </table>               
            </div>
        )
    }
}

class TextArea extends Component {
    render() {
        const {view} = this.props
        return (
            <textarea 
                readOnly={true}
                value={JSON.stringify(view, null, ' ')} 
                style={{float:'left', clear:'both'}} 
                cols='100' 
                rows='30' />
        )
    }
}

class MyClass5 extends Component {

    state = {
        tableArray : (new MyClass4()).getTableArray(),
        tableFields : (new MyClass4()).getTableFields(),
        nextNumber : function() {
            let max=0
            this.tableArray.forEach(table => {
                table.forEach(row => {
                    if(row.number > max) {
                        max = row.number;
                    }
                })
            });
            return ++max;
        }
    }

    removeTable = (index) => {
        const { tableArray } = this.state
        this.setState({
            tableArray : tableArray.filter((table, idx) => {
                return idx !== index;
            })
        })
    }

    addFruit = (index) => {
        const { tableArray, tableFields } = this.state
        const fruit = window.prompt('Enter the name of the fruit')
        this.setState({
            tableFields : tableFields,
            tableArray : tableArray.map((table, idx) => {
                if( idx === index) {                   
                    return table.concat([{number:this.state.nextNumber(), fruit:fruit}])
                }
                return table
            })
        })
    }

    render() {
        const tableFields = this.state.tableFields
        const tableArray = this.state.tableArray
        const tables = tableArray.map((table, index) => {
            const tbl = (
                <Table 
                    key={index} 
                    table={table} 
                    tableColumns={tableFields} 
                    tableIndex={index} 
                    removeTable={this.removeTable}
                    addFruit={this.addFruit} />
            )
            
            return tbl;
        })
        return (
            <div>
                {tables}
                <TextArea view={this.state} />
            </div>
        )
    }
}

MyClass5.title = "React Component with state data"

export default MyClass5;
