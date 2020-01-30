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

pragma solidity >=0.4.21 <0.6.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20Detailed.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20Capped.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20Burnable.sol";
import "@openzeppelin/contracts/token/ERC20/ERC20Pausable.sol";

contract InvalidERC20 {
    constructor(uint256 value) public {
    }
}

contract MinimalERC20 is ERC20 {
    constructor(uint256 initialSupply) ERC20() public {
        _mint(msg.sender, initialSupply);
    }
}

contract DetailedERC20 is ERC20, ERC20Detailed("ERC20 Detailed", "E2D", 9) {

    constructor(uint256 initialSupply) public {
        _mint(msg.sender, initialSupply);
    }

}

contract WeirdNameCharsERC20 is ERC20, ERC20Detailed {
    constructor(uint256 initialSupply) ERC20Detailed("��V��RvvvW�WR����� ", "ygunnnnn", 0) public {
        _mint(msg.sender, initialSupply);
    }
}

contract CappedERC20 is ERC20Capped {
    constructor(uint256 initialSupply) ERC20Capped(10) public {
        _mint(msg.sender, initialSupply);
    }
}
