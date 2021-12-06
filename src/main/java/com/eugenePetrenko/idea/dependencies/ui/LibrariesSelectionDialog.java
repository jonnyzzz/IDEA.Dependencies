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

import com.eugenePetrenko.idea.dependencies.data.LibOrModuleSet;
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static com.intellij.ui.SimpleTextAttributes.*;

/**
 * Created 07.04.13 17:54
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class LibrariesSelectionDialog extends DialogWrapper {
  private static final DataKey<DependencyNodeBase> DEPENDENCY_NODE = DataKey.create("jdependency_node");
  private static final DataKey<Collection<DependencyNodeBase>> DEPENDENCY_NODE_ARRAY = DataKey.create("jdependency_nodes");

  @NotNull
  private final ModulesDependencies myModel;
  @NotNull
  private final Tree myTree;

  public LibrariesSelectionDialog(@NotNull Project project, @NotNull final ModulesDependencies model) {
    super(project, true);
    myModel = model;

    myTree = new Tree(new RootNode(project, myModel));
    myTree.setRootVisible(false);
    myTree.setCellRenderer(new CellRenderer());
    init();

    TreeUtil.expandAll(myTree);
    setTitle("Detected Unnecessary Dependencies");
    TreeUtil.selectFirstNode(myTree);

    setOKButtonText("Remove Dependencies");
  }

  @NotNull
  public ModulesDependencies getModel() {
    return myModel;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    final JPanel pn = new JPanel();
    pn.setLayout(new BorderLayout());
    pn.add(new JBScrollPane(myTree), BorderLayout.CENTER);
    pn.add(new Label("Use Insert or Delete key to include/exclude or SPACE to toggle dependencies from remove"), BorderLayout.SOUTH);

    DataManager.registerDataProvider(pn, new DataProvider() {
      public Object getData(@NonNls String dataId) {
        final TreePath path = myTree.getSelectionPath();
        if (path != null) {
          Object component = path.getLastPathComponent();
          if (component instanceof ModuleHoldingNode && LangDataKeys.MODULE.is(dataId)) {
            return ((ModuleHoldingNode) component).getModule();
          }
          if (component instanceof LibraryHoldingNode && LangDataKeys.LIBRARY.is(dataId)) {
            return ((LibraryHoldingNode) component).getLibrary();
          }

          if (component instanceof DependencyNodeBase && DEPENDENCY_NODE.is(dataId)) {
            return component;
          }
        }

        if (DEPENDENCY_NODE_ARRAY.is(dataId)) {
          Collection<DependencyNodeBase> result = new ArrayList<DependencyNodeBase>();
          for (TreePath treePath : myTree.getSelectionModel().getSelectionPaths()) {
            Object obj = treePath.getLastPathComponent();
            if (obj instanceof DependencyNodeBase) {
              result.add((DependencyNodeBase) obj);
            }
            if (obj instanceof ModuleNode) {
              result.addAll(((ModuleNode) obj).getChildren());
            }
          }
          if (!result.isEmpty()) return result;
        }

        return null;
      }
    });

    new AnAction("Exclude") {
      @Override
      public void actionPerformed(AnActionEvent e) {
        Collection<DependencyNodeBase> nodes = e.getData(DEPENDENCY_NODE_ARRAY);
        if (nodes == null) return;

        for (DependencyNodeBase node : nodes) {
          node.setRemoved(true);
        }
        myTree.updateUI();
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE"), pn);
    new AnAction("Include") {
      @Override
      public void actionPerformed(AnActionEvent e) {
        Collection<DependencyNodeBase> nodes = e.getData(DEPENDENCY_NODE_ARRAY);
        if (nodes == null) return;

        for (DependencyNodeBase node : nodes) {
          node.setRemoved(false);
        }
        myTree.updateUI();
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("INSERT"), pn);
    new AnAction("Toggle") {
      @Override
      public void actionPerformed(AnActionEvent e) {
        Collection<DependencyNodeBase> nodes = e.getData(DEPENDENCY_NODE_ARRAY);
        if (nodes == null) return;

        for (DependencyNodeBase node : nodes) {
          node.toggle();
        }
        myTree.updateUI();
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("SPACE"), pn);

    return pn;
  }

  private class RootNode extends DefaultMutableTreeNode {
    @NotNull
    private final ModulesDependencies myModel;

    private RootNode(@NotNull final Project project, @NotNull final ModulesDependencies model) {
      myModel = model;
      final Vector<TreeNode> children = new Vector<>();
      final Module[] modules = ModuleManager.getInstance(project).getSortedModules();
      Arrays.sort(modules, Comparators.MODULE_COMPARATOR);
      for (Module module : modules) {
        final LibOrModuleSet filter = myModel.forModule(module);
        if (filter == null) continue;
        children.add(new ModuleNode(module, filter));
      }
      this.children = children;
    }
  }

  private interface ModuleHoldingNode {
    @NotNull
    Module getModule();
  }

  private interface LibraryHoldingNode {
    @NotNull
    Library getLibrary();
  }

  private class ModuleNode extends DefaultMutableTreeNode implements ModuleHoldingNode {
    private final Module myModule;
    private final Collection<DependencyNodeBase> myChildren = new ArrayList<DependencyNodeBase>();

    private ModuleNode(@NotNull Module module, @NotNull final LibOrModuleSet libOrModuleSet) {
      myModule = module;
      ModuleRootManager.getInstance(module).processOrder(new RootPolicy<Void>() {
        @Override
        public Void visitModuleOrderEntry(@NotNull ModuleOrderEntry moduleOrderEntry, Void value) {
          final Module mod = moduleOrderEntry.getModule();
          if (mod == null) return null;
          addNode(new DependencyModuleNode(libOrModuleSet, moduleOrderEntry, mod));
          return null;
        }

        @Override
        public Void visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Void value) {
          final Library lib = libraryOrderEntry.getLibrary();
          if (lib == null) return null;
          addNode(new DependencyLibNode(libOrModuleSet, libraryOrderEntry, lib));

          return null;
        }

        private void addNode(@NotNull DependencyNodeBase e) {
          if (e.intersects()) {
            myChildren.add(e);
          }
        }
      }, null);
      children = new Vector(myChildren);
    }

    @NotNull
    public Collection<DependencyNodeBase> getChildren() {
      return myChildren;
    }

    @NotNull
    public Module getModule() {
      return myModule;
    }
  }

  private interface DependencyNode {
    @NotNull
    DependencyScope getDependencyScope();
  }

  private interface RemovableNode {
    boolean isRemoved();
  }

  private abstract class DependencyNodeBase<T extends ExportableOrderEntry> extends DefaultMutableTreeNode implements RemovableNode, DependencyNode {
    protected final LibOrModuleSet myFilter;
    protected final T myEntry;
    private boolean myIsRemoved;

    protected DependencyNodeBase(@NotNull final LibOrModuleSet filter, @NotNull final T entry) {
      myFilter = filter;
      myEntry = entry;
    }

    public boolean isRemoved() {
      return myIsRemoved;
    }

    public void setRemoved(boolean isRemoved) {
      myIsRemoved = isRemoved;
    }

    public void toggle() {
      setRemoved(!isRemoved());
    }

    @NotNull
    public DependencyScope getDependencyScope() {
      return myEntry.getScope();
    }

    public abstract boolean intersects();
  }

  private class DependencyModuleNode extends DependencyNodeBase<ModuleOrderEntry> implements ModuleHoldingNode {
    @NotNull
    private final Module myModule;

    public DependencyModuleNode(@NotNull LibOrModuleSet filter, @NotNull ModuleOrderEntry entry, @NotNull Module module) {
      super(filter, entry);
      myModule = module;
    }

    @NotNull
    public Module getModule() {
      return myModule;
    }

    @Override
    public void setRemoved(boolean isRemoved) {
      super.setRemoved(isRemoved);
      if (isRemoved) {
        myFilter.removeDependency(getModule());
      } else {
        myFilter.addDependency(getModule());
      }
    }

    @Override
    public boolean intersects() {
      return myFilter.contains(getModule());
    }
  }

  private class DependencyLibNode extends DependencyNodeBase<LibraryOrderEntry> implements LibraryHoldingNode {
    private final Library myLib;

    private DependencyLibNode(@NotNull LibOrModuleSet filter, @NotNull LibraryOrderEntry entry, @NotNull Library library) {
      super(filter, entry);
      myLib = library;
    }

    @NotNull
    public Library getLibrary() {
      return myLib;
    }

    @Override
    public boolean intersects() {
      return myFilter.contains(getLibrary());
    }

    @Override
    public void setRemoved(boolean isRemoved) {
      super.setRemoved(isRemoved);
      if (isRemoved) {
        myFilter.removeDependency(getLibrary());
      } else {
        myFilter.addDependency(getLibrary());
      }
    }
  }

  private class CellRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      @SimpleTextAttributes.StyleAttributeConstant int textType = STYLE_PLAIN;
      if (value instanceof DependencyNodeBase) {
        final DependencyNodeBase node = (DependencyNodeBase) value;
        if (node.isRemoved()) {
          textType |= STYLE_STRIKEOUT;
        }
      } else {
        textType |= STYLE_BOLD;
      }

      if (value instanceof DependencyNode) {
        DependencyScope scope = ((DependencyNode) value).getDependencyScope();
        append("[");
        append(scope.getDisplayName(), new SimpleTextAttributes(STYLE_BOLD, UIUtil.getTreeForeground()));
        append("] ");
      }

      if (value instanceof ModuleHoldingNode) {
        final ModuleHoldingNode node = (ModuleHoldingNode) value;
        Module module = node.getModule();
        setIcon(ModuleType.get(module).getIcon());
        append(module.getName() + " ", new SimpleTextAttributes(textType, UIUtil.getTreeForeground()));
      }

      if (value instanceof LibraryHoldingNode) {
        final LibraryHoldingNode node = (LibraryHoldingNode) value;
        final Library lib = node.getLibrary();

        setIcon(LibraryPresentationManager.getInstance().getNamedLibraryIcon(lib, null));
        append(lib.getName() + " ", new SimpleTextAttributes(textType, UIUtil.getTreeForeground()));
      }
    }
  }
}
