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

// Acknowledgements: This source code has been inspired by the awesome work of Adam Gruber on his Mochawesome reporter
// URL: https://github.com/adamgruber/mochawesome | License: MIT

const Base = require('mocha/lib/reporters/base')
const uuid = require('uuid/v5')
const fs = require('fs-extra')
const fsu = require('fsu')
const path = require('path')
const dateFormat = require('dateformat')
const isEmpty = require('lodash.isempty')
const stringify = require('json-stringify-safe')
const chalk = require('chalk')
const stripAnsi = require('strip-ansi')

// Track the total number of tests registered
const totalTestsRegistered = { total: 0 }

const FILE_EXT_REGEX = /\.[^.]*?$/

// UUID Namespace to generate predictable UUIDs
const UUID_NAMESPACE = '1b671a64-40d5-491e-99b0-da01ff1f3341'

// Sane default options
const DEFAULT_OPTS = {
  quiet: false,
  reportDir: 'truffle-report',
  reportFilename: 'truffle-report',
  consoleReporter: 'list',
  overwrite: true,
  timestamp: false,
  ts: false,
}

/**
 * Logs an entry using the console and the level
 *
 * @param {String} msg - message to log
 * @param {String} level - log level [log, info, warn, error]
 * @param {Object} config - configuration object
 */
function log(msg, level = 'log', config = {}) {
  // Don't log messages in quiet mode
  if (config.quiet) return
  const logMethod = console[level] || console.log
  let out = msg
  if (typeof msg === 'object') {
    out = stringify(msg, null, 2)
  }
  logMethod(`[${chalk.gray('truffle-reporter')}] ${out}\n`)
}

/**
 * Creates a default configuration object with provided sane defaults.
 * @param {Object} opts - configuration object
 */
function conf(opts = {}) {
  return Object.assign({}, DEFAULT_OPTS, opts.reporterOptions || {})
}

/**
 * Saves a file
 *
 * @param {string} filename Name of file to save
 * @param {string} data Data to be saved
 * @param {boolean} overwrite Overwrite existing files (default: true)
 *
 * @return {Promise} Resolves with filename if successfully saved
 */
function saveFile(filename, data, overwrite) {
  if (overwrite) {
    return fs.outputFile(filename, data).then(() => filename)
  }

  return new Promise((resolve, reject) => {
    fsu.writeFileUnique(filename.replace(FILE_EXT_REGEX, '{_###}$&'), data, { force: true }, (err, savedFile) =>
      err === null ? resolve(savedFile) : reject(err),
    )
  })
}

/**
 * Get the dateformat format string based on the timestamp option
 *
 * @param {string|boolean} timestamp Timestamp option value
 *
 * @return {string} Valid dateformat format string
 */
function getTimestampFormat(timestamp) {
  switch (timestamp) {
    case '':
    case 'true':
    case true:
      return 'isoDateTime'
    default:
      return timestamp
  }
}

/**
 * Construct the path/name of the JSON file to be saved
 *
 * @param {Object} reportOptions Options object
 * @param {string} reportOptions.reportDir Directory to save report to
 * @param {string} reportOptions.reportFilename Filename to save report to
 * @param {string} reportOptions.timestamp Timestamp format to be appended to the filename
 *
 * @return {string} Fully resolved path without extension
 */
