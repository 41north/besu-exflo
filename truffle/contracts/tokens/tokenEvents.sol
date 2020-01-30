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

contract ERC20Events {

    constructor() public {
        emit Transfer(address(1), address(2), uint256(3));
        emit Approval(address(4), address(5), uint256(6));
    }

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
}

contract ERC721Events {

    constructor() public {
        emit Transfer(address(1), address(2), uint(3));
        emit Approval(address(4), address(5), uint(6));
        emit ApprovalForAll(address(1), address(2), true);
        emit ApprovalForAll(address(3), address(4), false);
    }

    event Transfer(address indexed from, address indexed to, uint256 indexed tokenId);
    event Approval(address indexed owner, address indexed approved, uint256 indexed tokenId);
    event ApprovalForAll(address indexed owner, address indexed operator, bool approved);

}

contract ERC777Events {

    constructor() public {
        emit Sent(address(1), address(2), address(3), uint256(4), "foo", "bar");
        emit Minted(address(5), address(6), uint256(7), "hello", "world");
        emit Burned(address(8), address(9), uint256(10), "fizz", "buzz");
        emit AuthorizedOperator(address(11), address(12));
        emit RevokedOperator(address(13), address(14));
    }

    event Sent(address indexed operator, address indexed from, address indexed to, uint256 amount, bytes data, bytes operatorData);
    event Minted(address indexed operator, address indexed to, uint256 amount, bytes data, bytes operatorData);
    event Burned(address indexed operator, address indexed from, uint256 amount, bytes data, bytes operatorData);
    event AuthorizedOperator(address indexed operator, address indexed tokenHolder);
    event RevokedOperator(address indexed operator, address indexed tokenHolder);

}

contract ERC1155Events {

    constructor() public {
        emit TransferSingle(address(1), address(2), address(3), uint256(4), uint256(5));

        uint256[] memory ids = new uint256[](3);
        ids[0] = 1;
        ids[1] = 2;
        ids[2] = 3;

        uint256[] memory values = new uint256[](3);
        values[0] = 4;
        values[1] = 5;
        values[2] = 6;

        emit TransferBatch(address(4), address(5), address(6), ids, values);
        emit ApprovalForAll(address(7), address(8), true);
        emit ApprovalForAll(address(9), address(10), false);
        emit URI("hello world", uint256(1));
    }

    event TransferSingle(address indexed _operator, address indexed _from, address indexed _to, uint256 _id, uint256 _value);
    event TransferBatch(address indexed _operator, address indexed _from, address indexed _to, uint256[] _ids, uint256[] _values);
    event ApprovalForAll(address indexed _owner, address indexed _operator, bool _approved);
    event URI(string _value, uint256 indexed _id);


}

