pragma solidity >=0.4.21 <0.6.0;

import "./ERC1155.sol";

contract ERC1155Detector {

  bytes4 constant ERC1155ID = 0xd9b67a26;
  bytes4 constant ERC1155TokenReceiverID = 0x4e2312e0;

  constructor() public {}

  /**
   * @dev Check interface ERC1155.
   */
  function hasERC1155Interface(address _target) public view returns (bool) {
    return ERC1155(_target).supportsInterface(ERC1155ID);
  }

  /**
   * @dev Check interface ERC1155 (TokenReceiver).
   */
  function hasERC1155TokenReceiverInterface(address _target) public view returns (bool) {
    return ERC1155(_target).supportsInterface(ERC1155TokenReceiverID);
  }

}
