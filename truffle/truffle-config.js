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

require('@babel/register')
require('@babel/polyfill')

const HDWalletProvider = require('@truffle/hdwallet-provider')

module.exports = {
  networks: {
    development: {
      provider: () =>
        new HDWalletProvider(
          [
            '8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63',
            'c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3',
            'ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f',
          ],
          'http://127.0.0.1:8545',
          0,
          3,
        ),
      network_id: '*', // Custom network
      gasPrice: 20000000000, // 20 gwei (in wei)
      from: '0xfe3b557e8fb62b89f4916b721be55ceb828dbd73', // Account to send txs from (default: accounts[0])
    },
  },
  mocha: {
    timeout: 100000,
    useColors: true,
    reporter: './reporter/truffle-reporter.js',
  },
  compilers: {
    solc: {
      version: '0.5.12',
      docker: false,
      settings: {
        optimizer: {
          enabled: false,
          runs: 200,
        },
      },
    },
  },
}
