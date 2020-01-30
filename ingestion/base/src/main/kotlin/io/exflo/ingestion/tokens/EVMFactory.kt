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

package io.exflo.ingestion.tokens

import java.math.BigInteger
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.mainnet.IstanbulGasCalculator
import org.hyperledger.besu.ethereum.vm.EVM
import org.hyperledger.besu.ethereum.vm.GasCalculator
import org.hyperledger.besu.ethereum.vm.OperationRegistry
import org.hyperledger.besu.ethereum.vm.operations.AddModOperation
import org.hyperledger.besu.ethereum.vm.operations.AddOperation
import org.hyperledger.besu.ethereum.vm.operations.AddressOperation
import org.hyperledger.besu.ethereum.vm.operations.AndOperation
import org.hyperledger.besu.ethereum.vm.operations.BalanceOperation
import org.hyperledger.besu.ethereum.vm.operations.BlockHashOperation
import org.hyperledger.besu.ethereum.vm.operations.ByteOperation
import org.hyperledger.besu.ethereum.vm.operations.CallCodeOperation
import org.hyperledger.besu.ethereum.vm.operations.CallDataCopyOperation
import org.hyperledger.besu.ethereum.vm.operations.CallDataLoadOperation
import org.hyperledger.besu.ethereum.vm.operations.CallDataSizeOperation
import org.hyperledger.besu.ethereum.vm.operations.CallOperation
import org.hyperledger.besu.ethereum.vm.operations.CallValueOperation
import org.hyperledger.besu.ethereum.vm.operations.CallerOperation
import org.hyperledger.besu.ethereum.vm.operations.ChainIdOperation
import org.hyperledger.besu.ethereum.vm.operations.CodeCopyOperation
import org.hyperledger.besu.ethereum.vm.operations.CodeSizeOperation
import org.hyperledger.besu.ethereum.vm.operations.CoinbaseOperation
import org.hyperledger.besu.ethereum.vm.operations.Create2Operation
import org.hyperledger.besu.ethereum.vm.operations.CreateOperation
import org.hyperledger.besu.ethereum.vm.operations.DelegateCallOperation
import org.hyperledger.besu.ethereum.vm.operations.DifficultyOperation
import org.hyperledger.besu.ethereum.vm.operations.DivOperation
import org.hyperledger.besu.ethereum.vm.operations.DupOperation
import org.hyperledger.besu.ethereum.vm.operations.EqOperation
import org.hyperledger.besu.ethereum.vm.operations.ExpOperation
import org.hyperledger.besu.ethereum.vm.operations.ExtCodeCopyOperation
import org.hyperledger.besu.ethereum.vm.operations.ExtCodeHashOperation
import org.hyperledger.besu.ethereum.vm.operations.ExtCodeSizeOperation
import org.hyperledger.besu.ethereum.vm.operations.GasLimitOperation
import org.hyperledger.besu.ethereum.vm.operations.GasOperation
import org.hyperledger.besu.ethereum.vm.operations.GasPriceOperation
import org.hyperledger.besu.ethereum.vm.operations.GtOperation
import org.hyperledger.besu.ethereum.vm.operations.InvalidOperation
import org.hyperledger.besu.ethereum.vm.operations.IsZeroOperation
import org.hyperledger.besu.ethereum.vm.operations.JumpDestOperation
import org.hyperledger.besu.ethereum.vm.operations.JumpOperation
import org.hyperledger.besu.ethereum.vm.operations.JumpiOperation
import org.hyperledger.besu.ethereum.vm.operations.LogOperation
import org.hyperledger.besu.ethereum.vm.operations.LtOperation
import org.hyperledger.besu.ethereum.vm.operations.MLoadOperation
import org.hyperledger.besu.ethereum.vm.operations.MSizeOperation
import org.hyperledger.besu.ethereum.vm.operations.MStore8Operation
import org.hyperledger.besu.ethereum.vm.operations.MStoreOperation
import org.hyperledger.besu.ethereum.vm.operations.ModOperation
import org.hyperledger.besu.ethereum.vm.operations.MulModOperation
import org.hyperledger.besu.ethereum.vm.operations.MulOperation
import org.hyperledger.besu.ethereum.vm.operations.NotOperation
import org.hyperledger.besu.ethereum.vm.operations.NumberOperation
import org.hyperledger.besu.ethereum.vm.operations.OrOperation
import org.hyperledger.besu.ethereum.vm.operations.OriginOperation
import org.hyperledger.besu.ethereum.vm.operations.PCOperation
import org.hyperledger.besu.ethereum.vm.operations.PopOperation
import org.hyperledger.besu.ethereum.vm.operations.PushOperation
import org.hyperledger.besu.ethereum.vm.operations.ReturnDataCopyOperation
import org.hyperledger.besu.ethereum.vm.operations.ReturnDataSizeOperation
import org.hyperledger.besu.ethereum.vm.operations.ReturnOperation
import org.hyperledger.besu.ethereum.vm.operations.RevertOperation
import org.hyperledger.besu.ethereum.vm.operations.SDivOperation
import org.hyperledger.besu.ethereum.vm.operations.SGtOperation
import org.hyperledger.besu.ethereum.vm.operations.SLoadOperation
import org.hyperledger.besu.ethereum.vm.operations.SLtOperation
import org.hyperledger.besu.ethereum.vm.operations.SModOperation
import org.hyperledger.besu.ethereum.vm.operations.SStoreOperation
import org.hyperledger.besu.ethereum.vm.operations.SarOperation
import org.hyperledger.besu.ethereum.vm.operations.SelfBalanceOperation
import org.hyperledger.besu.ethereum.vm.operations.SelfDestructOperation
import org.hyperledger.besu.ethereum.vm.operations.Sha3Operation
import org.hyperledger.besu.ethereum.vm.operations.ShlOperation
import org.hyperledger.besu.ethereum.vm.operations.ShrOperation
import org.hyperledger.besu.ethereum.vm.operations.SignExtendOperation
import org.hyperledger.besu.ethereum.vm.operations.StaticCallOperation
import org.hyperledger.besu.ethereum.vm.operations.StopOperation
import org.hyperledger.besu.ethereum.vm.operations.SubOperation
import org.hyperledger.besu.ethereum.vm.operations.SwapOperation
import org.hyperledger.besu.ethereum.vm.operations.TimestampOperation
import org.hyperledger.besu.ethereum.vm.operations.XorOperation
import org.hyperledger.besu.util.bytes.Bytes32
import org.hyperledger.besu.util.bytes.BytesValue

