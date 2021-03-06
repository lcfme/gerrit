/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
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

/**
 * @enum
 * @desc Experiment ids used in Gerrit.
 */
export const ExperimentIds = {
  CLEANER_CHANGELOG: 'UiFeature__cleaner_changelog',
};

/**
 * Flags service.
 *
 * Provides all related methods / properties regarding on feature flags.
 */
export class FlagsService {
  constructor() {
    // stores all enabled experiments
    this._experiments = new Set();
    this._loadExperiments();
  }

  /**
   * @param {string} experimentId
   * @returns {boolean}
   */
  isEnabled(experimentId) {
    return this._experiments.has(experimentId);
  }

  _loadExperiments() {
    this._experiments = new Set(window.ENABLED_EXPERIMENTS);
  }

  /**
   * @returns {string[]} array of all enabled experiments.
   */
  get enabledExperiments() {
    return [...this._experiments];
  }
}
