import React, { Component } from 'react';
import './css/index2.css';

const TableHeader = (props) => { 
    const cells = props.tableColumns.map((cell, index) => {
        return <th key={index}>{cell}</th>
    })
    return (
        <thead>
            <tr>
                {cells}
            </tr>
        </thead>
    )
}

const TableBody = (props) => { 
    const rows = props.table.map((row, index) => {
        return (
            <tr key={index}>
                <td>{row.number}</td>
                <td>{row.fruit}</td>
            </tr>
        )
    })
    return <tbody>{rows}</tbody> 
}

class Table extends Component {
    render() {
        const { table, tableColumns, tableIndex } = this.props;
        const clsName = tableIndex % 3 === 0 ? 'left' : 'right';

        return (           
            <div className={clsName}>
                <table cellPadding="0" cellSpacing="0">
                    <TableHeader tableColumns={tableColumns} />
                    <TableBody table={table} tableIndex={tableIndex}/>
                </table>
            </div>
        )
    }
}

class MyClass4 extends Component {

    tableFields =  [ 'Number', 'Fruit'];
    tableArray = [
        [ 
            { number : 1, fruit : 'apples' },
            { number : 2, fruit : 'oranges' }, 
            { number : 3, fruit : 'pears' }
        ],
        [ 
            { number : 4, fruit : 'mangos' },
            { number : 5, fruit : 'banannas' }, 
            { number : 6, fruit : 'apricots' }
        ],
        [ 
            { number : 7, fruit : 'kiwis' },
            { number : 8, fruit : 'strawberrys' }, 
            { number : 9, fruit : 'blackberrys' }
        ],
        [ 
            { number : 10, fruit : 'cherries' },
            { number : 11, fruit : 'grapes' }, 
            { number : 12, fruit : 'canteloupes' }
        ],
        [ 
            { number : 13, fruit : 'watermelons' },
            { number : 14, fruit : 'limes' }, 
            { number : 15, fruit : 'raspberries' }
        ],
        [ 
            { number : 16, fruit : 'blueberries' },
            { number : 17, fruit : 'grapefruits' }, 
            { number : 18, fruit : 'Lemons' }
        ]
    ]

    getTableFields = function (){
        return this.tableFields;
    }

    getTableArray = () => {
        return this.tableArray;
    }

    render() {
        const tables = this.tableArray.map((table, index) => {
            return (
                <Table 
                    key={index} 
                    table={table} 
                    tableColumns={this.tableFields} 
                    tableIndex={index} />
            )
        })
        return <div>{tables}</div>
    }
}

MyClass4.title = "React Component with props data"

export default MyClass4;

export {  MyClass4, Table, TableHeader, TableBody };