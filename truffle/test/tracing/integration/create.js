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

contract('CreateIntegration', function(accounts) {
  const [ownerAccount] = accounts
  const CreateIntegration = artifacts.require('CreateIntegration')

  it('should create a DummyContract when "createDummyContract" method is called', async function() {
    // Create an instance of FactoryContract
    const instance = await CreateIntegration.new({ from: ownerAccount, value: 2000000000000000000 })

    // call contract
    const result = await instance.createDummyContract()

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

  it('should NOT create DummyOutOfGasContract when "createDummyOutOfGasContract" method is called', async function() {
    // Create an instance of FactoryContract
    const instance = await CreateIntegration.new({ from: ownerAccount, value: 2000000000000000000 })

    // call contract
    try {
      await instance.createDummyOutOfGasContract()
    } catch (result) {
      // check it was deployed correctly
      expect(result.receipt.status).to.be.false

      // collect web3 block, tx hash and tx status for reporting
      this.test.web3.summaries.push({
        blockNumber: result.receipt.blockNumber,
        blockHash: result.receipt.blockHash,
        txHash: result.tx,
        txStatus: result.receipt.status,
      })
    }
  })

  it('should NOT create DummyBadOpCodeContract when "createDummyBadOpCodeContract" method is called', async function() {
    // Create an instance of FactoryContract
    const instance = await CreateIntegration.new({ from: ownerAccount, value: 2000000000000000000 })

    // call contract
    try {
      await instance.createDummyBadOpCodeContract()
    } catch (result) {
      // check it was deployed correctly
      expect(result.receipt.status).to.be.false

      // collect web3 block, tx hash and tx status for reporting
      this.test.web3.summaries.push({
        blockNumber: result.receipt.blockNumber,
        blockHash: result.receipt.blockHash,
        txHash: result.tx,
        txStatus: result.receipt.status,
      })
    }
  })

  it('should NOT create a DummyRevertContract when "createDummyRevertContract" method is called', async function() {
    // Create an instance of FactoryContract
    const instance = await CreateIntegration.new({ from: ownerAccount, value: 2000000000000000000 })

    // call contract
    try {
      await instance.createDummyRevertContract()
    } catch (result) {
      // check it was deployed correctly
      expect(result.receipt.status).to.be.false

      // collect web3 block, tx hash and tx status for reporting
      this.test.web3.summaries.push({
        blockNumber: result.receipt.blockNumber,
        blockHash: result.receipt.blockHash,
        txHash: result.tx,
        txStatus: result.receipt.status,
      })
    }
  })

  it('should create a CreateNestedIntegration contract when "createNestedDummyContracts" method is called', async function() {
    // Create an instance of FactoryContract
    const instance = await CreateIntegration.new({ from: ownerAccount, value: 10000000000000000000 })

    // call contract
    const result = await instance.createNestedDummyContracts({ value: 5000000000000000000 })

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
