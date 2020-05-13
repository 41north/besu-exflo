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

package io.exflo.gradle.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.units.bigints.UInt256
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.CallParameter
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult
import org.hyperledger.besu.ethereum.vm.Code
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.methods.response.AbiDefinition
import java.io.File
import javax.lang.model.SourceVersion as JavaSourceVersion

/**
 * Task that allows to auto generate Kotlin code from Solidity ABI files.
 *
 * Note: The output produced by this class is not a complete replacement of Web3J codegen module.
 */
open class Web3KtCodegenTask : DefaultTask() {

    @Input
    lateinit var solidityDir: String
    @Input
    lateinit var basePackageName: String
    @Input
    lateinit var destinationDir: String
    @Input
    lateinit var contracts: List<ClassOutput>

    init {
        group = "web3"
        description = "Generates Web3 (simple) contract wrappers in Kotlin"
    }

    @TaskAction
    fun run() {
        contracts.forEach { contract ->
            SolidityContractWrapperGen
                .generate(
                    solidityDir,
                    basePackageName,
                    destinationDir,
                    contract
                )
        }
    }
}

data class ClassOutput(
    val name: String,
    val visibility: ClassVisibility = ClassVisibility.PUBLIC
)

enum class ClassVisibility {
    ABSTRACT,
    PUBLIC
}

object SolidityContractWrapperGen {

    private const val WARNING_MESSAGE = "Auto generated code with SolidityContractWrapperGen. Do not modify manually!"
    private const val TYPE_FUNCTION = "function"
    private val KT_KEYWORDS = setOf(
        "package",
        "as",
        "typealias",
        "class",
        "this",
        "super",
        "val",
        "var",
        "fun",
        "for",
        "null",
        "true",
        "false",
        "is",
        "in",
        "throw",
        "return",
        "break",
        "continue",
        "object",
        "if",
        "try",
        "else",
        "while",
        "do",
        "when",
        "interface",
        "typeof"
    )

    private val mapper: ObjectMapper = ObjectMapperFactory.getObjectMapper()

    fun generate(
        solidityDir: String,
        basePackageName: String,
        destinationDir: String,
        contract: ClassOutput
    ) {
        val className = when (contract.visibility) {
            ClassVisibility.ABSTRACT -> "Abstract${contract.name}"
            ClassVisibility.PUBLIC -> contract.name
        }
        val classVisibility =
            when (contract.visibility) {
                ClassVisibility.ABSTRACT -> KModifier.ABSTRACT
                ClassVisibility.PUBLIC -> KModifier.PUBLIC
            }
        val codeBinary = String(File("${solidityDir}${File.separator}${contract.name}.bin-runtime").readBytes())
        val functionDefinitions = loadContractDefinition(File("${solidityDir}${File.separator}${contract.name}.abi"))

        val contractClass = TypeSpec.classBuilder(className)
            .addKdoc(WARNING_MESSAGE)
            .addModifiers(classVisibility)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("transactionSimulator", TransactionSimulator::class, KModifier.PRIVATE)
                    .addParameter("precompiledAddress", Address::class)
                    .addParameter("contractAddress", Address::class, KModifier.PRIVATE)
                    .addParameter("blockHash", Hash::class, KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("transactionSimulator", TransactionSimulator::class, KModifier.PRIVATE)
                    .initializer("transactionSimulator")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("precompiledAddress", Address::class, KModifier.PRIVATE)
                    .initializer("precompiledAddress")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("contractAddress", Address::class, KModifier.PRIVATE)
                    .initializer("contractAddress")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("blockHash", Hash::class, KModifier.PRIVATE)
                    .initializer("blockHash")
                    .build()
            )
            .addFunctions(buildFunctionDefinitions(functionDefinitions))
            .addFunction(buildExecuteFunction())
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addProperty(
                        PropertySpec.builder("CODE", Code::class, KModifier.PUBLIC)
                            .initializer("Code(Bytes.fromHexString(%S))", codeBinary)
                            .build()
                    )
                    .build()
            )
            .build()

