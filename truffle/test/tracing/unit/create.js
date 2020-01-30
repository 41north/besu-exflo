/*
 * Copyright (c) 2020 41North.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

contract('CreateOpCodeUnit', function(accounts) {
  it('should create a new smart contract with valid code and with ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createWithValidCodeAndWithEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should create a new smart contract with valid code and without ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createWithValidCodeAndWithoutEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should create a new smart contract without code and without ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createWithoutCodeAndWithoutEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should create a new smart contract with invalid code and without ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createWithInvalidCodeAndWithoutEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should create two new smart contracts with valid code and with ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createTwoContractsWithValidCodeAndWithEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should create two new smart contracts with valid and invalid code and with ether associated', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const CreateOpCodeUnit = artifacts.require('CreateOpCodeUnit')
    const instance = await CreateOpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.createTwoContractsWithValidAndInvalidCodeAndWithEtherAssociated()

    // assert tx status is ok
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })
})
