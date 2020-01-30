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

const Tx = require('ethereumjs-tx').Transaction
const Common = require('ethereumjs-common').default

contract('CreateDestroyIntegration', async function(accounts) {
  const [ownerAccount] = accounts
  const CreateDestroyIntegration = artifacts.require('CreateDestroyIntegration')

  it('should create and destroy itself on contract deploy', async function() {
    // For this specific test, we need to resort to deploy manually the transaction
    // The reason is that truffle doesn't work if the deployed contract selfdestructs on constructor

    // Dev network
    const dev = Common.forCustomChain(
      'mainnet',
      {
        name: 'dev',
        chainId: 2018,
      },
      'petersburg',
    )

    // Private key from ownerAccount (as defined in truffle-config.js)
    const privateKey = Buffer.from('8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63', 'hex')

    // Prepare transaction
    const tx = new Tx(
      {
        gasPrice: web3.utils.toHex(20000000000),
        gasLimit: web3.utils.toHex(4000000),
        from: ownerAccount,
        data: CreateDestroyIntegration.bytecode,
        value: web3.utils.toHex(1000000000000000000),
        nonce: await web3.eth.getTransactionCount(ownerAccount),
      },
      {
        common: dev,
      },
    )
    tx.sign(privateKey)

    //console.log(tx.validate(true))

    // Serialize
    const serializedTx = `0x${tx.serialize().toString('hex')}`

    // Deploy tx
    const result = await web3.eth.sendSignedTransaction(serializedTx)

    // collect web3 block, tx hash and tx status for reporting
    this.test.web3.summaries.push({
      blockNumber: result.blockNumber,
      blockHash: result.blockHash,
      txHash: result.transactionHash,
      txStatus: true,
    })
  })
})
