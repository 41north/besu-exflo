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

contract('SelfDestructIntegration', function(accounts) {
  const [ownerAccount, refundAccount] = accounts
  const SelfDestructIntegration = artifacts.require('SelfDestructIntegration')

  it('should destroy the contract and refund the sender', async function() {
    // deploy contract
    const instance = await SelfDestructIntegration.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger self destruct
    const result = await instance.destroyAndRefundSender({ from: refundAccount })

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

  it('should destroy and refund self (which triggers the destroy of ether)', async function() {
    // deploy contract
    const instance = await SelfDestructIntegration.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger self destruct
    const result = await instance.destroyAndRefundSelf()

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
})

contract('SelfDestructDelegatingCallsIntegration', function(accounts) {
  const [ownerAccount] = accounts
  const SelfDestructDelegatingCallsIntegration = artifacts.require('SelfDestructDelegatingCallsIntegration')

  it('should send ether to contract after self referencing destroy', async function() {
    // deploy contract
    const instance = await SelfDestructDelegatingCallsIntegration.new({ from: ownerAccount, value: 10000000000000000000 })

    // trigger self destruct
    const result = await instance.sendEtherToContractAfterSelfReferencingDestroy()

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

  it('should produce a cascading destroy and refund sender', async function() {
    // deploy contract
    const instance = await SelfDestructDelegatingCallsIntegration.new({ from: ownerAccount, value: 10000000000000000000 })

    // trigger self destruct
    const result = await instance.cascadingDestroyAndRefundSender({ from: ownerAccount })

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

  it('should create self destroying contracts and self destruct itself', async function() {
    // deploy contract
    const instance = await SelfDestructDelegatingCallsIntegration.new({ from: ownerAccount, value: 10000000000000000000 })

    // trigger self destruct
    const result = await instance.createSelfDestroyingContractsAndSelfDestructItself({ from: ownerAccount })

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
})