// TODO: Review with Besu devs if we can obtain concrete EVM versions directly as currently methods are private

/**
 * This class registers an Istanbul EVM used by our Token detector precompiled contracts.
 *
 * Ideally this class shouldn't be necessary if affected classes had public methods.
 */
object EVMFactory {
    fun istanbul(chainId: BigInteger): EVM {
        val registry = OperationRegistry()

        val gasCalculator = IstanbulGasCalculator()

        EVMRegistry.registerIstanbulOpcodes(
            registry,
            gasCalculator,
            Account.DEFAULT_VERSION,
            chainId
        )

        return EVM(registry, gasCalculator)
    }
}

internal object EVMRegistry {
    private fun registerFrontierOpcodes(
        registry: OperationRegistry,
        gasCalculator: GasCalculator,
        accountVersion: Int
    ) {
        registry.put(AddOperation(gasCalculator), accountVersion)
        registry.put(AddOperation(gasCalculator), accountVersion)
        registry.put(MulOperation(gasCalculator), accountVersion)
        registry.put(SubOperation(gasCalculator), accountVersion)
        registry.put(DivOperation(gasCalculator), accountVersion)
        registry.put(SDivOperation(gasCalculator), accountVersion)
        registry.put(ModOperation(gasCalculator), accountVersion)
        registry.put(SModOperation(gasCalculator), accountVersion)
        registry.put(ExpOperation(gasCalculator), accountVersion)
        registry.put(AddModOperation(gasCalculator), accountVersion)
        registry.put(MulModOperation(gasCalculator), accountVersion)
        registry.put(SignExtendOperation(gasCalculator), accountVersion)
        registry.put(LtOperation(gasCalculator), accountVersion)
        registry.put(GtOperation(gasCalculator), accountVersion)
        registry.put(SLtOperation(gasCalculator), accountVersion)
        registry.put(SGtOperation(gasCalculator), accountVersion)
        registry.put(EqOperation(gasCalculator), accountVersion)
        registry.put(IsZeroOperation(gasCalculator), accountVersion)
        registry.put(AndOperation(gasCalculator), accountVersion)
        registry.put(OrOperation(gasCalculator), accountVersion)
        registry.put(XorOperation(gasCalculator), accountVersion)
        registry.put(NotOperation(gasCalculator), accountVersion)
        registry.put(ByteOperation(gasCalculator), accountVersion)
        registry.put(Sha3Operation(gasCalculator), accountVersion)
        registry.put(AddressOperation(gasCalculator), accountVersion)
        registry.put(BalanceOperation(gasCalculator), accountVersion)
        registry.put(OriginOperation(gasCalculator), accountVersion)
        registry.put(CallerOperation(gasCalculator), accountVersion)
        registry.put(CallValueOperation(gasCalculator), accountVersion)
        registry.put(CallDataLoadOperation(gasCalculator), accountVersion)
        registry.put(CallDataSizeOperation(gasCalculator), accountVersion)
        registry.put(CallDataCopyOperation(gasCalculator), accountVersion)
        registry.put(CodeSizeOperation(gasCalculator), accountVersion)
        registry.put(CodeCopyOperation(gasCalculator), accountVersion)
        registry.put(GasPriceOperation(gasCalculator), accountVersion)
        registry.put(ExtCodeCopyOperation(gasCalculator), accountVersion)
        registry.put(ExtCodeSizeOperation(gasCalculator), accountVersion)
        registry.put(BlockHashOperation(gasCalculator), accountVersion)
        registry.put(CoinbaseOperation(gasCalculator), accountVersion)
        registry.put(TimestampOperation(gasCalculator), accountVersion)
        registry.put(NumberOperation(gasCalculator), accountVersion)
        registry.put(DifficultyOperation(gasCalculator), accountVersion)
        registry.put(GasLimitOperation(gasCalculator), accountVersion)
        registry.put(PopOperation(gasCalculator), accountVersion)
        registry.put(MLoadOperation(gasCalculator), accountVersion)
        registry.put(MStoreOperation(gasCalculator), accountVersion)
        registry.put(MStore8Operation(gasCalculator), accountVersion)
        registry.put(SLoadOperation(gasCalculator), accountVersion)
        registry.put(
            SStoreOperation(
                gasCalculator,
                SStoreOperation.FRONTIER_MINIMUM
            ),
            accountVersion
        )
        registry.put(JumpOperation(gasCalculator), accountVersion)
        registry.put(JumpiOperation(gasCalculator), accountVersion)
        registry.put(PCOperation(gasCalculator), accountVersion)
        registry.put(MSizeOperation(gasCalculator), accountVersion)
        registry.put(GasOperation(gasCalculator), accountVersion)
        registry.put(JumpDestOperation(gasCalculator), accountVersion)
        registry.put(ReturnOperation(gasCalculator), accountVersion)
        registry.put(InvalidOperation(gasCalculator), accountVersion)
        registry.put(StopOperation(gasCalculator), accountVersion)
        registry.put(SelfDestructOperation(gasCalculator), accountVersion)
        registry.put(CreateOperation(gasCalculator), accountVersion)
        registry.put(CallOperation(gasCalculator), accountVersion)
        registry.put(CallCodeOperation(gasCalculator), accountVersion)

        // Register the PUSH1, PUSH2, ..., PUSH32 operations.

        for (i in 1..32) {
            registry.put(PushOperation(i, gasCalculator), accountVersion)
        }

        // Register the DUP1, DUP2, ..., DUP16 operations.

        for (i in 1..16) {
            registry.put(DupOperation(i, gasCalculator), accountVersion)
        }

        // Register the SWAP1, SWAP2, ..., SWAP16 operations.

        for (i in 1..16) {
            registry.put(SwapOperation(i, gasCalculator), accountVersion)
        }

        // Register the LOG0, LOG1, ..., LOG4 operations.

        for (i in 0..4) {
            registry.put(LogOperation(i, gasCalculator), accountVersion)
        }
    }