function getFilename({ reportDir, reportFilename = 'truffle-report', timestamp }) {
  let ts = ''
  if (timestamp !== false && timestamp !== 'false') {
    const format = getTimestampFormat(timestamp)
    ts = `_${dateFormat(new Date(), format)}`
      // replace commas, spaces or comma-space combinations with underscores
      .replace(/(,\s*)|,|\s+/g, '_')
      // replace forward and back slashes with hyphens
      .replace(/\\|\//g, '-')
      // remove colons
      .replace(/:/g, '')
  }
  const filename = `${reportFilename.replace(FILE_EXT_REGEX, '')}${ts}`
  return path.resolve(process.cwd(), reportDir, filename)
}

/**
 * Create the report
 *
 * @param {string} data JSON test data
 * @param {Object} opts Report options
 *
 * @return {Promise} Resolves if report was created successfully
 */
function create(data, opts = {}) {
  // For saving JSON from truffle-reporter
  const jsonFile = `${getFilename(opts)}.json`

  const { overwrite } = opts

  return saveFile(
    jsonFile,
    // Preserve `undefined` values as `null` when stringifying
    JSON.stringify(data, (k, v) => (v === undefined ? null : v), 2),
    overwrite,
  )
}

/**
 * Done function gets called before mocha exits
 *
 * Creates and saves the report HTML and JSON files
 *
 * @param {Object} output    Final report object
 * @param {Object} options   Options to pass to report generator
 * @param {Object} config    Reporter config object
 * @param {Number} failures  Number of reported failures
 * @param {Function} exit
 *
 * @return {Promise} Resolves with successful report creation
 */
async function done(output, config, failures, exit) {
  try {
    const jsonFile = await create(output, config)
    if (!jsonFile) {
      log('No files were generated', 'warn', config)
    } else {
      log(`Report JSON saved to ${jsonFile}`, null, config)
    }
    exit && exit(failures > 0 ? 1 : 0)
  } catch (err) {
    log(err, 'error', config)
  }
}

/**
 * Get the class of the configured console reporter. This reporter outputs
 * test results to the console while mocha is running, and before
 * truffle-report generates its own report.
 *
 * Defaults to 'spec'.
 *
 * @param {String} reporter   Name of reporter to use for console output
 *
 * @return {Object} Reporter class object
 */
function consoleReporter(reporter) {
  if (reporter) {
    try {
      // eslint-disable-next-line import/no-dynamic-require
      return require(`mocha/lib/reporters/${reporter}`)
    } catch (e) {
      log(`Unknown console reporter '${reporter}', defaulting to spec`)
    }
  }

  return require('mocha/lib/reporters/spec')
}

/**
 * Return a plain-object representation of `suite` with additional properties for rendering.
 *
 * @param {Object} suite
 * @param {Object} totalTestsRegistered
 * @param {Integer} totalTestsRegistered.total
 *
 * @return {Object|boolean} cleaned suite or false if suite is empty
 */
function cleanSuite(suite, totalTestsRegistered) {
  let duration = 0

  const tests = suite.tests.map(test => {
    const description = stripAnsi(test.title)
    const cleanedTest = {
      uuid: test.uuid || uuid(description, UUID_NAMESPACE),
      description,
      web3: test.web3,
    }
    duration += test.duration || 0
    return cleanedTest
  })

  totalTestsRegistered.total += tests.length

  const cleaned = {
    uuid: uuid(stripAnsi(suite.title), UUID_NAMESPACE),
    title: stripAnsi(suite.title),
    file: suite.file ? suite.file.replace(process.cwd(), '') : '',
    tests,
    suites: suite.suites,
  }

  if (cleaned.title === '') delete cleaned.title
  // Clean arbitrary "Contract: " substring that truffle adds to title
  else cleaned.title = cleaned.title.replace('Contract: ', '')

  if (cleaned.file === '') delete cleaned.file
  if (cleaned.tests.length === 0) delete cleaned.tests
  if (cleaned.suites.length === 0) delete cleaned.suites

  const isEmptySuite = isEmpty(cleaned.suites) && isEmpty(cleaned.tests)

  return !isEmptySuite && cleaned
}

/**
 * Map over a suite, returning a cleaned suite object
 * and recursively cleaning any nested suites.
 *
 * @param {Object} suite          Suite to map over
 * @param {Object} totalTestsReg  Cumulative count of total tests registered
 * @param {Integer} totalTestsReg.total
 * @param {Object} config         Reporter configuration
 */
function mapSuites(suite, totalTestsReg, config) {
  const suites = suite.suites.reduce((acc, subSuite) => {
    const mappedSuites = mapSuites(subSuite, totalTestsReg, config)
    if (mappedSuites) {
      acc.push(mappedSuites)
    }
    return acc
  }, [])
  const toBeCleaned = Object.assign({}, suite, { suites })
  return cleanSuite(toBeCleaned, totalTestsReg)
}

/**
 * Initialize a new reporter.
 *
 * @param {Runner} runner
 * @param {Object} options
 * @api public
 */
function TruffleReporter(runner, options) {
  // Set the config options
  this.config = conf(options)

  // Done function will be called before mocha exits
  // This is where we will save JSON
  this.done = (failures, exit) => done(this.output, this.config, failures, exit)

  // Reset total tests counter
  totalTestsRegistered.total = 0

  // Call the Base mocha reporter
  Base.call(this, runner)

  const reporterName = this.config.consoleReporter
  if (reporterName !== 'none') {
    const ConsoleReporter = consoleReporter(reporterName)
    new ConsoleReporter(runner) // eslint-disable-line
  }

  // Generate web3 object on each test entry
  runner.on('test', item => (item.web3 = { summaries: [] }))

  // Process the full suite
  runner.on('end', () => {
    try {
      const rootSuite = mapSuites(this.runner.suite, totalTestsRegistered, this.config)

      // Save the final output to be used in the done function
      this.output = {
        stats: this.stats,
        results: rootSuite.suites,
      }
    } catch (e) {
      // required because thrown errors are not handled directly in the
      // event emitter pattern and mocha does not have an "on error"
      log(`Problem with truffle-reporter: ${e.stack}`, 'error')
    }
  })
}

module.exports = TruffleReporter
