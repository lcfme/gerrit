// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.project;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.InheritableBoolean;
import com.google.gerrit.reviewdb.client.Project.SubmitType;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.git.TransferConfig;
import com.google.gerrit.server.project.PutConfig.Input;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class PutConfig implements RestModifyView<ProjectResource, Input> {
  private static final Logger log = LoggerFactory.getLogger(PutConfig.class);

  public static class Input {
    public String description;
    public InheritableBoolean useContributorAgreements;
    public InheritableBoolean useContentMerge;
    public InheritableBoolean useSignedOffBy;
    public InheritableBoolean requireChangeId;
    public String maxObjectSizeLimit;
    public SubmitType submitType;
    public Project.State state;
    public Map<String, Map<String, String>> pluginConfigValues;
  }

  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final ProjectCache projectCache;
  private final Provider<CurrentUser> self;
  private final ProjectState.Factory projectStateFactory;
  private final TransferConfig config;
  private final DynamicMap<ProjectConfigEntry> pluginConfigEntries;
  private final PluginConfigFactory cfgFactory;
  private final DynamicMap<RestView<ProjectResource>> views;
  private final Provider<CurrentUser> currentUser;

  @Inject
  PutConfig(MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      Provider<CurrentUser> self,
      ProjectState.Factory projectStateFactory,
      TransferConfig config,
      DynamicMap<ProjectConfigEntry> pluginConfigEntries,
      PluginConfigFactory cfgFactory,
      DynamicMap<RestView<ProjectResource>> views,
      Provider<CurrentUser> currentUser) {
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.projectCache = projectCache;
    this.self = self;
    this.projectStateFactory = projectStateFactory;
    this.config = config;
    this.pluginConfigEntries = pluginConfigEntries;
    this.cfgFactory = cfgFactory;
    this.views = views;
    this.currentUser = currentUser;
  }

  @Override
  public ConfigInfo apply(ProjectResource rsrc, Input input)
      throws ResourceNotFoundException, BadRequestException,
      ResourceConflictException {
    Project.NameKey projectName = rsrc.getNameKey();
    if (!rsrc.getControl().isOwner()) {
      throw new ResourceNotFoundException(projectName.get());
    }

    if (input == null) {
      throw new BadRequestException("config is required");
    }

    final MetaDataUpdate md;
    try {
      md = metaDataUpdateFactory.create(projectName);
    } catch (RepositoryNotFoundException notFound) {
      throw new ResourceNotFoundException(projectName.get());
    } catch (IOException e) {
      throw new ResourceNotFoundException(projectName.get(), e);
    }
    try {
      ProjectConfig projectConfig = ProjectConfig.read(md);
      Project p = projectConfig.getProject();

      p.setDescription(Strings.emptyToNull(input.description));

      if (input.useContributorAgreements != null) {
        p.setUseContributorAgreements(input.useContributorAgreements);
      }
      if (input.useContentMerge != null) {
        p.setUseContentMerge(input.useContentMerge);
      }
      if (input.useSignedOffBy != null) {
        p.setUseSignedOffBy(input.useSignedOffBy);
      }
      if (input.requireChangeId != null) {
        p.setRequireChangeID(input.requireChangeId);
      }

      if (input.maxObjectSizeLimit != null) {
        p.setMaxObjectSizeLimit(input.maxObjectSizeLimit);
      }

      if (input.submitType != null) {
        p.setSubmitType(input.submitType);
      }

      if (input.state != null) {
        p.setState(input.state);
      }

      if (input.pluginConfigValues != null) {
        setPluginConfigValues(projectConfig, input.pluginConfigValues);
      }

      md.setMessage("Modified project settings\n");
      try {
        projectConfig.commit(md);
        (new PerRequestProjectControlCache(projectCache, self.get()))
            .evict(projectConfig.getProject());
      } catch (IOException e) {
        if (e.getCause() instanceof ConfigInvalidException) {
          throw new ResourceConflictException("Cannot update " + projectName
              + ": " + e.getCause().getMessage());
        } else {
          throw new ResourceConflictException("Cannot update " + projectName);
        }
      }

      ProjectState state = projectStateFactory.create(projectConfig);
      return new ConfigInfo(
          state.controlFor(currentUser.get()),
          config, pluginConfigEntries, cfgFactory, views);
    } catch (ConfigInvalidException err) {
      throw new ResourceConflictException("Cannot read project " + projectName, err);
    } catch (IOException err) {
      throw new ResourceConflictException("Cannot update project " + projectName, err);
    } finally {
      md.close();
    }
  }

  private void setPluginConfigValues(ProjectConfig projectConfig,
      Map<String, Map<String, String>> pluginConfigValues)
      throws BadRequestException {
    for (Entry<String, Map<String, String>> e : pluginConfigValues.entrySet()) {
      String pluginName = e.getKey();
      PluginConfig cfg = projectConfig.getPluginConfig(pluginName);
      for (Entry<String, String> v : e.getValue().entrySet()) {
        ProjectConfigEntry projectConfigEntry =
            pluginConfigEntries.get(pluginName, v.getKey());
        if (projectConfigEntry != null) {
          if (!isValidParameterName(v.getKey())) {
            log.warn(String.format(
                "Parameter name '%s' must match '^[a-zA-Z0-9]+[a-zA-Z0-9-]*$'", v.getKey()));
            continue;
          }
          if (v.getValue() != null) {
            try {
              switch (projectConfigEntry.getType()) {
                case BOOLEAN:
                  cfg.setBoolean(v.getKey(), Boolean.parseBoolean(v.getValue()));
                  break;
                case INT:
                  cfg.setInt(v.getKey(), Integer.parseInt(v.getValue()));
                  break;
                case LONG:
                  cfg.setLong(v.getKey(), Long.parseLong(v.getValue()));
                  break;
                case STRING:
                  cfg.setString(v.getKey(), v.getValue());
                  break;
                default:
                  log.warn(String.format(
                      "The type '%s' of parameter '%s' is not supported.",
                      projectConfigEntry.getType().name(), v.getKey()));
              }
            } catch (NumberFormatException ex) {
              throw new BadRequestException(String.format(
                  "The value '%s' of config parameter '%s' of plugin '%s' is invalid: %s",
                  v.getValue(), v.getKey(), pluginName, ex.getMessage()));
            }
          } else {
            cfg.unset(v.getKey());
          }
        } else {
          throw new BadRequestException(String.format(
              "The config parameter '%s' of plugin '%s' does not exist.",
              v.getKey(), pluginName));
        }
      }
    }
  }

  private static boolean isValidParameterName(String name) {
    return CharMatcher.JAVA_LETTER_OR_DIGIT
        .or(CharMatcher.is('-'))
        .matchesAllOf(name) && !name.startsWith("-");
  }
}
