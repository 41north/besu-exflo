/*
 * Copyright (c) 2019 41North.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.4.25 <0.6.0;

contract SelfDestructIntegration {

    constructor() public payable {}

    function pay() public payable {}

    function destroyAndRefundSender() public {
        selfdestruct(msg.sender);
    }

    function destroyAndRefundSelf() public {
        assembly {
            selfdestruct(address)
        }
    }
}

contract SelfDestructDelegatingCallsIntegration {

    SelfDestructIntegration private one;
    SelfDestructIntegration private two;
    SelfDestructIntegration private three;

    constructor() public payable {
        require(msg.value >= 10 ether);

        one = (new SelfDestructIntegration).value(1 ether)();
        two = (new SelfDestructIntegration).value(2 ether)();
        three = (new SelfDestructIntegration).value(3 ether)();
    }

    function sendEtherToContractAfterSelfReferencingDestroy() public payable {
        one.destroyAndRefundSelf();  // 1 ether is 'destroyed'
        one.pay.value(2 ether)();    // send another 1 ether which also gets destroyed
    }

    function cascadingDestroyAndRefundSender() public {
        one.destroyAndRefundSender();
        two.destroyAndRefundSender();
        three.destroyAndRefundSender();

        // this contract should now hold 6 ether which we refund to sender
        selfdestruct(msg.sender);
    }

    function createSelfDestroyingContractsAndSelfDestructItself() public payable {
        SelfDestructIntegration first = (new SelfDestructIntegration).value(10 wei)();
        SelfDestructIntegration second = (new SelfDestructIntegration).value(20 wei)();
        SelfDestructIntegration third = (new SelfDestructIntegration).value(30 wei)();

        first.destroyAndRefundSender();
        second.destroyAndRefundSelf();
        third.destroyAndRefundSender();

        selfdestruct(msg.sender);
    }
}