    private fun registerHomesteadOpcodes(
        registry: OperationRegistry,
        gasCalculator: GasCalculator,
        accountVersion: Int
    ) {
        registerFrontierOpcodes(registry, gasCalculator, accountVersion)
        registry.put(DelegateCallOperation(gasCalculator), accountVersion)
    }

    private fun registerByzantiumOpcodes(
        registry: OperationRegistry,
        gasCalculator: GasCalculator,
        accountVersion: Int
    ) {
        registerHomesteadOpcodes(registry, gasCalculator, accountVersion)
        registry.put(ReturnDataCopyOperation(gasCalculator), accountVersion)
        registry.put(ReturnDataSizeOperation(gasCalculator), accountVersion)
        registry.put(RevertOperation(gasCalculator), accountVersion)
        registry.put(StaticCallOperation(gasCalculator), accountVersion)
    }

    private fun registerConstantinopleOpcodes(
        registry: OperationRegistry,
        gasCalculator: GasCalculator,
        accountVersion: Int
    ) {
        registerByzantiumOpcodes(registry, gasCalculator, accountVersion)
        registry.put(Create2Operation(gasCalculator), accountVersion)
        registry.put(SarOperation(gasCalculator), accountVersion)
        registry.put(ShlOperation(gasCalculator), accountVersion)
        registry.put(ShrOperation(gasCalculator), accountVersion)
        registry.put(ExtCodeHashOperation(gasCalculator), accountVersion)
    }

    fun registerIstanbulOpcodes(
        registry: OperationRegistry,
        gasCalculator: GasCalculator,
        accountVersion: Int,
        chainId: BigInteger
    ) {
        registerConstantinopleOpcodes(registry, gasCalculator, accountVersion)
        registry.put(
            ChainIdOperation(
                gasCalculator,
                Bytes32.leftPad(BytesValue.of(*chainId.toByteArray()))
            ),
            Account.DEFAULT_VERSION
        )
        registry.put(SelfBalanceOperation(gasCalculator), Account.DEFAULT_VERSION)
        registry.put(
            SStoreOperation(
                gasCalculator,
                SStoreOperation.EIP_1706_MINIMUM
            ),
            Account.DEFAULT_VERSION
        )
    }
}
