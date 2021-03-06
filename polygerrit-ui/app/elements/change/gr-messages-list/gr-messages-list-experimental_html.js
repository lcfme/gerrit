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
    :host {
      display: flex;
      justify-content: space-between;
    }
    .header {
      align-items: center;
      border-top: 1px solid var(--border-color);
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      padding: var(--spacing-s) var(--spacing-l);
    }
    .highlighted {
      animation: 3s fadeOut;
    }
    @keyframes fadeOut {
      0% {
        background-color: var(--emphasis-color);
      }
      100% {
        background-color: var(--view-background-color);
      }
    }
    .container {
      align-items: center;
      display: flex;
    }
    gr-message:not(:last-of-type) {
      border-bottom: 1px solid var(--border-color);
    }
    gr-message {
      background-color: var(--background-color-secondary);
    }
  </style>
  <div class="header">
    <span
      id="showAllActivityToggleContainer"
      class="container"
      hidden$="[[!_isVisibleShowAllActivityToggle(_combinedMessages)]]"
    >
      <paper-toggle-button
        id="showAllActivityToggle"
        checked="{{_showAllActivity}}"
      ></paper-toggle-button
      >Show all entries
      <span class="transparent separator"></span>
    </span>
    <gr-button
      id="collapse-messages"
      link=""
      title="[[_expandAllTitle]]"
      on-click="_handleExpandCollapseTap"
    >
      [[_expandAllState]]
    </gr-button>
  </div>
  <template
    id="messageRepeat"
    is="dom-repeat"
    items="[[_combinedMessages]]"
    as="message"
    filter="_isMessageVisible"
  >
    <gr-message
      change-num="[[changeNum]]"
      message="[[message]]"
      comment-threads="[[_computeThreadsForMessage(changeComments, message)]]"
      project-name="[[projectName]]"
      show-reply-button="[[showReplyButtons]]"
      on-message-anchor-tap="_handleAnchorClick"
      label-extremes="[[_labelExtremes]]"
      data-message-id$="[[message.id]]"
    ></gr-message>
  </template>
`;
