/**
 * @license
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const SAFE_URL_PATTERN = /^(https?:\/\/|mailto:|[^:/?#]*(?:[/?#]|$))/i;

/** @polymerBehavior Gerrit.SafeTypes */
export const SafeTypes = {};

/**
 * Wraps a string to be used as a URL. An error is thrown if the string cannot
 * be considered safe.
 *
 * @constructor
 * @param {string} url the unwrapped, potentially unsafe URL.
 */
SafeTypes.SafeUrl = function(url) {
  if (!SAFE_URL_PATTERN.test(url)) {
    throw new Error(`URL not marked as safe: ${url}`);
  }
  this._url = url;
};

/**
 * Get the string representation of the safe URL.
 *
 * @returns {string}
 */
SafeTypes.SafeUrl.prototype.asString = function() {
  return this._url;
};

SafeTypes.safeTypesBridge = function(value, type) {
  // If the value is being bound to a URL, ensure the value is wrapped in the
  // SafeUrl type first. If the URL is not safe, allow the SafeUrl constructor
  // to surface the error.
  if (type === 'URL') {
    let safeValue = null;
    if (value instanceof SafeTypes.SafeUrl) {
      safeValue = value;
    } else if (typeof value === 'string') {
      safeValue = new SafeTypes.SafeUrl(value);
    }
    if (safeValue) {
      return safeValue.asString();
    }
  }

  // If the value is being bound to a string or a constant, then the string
  // can be used as is.
  if (type === 'STRING' || type === 'CONSTANT') {
    return value;
  }

  // Otherwise fail.
  throw new Error(`Refused to bind value as ${type}: ${value}`);
};

// TODO(dmfilippov) Remove the following lines with assignments
// Plugins can use the behavior because it was accessible with
// the global Gerrit... variable. To avoid breaking changes in plugins
// temporary assign global variables.
window.Gerrit = window.Gerrit || {};
window.Gerrit.SafeTypes = SafeTypes;

