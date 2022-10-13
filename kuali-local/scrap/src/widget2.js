import React from 'react';
import './css/index2.css';
import MyClass1 from './widget1';

class MyClass2 extends React.Component {
    render() {
        return (
            <div>
                <div className="left"><MyClass1 /></div>
                <div className="right"><MyClass1 /></div>
                <div className="right"><MyClass1 /></div>

                <div className="left"><MyClass1 /></div>
                <div className="right"><MyClass1 /></div>
                <div className="right"><MyClass1 /></div>
            </div>
        )
    }
}

MyClass2.title = 'React Component with class component'

export default MyClass2