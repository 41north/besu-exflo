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

contract CreateOpCodeUnit {

    constructor() public payable {
        require(msg.value >= 1 ether);
    }

    function createWithValidCodeAndWithEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)
            mstore(ptr, 0x3000000000000000000000000000000000000000000000000000000000000000)
            pop(create(100000000000, ptr, 0x20))
        }
    }

    function createWithValidCodeAndWithoutEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)
            mstore(ptr, 0x3000000000000000000000000000000000000000000000000000000000000000)
            pop(create(0, ptr, 0x20))
        }
    }

    function createWithoutCodeAndWithoutEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)
            mstore(ptr, 0x0000000000000000000000000000000000000000000000000000000000000000)
            pop(create(0, ptr, 0x20))
        }
    }

    function createWithInvalidCodeAndWithoutEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)
            mstore(ptr, 0xfc00000000000000000000000000000000000000000000000000000000000000)
            pop(create(0, ptr, 0x20))
        }
    }

    function createTwoContractsWithValidCodeAndWithEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)

            mstore(ptr, 0x3000000000000000000000000000000000000000000000000000000000000000)
            pop(create(100000000000, ptr, 0x20))

            mstore(ptr, 0x3200000000000000000000000000000000000000000000000000000000000000)
            pop(create(100000000000, ptr, 0x20))
        }
    }

    function createTwoContractsWithValidAndInvalidCodeAndWithEtherAssociated() public {
        assembly {
            let ptr := mload(0x40)

            mstore(ptr, 0x3000000000000000000000000000000000000000000000000000000000000000)
            pop(create(100000000000, ptr, 0x20))

            mstore(ptr, 0xfc00000000000000000000000000000000000000000000000000000000000000)
            pop(create(100000000000, ptr, 0x20))
        }
    }

}
