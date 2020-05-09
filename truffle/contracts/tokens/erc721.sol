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

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/token/ERC721/ERC721Metadata.sol";
import "@openzeppelin/contracts/token/ERC721/ERC721Enumerable.sol";
import "@openzeppelin/contracts/token/ERC721/ERC721Full.sol";

contract MinimalERC721 is ERC721 {
    constructor() ERC721() public {
    }
}

contract MetadataERC721 is ERC721Metadata {
    constructor() ERC721Metadata("MetadataERC721", "E721") public {
    }
}

contract FullERC721 is ERC721Full {
    constructor() ERC721Full("ERC721 Full", "E721") public {
    }
}

contract WeirdNameCharsERC721 is ERC721Full {
    constructor() ERC721Full("��V��RvvvW�WR����� ", "�WR") public {
    }
}

contract InvalidERC721 {
}
