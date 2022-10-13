import React, { Component } from 'react';
import './css/index.css';
import { Subscription } from 'rxjs';

class DummyMetrics extends Component {

    render() {
        return <DummyMetric />
    }
}

class DummyMetric extends Component {

    /**
     * The user has typed a character that hasn't appeared in the textbox yet and can be cancelled 
     * if it doesn't satisfy this validation test.
     */ 
    validatePercentChar = (event) => {
        let char = event.nativeEvent.key;
        let chars = event.nativeEvent.srcElement.value;
        if( ! /[\d\.]/.test(char)) {
            event.preventDefault();
        }
        if(/\./.test(this.lastCPUPercent) && char=='.') {
            event.preventDefault();
        }
        this.lastCPUPercent = chars;
    }

    /**
     * The user has typed a character and the textbox shows a new value as a result.
     * If the new value of the textbox does not satisfy this validation test, the value
     * of the textbox if altered/trimmed so that it does.
     */
    validatePercent = (event) => {
        let txt = event.nativeEvent.srcElement;
        let val = txt.value;
        let valid = false;
        
        if(val == '100' || /^100\.\d{0,2}$/.test(val)) {
            valid = true;
        }
        else if(/^\d{0,2}\.\d{0,2}$/.test(val)) {
            valid = true;
        }
        else if(/^\d{1,2}$/.test(val)) {
            valid = true;
        }
        else if(val == '') {
            valid = true;
        }
        
        if(valid) {
            this.lastCPUPercent = val;
        }
        else {
            txt.value = this.lastCPUPercent;
        }       
    }

    /**
     * Some values a textbox might have are invalid but were not cancelled by the validatePercent function because
     * the user might not have been done typing. In this case, we know the user is finished typing because the
     * onblur event triggered this function. The textbox is altered/trimmed to satisfy the validation/format test below.
     */
    cleanupPercent = (event) => {
        let txt = event.nativeEvent.srcElement;
        if(txt == undefined) return;
        let val = txt.value;
        if(val == '.') txt.value = '';
        else if(/^0*\.0*$/.test(val)) txt.value = '0.00';
        else if(/^\d*\.$/.test(val)) txt.value = txt.value + '00';
        else if(/^\d*\.\d$/.test(val)) txt.value = txt.value + '0';
        else if(val.substr(0,2) == '0.') txt.value = val.substr(1);
        else if(val.substr(0,1) == '.') txt.value = val.substr(1);
        else if(val.substr(0,1) == '.') txt.value = '0' + val;
        else if(/^00/.test(val)) txt.value = val.substr(1);
    }

    validateNumberChar = (event) => {
        let char = event.nativeEvent.key;
        let chars = event.nativeEvent.srcElement.value;
        if( ! /\d/.test(char)) {
            event.preventDefault();
        }
    }

    validateNumber = (event) => {
        let txt = event.nativeEvent.srcElement;
        let val = txt.value;
        let valid = false;

        if(val.substr(0,1) == '0') {
            txt.value = txt.value.substr(1)
        }
    }

    render() {

        return (
            <div>
                <div style={{fontWeight:"bold", fontSize:"12px", paddingBottom:"12px"}}>
                    Make this EC2 instance report lies to cloudwatch about its resource utilization metrics.<br></br>
                    It should affect the average for the cluster.
                </div>
                <div style={{paddingLeft:"16px"}}>
                    <table style={{borderWidth:0}} >
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                CPU Utilization Percent:
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text' 
                                    id="dummyCPU" name="dummyCPU"
                                    onKeyPress={this.validatePercentChar} 
                                    onChange={this.validatePercent} 
                                    onBlur={this.cleanupPercent} />
                            </td>
                        </tr>
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                Memory Utilization Percent:
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text' 
                                    id="dummyMemory" name="dummyMemory"
                                    onKeyPress={this.validatePercentChar} 
                                    onChange={this.validatePercent} 
                                    onBlur={this.cleanupPercent} />
                            </td>
                        </tr>
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                Disk Utilization Percent
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text'
                                    id="dummyDisk" name="dummyDisk"
                                    onKeyPress={this.validatePercentChar} 
                                    onChange={this.validatePercent} 
                                    onBlur={this.cleanupPercent} />
                            </td>
                        </tr>
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                IO Wait Percent
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text'
                                    id="dummyIoWait" name="dummyIoWait"
                                    onKeyPress={this.validatePercentChar} 
                                    onChange={this.validatePercent} 
                                    onBlur={this.cleanupPercent} />
                            </td>
                        </tr>
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                Number of TCP Connections
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text' 
                                    id="dummyTcp" name="dummyTcp"
                                    onKeyPress={this.validateNumberChar} 
                                    onChange={this.validateNumber} />
                            </td>
                        </tr>
                        <tr>
                            <td align="right" style={{paddingTop:"8px"}}>
                                Number of Users
                            </td>
                            <td style={{paddingTop:"8px"}}>
                                <input type='text' 
                                    id="dummyUsers" name="dummyUsers"
                                    onKeyPress={this.validateNumberChar} 
                                    onChange={this.validateNumber} />
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
        )
    }
}

export default DummyMetrics 