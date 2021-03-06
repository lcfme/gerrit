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
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

export const htmlTemplate = html`
  <style include="shared-styles">
    .chip {
      border-radius: var(--border-radius);
      background-color: var(--chip-background-color);
      padding: 0 var(--spacing-m);
      white-space: nowrap;
    }
    :host(.merged) .chip {
      background-color: #5b9d52;
      color: #5b9d52;
    }
    :host(.abandoned) .chip {
      background-color: #afafaf;
      color: #afafaf;
    }
    :host(.wip) .chip {
      background-color: #8f756c;
      color: #8f756c;
    }
    :host(.private) .chip {
      background-color: #c17ccf;
      color: #c17ccf;
    }
    :host(.merge-conflict) .chip {
      background-color: #dc5c60;
      color: #dc5c60;
    }
    :host(.active) .chip {
      background-color: #29b6f6;
      color: #29b6f6;
    }
    :host(.ready-to-submit) .chip {
      background-color: #e10ca3;
      color: #e10ca3;
    }
    :host(.custom) .chip {
      background-color: #825cc2;
      color: #825cc2;
    }
    :host([flat]) .chip {
      background-color: transparent;
      padding: 0;
    }
    :host(:not([flat])) .chip {
      color: white;
    }
  </style>
  <gr-tooltip-content
    has-tooltip=""
    position-below=""
    title="[[tooltipText]]"
    max-width="40em"
  >
    <div class="chip" aria-label$="Label: [[status]]">
      [[_computeStatusString(status)]]
    </div>
  </gr-tooltip-content>
`;
