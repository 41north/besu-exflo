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

// const {
//   deployAccount,
//   numberToUint256,
//   encodeParam,
//   isContract,
//   buildCreate2Address
// } = require('../../utils');
//
// contract('Create2FactoryIntegration', accounts => {
//
//   const [sender] = accounts;
//
//   const {bytecode: accountBytecode} = require('/truffle/build/contracts/DummyContract.json');
//   const Create2Factory = artifacts.require('Create2FactoryIntegration');
//   const salt = 1;
//
//   let instance;
//   let computedAddress;
//
//   before(async () => {
//     instance = await Create2Factory.new({from: sender, value: 1000000000000000000});
//   });
//
//   it('should "park" an address for a new contract without creating it', async () => {
//
//     const bytecode = `${accountBytecode}${encodeParam('address', sender).slice(2)}`;
//
//     computedAddress = buildCreate2Address(
//       instance.address,
//       numberToUint256(salt),
//       bytecode
//     );
//
//     const isContract = await isContract(computedAddress);
//     expect(isContract, 'Computed address should not be a contract').to.be.false;
//
//   });
//
//   it('should deploy the account', async () => {
//
//     const result = await deployAccount(instance.address, salt, sender);
//
//   })
//
// });
