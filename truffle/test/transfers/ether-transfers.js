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

import { randomAddress, randomValue } from '../utils'

const RANDOM_ACCOUNTS = 5

contract('EtherTransfers', function(accounts) {
  const [ownerAddress] = accounts

  it('should transfer regular ether', async function() {
    for (let i = 0; i < RANDOM_ACCOUNTS; i++) {
      accounts[i] = {
        address: randomAddress(),
        value: randomValue(),
      }

      const result = await web3.eth.sendTransaction({
        from: ownerAddress,
        to: accounts[i].address,
        value: accounts[i].value,
      })

      // collect web3 block, tx hash and tx status for reporting
      this.test.web3.summaries.push({
        blockNumber: result.blockNumber,
        blockHash: result.blockHash,
        txHash: result.transactionHash,
        txStatus: result.status,
      })
    }
  })
})
