pragma solidity >=0.4.21 <0.6.0;

import "./ERC721.sol";

contract ERC721Detector {

    bytes4 constant ERC721ID = 0x80ac58cd;
    bytes4 constant ERC721MetadataID = 0x5b5e139f;
    bytes4 constant ERC721EnumerableID = 0x780e9d63;

    constructor() public {}

    /**
     * @dev Check interface ERC721.
     */
    function hasERC721Interface(address _target) public view returns (bool) {
        return ERC165(_target).supportsInterface(ERC721ID);
    }

    /**
     * @dev Check interface ERC721Metadata.
     */
    function hasERC721MetadataInterface(address _target) public view returns (bool) {
        return ERC165(_target).supportsInterface(ERC721MetadataID);
    }

    /**
     * @dev Check interface ERC721Enumerable.
     */
    function hasERC721EnumerableInterface(address _target) public view returns (bool) {
        return ERC165(_target).supportsInterface(ERC721EnumerableID);
    }

}