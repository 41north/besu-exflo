pragma solidity >=0.4.21 <0.6.0;

import "./ERC20.sol";

/**
* @dev Class that detects compliant ERC20 smart contracts.
*/
contract ERC20Detector {

    constructor() public {}

    /**
     * @dev checkERC20DetailedInterface() should not throw.
     */
    function hasERC20DetailedInterface(address _target) public view returns (bool) {
        ERC20Detailed c = ERC20Detailed(_target);

        // Let's call these view methods directly to see if they throw or not
        c.name();
        c.symbol();
        c.decimals();

        return true;
    }

    /**
     * @dev checkERC20CappedInterface() should not throw.
     */
    function hasERC20CappedInterface(address _target) public view returns (bool) {
        ERC20Capped c = ERC20Capped(_target);

        // Let's call this view method directly to see if the throw or not
        c.cap();

        return true;
    }

}