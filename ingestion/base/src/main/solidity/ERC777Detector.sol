pragma solidity >=0.4.21 <0.6.0;

import "./ERC777.sol";

/**
* @dev Class that detects compliant ERC777 smart contracts.
*/
contract ERC777Detector {

  constructor() public {}

  /**
   * @dev Check ERC777 interface.
   */
  function hasERC777Interface(address _target) public view returns (bool) {
    IERC777 c = IERC777(_target);

    // Let's call these view methods directly to see if they throw or not
    c.name();
    c.symbol();
    c.totalSupply();
    c.granularity();
    c.defaultOperators();

    return true;
  }

}
