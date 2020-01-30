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

contract CreateIntegration {

    constructor() public payable {}

    function createDummyContract() public payable {
        (new DummyOwnerContract).value(msg.value)(msg.sender);
    }

    function createDummyOutOfGasContract() public payable {
        new DummyOutOfGasContract();
    }

    function createDummyBadOpCodeContract() public payable {
        new DummyBadOpCodeContract();
    }

    function createDummyRevertContract() public payable {
        new DummyRevertContract();
    }

    function createNestedDummyContracts() public payable {
        CreateNestedIntegration c = (new CreateNestedIntegration).value(msg.value)();
        c.createDummyOwnerContract();
    }

}

contract CreateNestedIntegration {

    constructor() public payable {
        require(msg.value >= 3 ether);
        (new DummyContract).value(0 ether)();
        (new DummyContract).value(1 ether)();
        (new DummyContract).value(2 ether)();
    }

    function createDummyOwnerContract() public payable {
        (new DummyOwnerContract).value(25 wei)(msg.sender);
    }

}
