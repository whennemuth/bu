class upper {
    constructor(myprop = 'hello') {
        this._myprop = myprop;
    }
    speak() {
        this.talk();
    }
}
class lower1 extends upper {
    constructor(myprop) {
        super(myprop);
        if(lower1.sum == undefined) {
            lower1.sum = 0;
        }
        lower1.sum++;
    }
    talk() {
        console.log(this._myprop);
    }
    static get SUM() {
        return lower1.sum;
    }
}

let subclass1 = new lower1();
console.log(lower1.SUM);
let subclass2 = new lower1('goodbye');
console.log(lower1.SUM);
subclass1.speak();
subclass2.speak();
console.log(subclass1._myprop);
console.log(subclass2._myprop);