        FileSpec.builder(basePackageName, className)
            .addAliasedImport(org.web3j.abi.datatypes.Address::class, "Web3Address")
            .addType(contractClass)
            .build()
            .writeTo(File(destinationDir))
    }

    private fun loadContractDefinition(absFile: File): List<AbiDefinition> =
        mapper
            .readValue(absFile, Array<AbiDefinition>::class.java)
            .toList()

    private fun buildFunctionDefinitions(functionDefinitions: List<AbiDefinition>): List<FunSpec> =
        functionDefinitions
            .filter { fn -> fn.type == TYPE_FUNCTION }
            .map { fn ->
                val functionName = fn.name.let { if (!JavaSourceVersion.isName(it) || KT_KEYWORDS.contains(it)) "_$it" else it }
                val funBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC)

                when {
                    fn.isConstant -> return@map buildConstantFunction(fn, funBuilder)
                    else -> return@map buildUnsupportedFunction(funBuilder) // For now we don't support other type of functions
                }
            }

    private fun buildUnsupportedFunction(funBuilder: FunSpec.Builder): FunSpec =
        funBuilder
            .addCode("""throw %T("Currently this function is not supported")""", UnsupportedOperationException::class)
            .build()

    private fun buildConstantFunction(functionDefinition: AbiDefinition, funBuilder: FunSpec.Builder): FunSpec {
        // Add annotation
        funBuilder.addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .addMember(""""UNCHECKED_CAST"""")
                .build()
        )

        val outputParameterTypes = asTypeNames(functionDefinition.outputs)

        when (outputParameterTypes.size) {
            0 -> funBuilder.addStatement("""throw new RuntimeException("Cannot call constant function with void return type")""")
            1 -> {
                val fnName = functionDefinition.name
                val typeName = outputParameterTypes.first()
                val outputTypeName = asReturnTypeName(typeName)

                funBuilder
                    .addCode(
                        """
                        |val fn = %T("$fnName", listOf(Web3Address(contractAddress.toHexString())), listOf(%T.create(%T::class.java)))
                        |val fnEncoded = %T.fromHexString(%T.encode(fn))
                        |return execute(fnEncoded, precompiledAddress, blockHash)
                        |   ?.output
                        |   ?.let {
                        |      val rawInput = it.toUnprefixedHexString()
                        |      %T.decode(rawInput, fn.outputParameters) as List<%T>
                        |    }
                        |   ?.firstOrNull()
                        |   ${ mapToOutputTypeName(outputTypeName) }
                    """.trimMargin(),
                        Function::class,
                        TypeReference::class,
                        typeName,
                        Bytes::class,
                        FunctionEncoder::class,
                        FunctionReturnDecoder::class,
                        typeName
                    )
                    .returns(outputTypeName.copy(nullable = true))
            }
            else -> throw UnsupportedOperationException("Complex types not supported yet!")
        }

        return funBuilder.build()
    }

    private fun asTypeNames(namedTypes: List<AbiDefinition.NamedType>): List<TypeName> =
        namedTypes
            .map { namedType -> asTypeName(namedType.type) }

    private fun asTypeName(typeDeclaration: String): TypeName {
        val solidityType = trimStorageDeclaration(typeDeclaration)
        return TypeReference
            .makeTypeReference(solidityType, false, false)
            .type
            .asTypeName()
    }

    private fun asReturnTypeName(type: TypeName): TypeName =
        when (type) {
            is ClassName -> when (type) {
                Bool::class.asClassName() -> BOOLEAN
                Utf8String::class.asClassName() -> STRING
                Uint256::class.asClassName() -> UInt256::class.asTypeName()
                Uint8::class.asClassName() -> Byte::class.asTypeName()
                else -> type
            }
            else -> type
        }

    private fun mapToOutputTypeName(type: TypeName): String =
        when (type) {
            is ClassName -> when (type) {
                BOOLEAN -> "?.value"
                STRING -> "?.value"
                Byte::class.asTypeName() -> "?.value"
                UInt256::class.asTypeName() -> "?.value?.let { UInt256.of(it) }"
                else -> ""
            }
            else -> ""
        }

    private fun buildExecuteFunction(): FunSpec =
        FunSpec.builder("execute")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("method", Bytes::class)
            .addParameter("address", Address::class)
            .addParameter("blockHash", Hash::class)
            .addStatement(
                """
                |return transactionSimulator.process(
                |   %T(
                |       null,
                |       address,
                |       100_000,
                |       null,
                |       null,
                |       method
                |   ),
                |   blockHash
                |)
                |.orElseGet(null)
                |?.takeIf { it.isSuccessful }
                |""".trimMargin(),
                CallParameter::class
            )
            .returns(TransactionSimulatorResult::class.asTypeName().copy(nullable = true))
            .build()

    private fun trimStorageDeclaration(type: String): String =
        if (type.endsWith(" storage") || type.endsWith(" memory")) {
            type.split(" ").toTypedArray()[0]
        } else {
            type
        }
}