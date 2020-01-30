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

contract('CallEtherSenderIntegration', function(accounts) {
  const [ownerAddress, senderAddress] = accounts
  const EtherSender = artifacts.require('CallEtherSenderIntegration')

  let instance

  before(async () => {
    instance = await EtherSender.new({ from: ownerAddress, value: 1000000 })
  })

  it('should send ether successfully', async function() {
    // call contract
    const result = await instance.sendEther(1000000, { value: 1000000, from: senderAddress })

    // check it was deployed correctly
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should transfer ether successfully', async function() {
    // call contract
    const result = await instance.transferEther(1000000, { value: 1000000, from: senderAddress })

    // check it was deployed correctly
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should transfer ether successfully via a fallback function', async function() {
    // call contract
    const result = await instance.callValueEther(1000000, { value: 1000000, from: senderAddress })

    // check it was deployed correctly
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should fail to send ether when gas is too low', async function() {
    // call contract
    try {
      await instance.sendEther(2000000, { value: 1000000, from: senderAddress, gas: 1000 })
    } catch (e) {
      assert.equal(e.toString(), 'Error: Intrinsic gas exceeds gas limit', 'Incorrect error thrown')
    }
  })

  it('should fail to transfer ether when gas is too low', async function() {
    // call contract
    try {
      await instance.transferEther(2000000, { value: 1000000, from: senderAddress, gas: 1000 })
    } catch (e) {
      expect(e.toString(), 'Incorrect error thrown').to.equal('Error: Intrinsic gas exceeds gas limit')
    }
  })

  it('should fail to transfer ether via a fallback function when gas is too low', async function() {
    // call contract
    try {
      await instance.callValueEther(2000000, { value: 1000000, from: senderAddress, gas: 1000 })
    } catch (e) {
      assert.equal(e.toString(), 'Error: Intrinsic gas exceeds gas limit', 'Incorrect error thrown')
    }
  })
})

contract('CallNestedEtherSenderIntegration', function(accounts) {
  const [ownerAddress, senderAddress] = accounts
  const NestedEtherSender = artifacts.require('CallNestedEtherSenderIntegration')

  let instance

  before(async () => {
    instance = await NestedEtherSender.new({ from: ownerAddress, value: 1000000 })
  })

  it('should send ether to a nested child contract', async function() {
    // call contract
    const result = await instance.nestedSendEther(2000000, { value: 2000000, from: senderAddress })

    // check it was deployed correctly
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should transfer ether to a nested child contract', async function() {
    // call contract
    const result = await instance.nestedTransferEther(1000000, { value: 1000000, from: senderAddress })

    // check it was deployed correctly
    expect(result.receipt.status).to.be.true

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })

  it('should fail to send ether to a nested child contract when value is too low', async function() {
    try {
      await instance.nestedSendEther(2000000, { value: 1000000, from: senderAddress })
    } catch (e) {
      expect(e.toString().substring(0, 25), 'Error message incorrect').to.equal('StatusError: Transaction:')
    }
  })

  it('should fail to transfer ether to a nested child contract when value is too low', async function() {
    try {
      await instance.nestedTransferEther(3000000, { value: 2000000, from: senderAddress })
    } catch (e) {
      expect(e.toString().substring(0, 25), 'Error message incorrect').to.equal('StatusError: Transaction:')
    }
  })
})
