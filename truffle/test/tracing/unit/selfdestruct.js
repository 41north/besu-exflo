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

contract('SelfDestructOpCodeUnit', function(accounts) {
  it('should destroy the contract and refund the sender', async function() {
    const [ownerAccount, refundAccount] = accounts

    // deploy contract
    const SelfDestruct = artifacts.require('SelfDestructOpCodeUnit')
    const instance = await SelfDestruct.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger self destruct
    const result = await instance.destroyAndRefundSender({ from: refundAccount })

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

  it("should destroy and refund self while 'destroying' ether", async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const SelfDestruct = artifacts.require('SelfDestructOpCodeUnit')
    const instance = await SelfDestruct.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger self destruct
    const result = await instance.destroyAndRefundSelf()

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
