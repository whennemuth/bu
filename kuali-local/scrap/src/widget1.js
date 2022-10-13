import React from 'react';
import './css/index.css';

class MyClass1 extends React.Component {
    render() {

        return <div>
            <table cellPadding="0" cellSpacing="0">
                <tbody>
                <tr><td>one</td><td>apples</td></tr>
                <tr><td>two</td><td>oranges</td></tr>
                <tr><td>three</td><td>pears</td></tr>
                </tbody>
            </table>
        </div>
    }
}

MyClass1.title = 'Basic React Component';

export default MyClass1
