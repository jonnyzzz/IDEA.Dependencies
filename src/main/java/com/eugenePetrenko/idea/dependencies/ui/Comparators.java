/*
 * Copyright 2013-2013 Eugene Petrenko
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

package com.eugenePetrenko.idea.dependencies.ui;

import com.intellij.openapi.module.Module;

import java.util.Comparator;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:45
 */
public class Comparators {
  public static final Comparator<Module> MODULE_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());
}
