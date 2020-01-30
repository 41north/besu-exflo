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

import "../dummy.sol";

contract CallEtherSenderIntegration {

    DummyContract public receiver = new DummyContract();

    constructor() public payable {}

    function sendEther(uint amount) public payable {
        require(address(receiver).send(amount));
    }

    function callValueEther(uint amount) public payable {
        (bool success,) = address(receiver).call.value(amount).gas(35000)("");
        if (!success) {
            revert('Call to receiver failed.');
        }
    }

    function transferEther(uint _amount) public payable {
        address(receiver).transfer(_amount);
    }

}

contract CallNestedEtherSenderIntegration {

    CallEtherSenderIntegration private sender = new CallEtherSenderIntegration();

    constructor() public payable {}

    function nestedSendEther(uint amount) public payable {
        (sender.sendEther).value(msg.value)(amount);
    }

    function nestedTransferEther(uint amount) public payable {
        (sender.transferEther).value(msg.value)(amount);
    }

}
