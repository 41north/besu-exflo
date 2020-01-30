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

contract DummyContract {

    address public owner;

    constructor() public payable {
        owner = msg.sender;
    }

    function() external payable {}
}

contract DummyOwnerContract {
    address public owner;

    constructor(address _owner) public payable {
        owner = _owner;
    }

    function() external payable {}
}

contract DummyOutOfGasContract {

    constructor() public payable {
        while (true) {
            assembly {
                // Added this opcode to consume gas faster as in dev environment gas limits are almost infinite
                sstore(0x0, 0x0)
            }
        }
    }
}

contract DummyBadOpCodeContract {

    constructor() public payable {
        assembly {
            pop(keccak256(0x800000000000000000000000, 0x80000000000000000000000000000))
        }
    }
}

contract DummyRevertContract {

    constructor() public payable {
        assembly {
            revert(0x0, 0x0)
        }
    }
}
