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

contract('Create2OpCodeUnit', function(accounts) {
  it('should create a new smart contract with an empty source', async function() {
    const [ownerAccount] = accounts

    // deploy contract
    const Create2OpCodeUnit = artifacts.require('Create2OpCodeUnit')
    const instance = await Create2OpCodeUnit.new({ from: ownerAccount, value: 1000000000000000000 })

    // trigger opcode call
    const result = await instance.create2OpCode()

    // store tx status
    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.receipt.blockNumber,
      blockHash: result.receipt.blockHash,
      txHash: result.tx,
      txStatus: result.receipt.status,
    })
  })
})
