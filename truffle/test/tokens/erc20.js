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

['InvalidERC20', 'MinimalERC20', 'DetailedERC20', 'WeirdNameCharsERC20', 'CappedERC20']
  .forEach(contractName => {
    contract(contractName, function(accounts) {
      const [ownerAddress] = accounts;
      const Erc20 = artifacts.require(contractName);

      it('should deploy the contract', async function() {
        // Deploy
        const result = await Erc20.new(10, { from: ownerAddress });

        // Retrieve receipt
        const receipt = await web3.eth.getTransactionReceipt(result.transactionHash);

        // collect web3 block, tx hash and tx status for reporting
        this.test.web3.summaries.push({
          blockNumber: receipt.blockNumber,
          blockHash: receipt.blockHash,
          txHash: receipt.transactionHash,
          txStatus: receipt.status,
        })
      })
    })
  });
