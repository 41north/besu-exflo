pragma solidity >=0.4.21 <0.6.0;

import "./ERC165.sol";

/**
* @dev Class that detects compliant ERC165 smart contracts.
*/
contract ERC165Detector {

    bytes4 constant ERC165ID = 0x01ffc9a7;

    constructor() public {}

    /**
     * @dev checkERC165Interface() should not throw.
     */
    function hasERC165Interface(address _target) public view returns (bool) {
        return ERC165(_target).supportsInterface(ERC165ID);
    }

}