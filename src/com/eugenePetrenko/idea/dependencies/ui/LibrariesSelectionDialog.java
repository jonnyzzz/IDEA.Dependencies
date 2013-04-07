package com.eugenePetrenko.idea.dependencies.ui;

import com.eugenePetrenko.idea.dependencies.DependenciesFilter;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.RootPolicy;
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
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

import static com.intellij.ui.SimpleTextAttributes.*;

/**
 * Created 07.04.13 17:54
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class LibrariesSelectionDialog extends DialogWrapper {
  @NotNull
  private final Project myProject;
  @NotNull
  private final Tree myTree;

  public LibrariesSelectionDialog(@NotNull Project project) {
    super(project, true);
    myProject = project;

    myTree = new Tree(new RootNode(myProject));
    myTree.setRootVisible(false);
    myTree.setCellRenderer(new CellRenderer());
    init();

    TreeUtil.expandAll(myTree);
    setTitle("Detected Unnecessary Dependencies");
    TreeUtil.selectFirstNode(myTree);

    setOKButtonText("Remove Dependencies");
  }

  private static final DataKey<DependencyNodeBase> DEPENDENCY_NODE = DataKey.create("jdependency_node");
  private static final DataKey<Collection<DependencyNodeBase>> DEPENDENCY_NODE_ARRAY = DataKey.create("jdependency_nodes");

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    final JPanel pn = new JPanel();
    pn.setLayout(new BorderLayout());
    pn.add(new JBScrollPane(myTree), BorderLayout.CENTER);
    pn.add(new Label("Use Insert or Delete key to include/exclude dependencies from remove"), BorderLayout.SOUTH);

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
              result.add((DependencyNodeBase)obj);
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

    return pn;
  }

  private class RootNode extends DefaultMutableTreeNode {
    private final Project myProject;
    private RootNode(@NotNull Project project) {
      myProject = project;
      children = new Vector();
      Module[] modules = ModuleManager.getInstance(project).getSortedModules();
      Arrays.sort(modules, MODULE_COMPARATOR);
      for (Module module : modules) {
        //noinspection unchecked
        children.add(new ModuleNode(module));
      }
    }
  }

  private interface ModuleHoldingNode {
    @NotNull Module getModule();
  }

  private interface LibraryHoldingNode {
    @NotNull Library getLibrary();
  }

  private class ModuleNode extends DefaultMutableTreeNode implements ModuleHoldingNode {
    private final Module myModule;
    private final Collection<DependencyNodeBase> myChildren = new ArrayList<DependencyNodeBase>();

    private ModuleNode(@NotNull Module module) {
      myModule = module;
      ModuleRootManager.getInstance(module).processOrder(new RootPolicy<Void>(){
        @Override
        public Void visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Void value) {
          if (!DependenciesFilter.REMOVABLE_DEPENDENCY.apply(moduleOrderEntry)) return null;

          final Module mod = moduleOrderEntry.getModule();
          if (mod == null) return null;
          //noinspection unchecked
          myChildren.add(new DependencyModuleNode(mod));

          return null;
        }

        @Override
        public Void visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Void value) {
          if (!DependenciesFilter.REMOVABLE_DEPENDENCY.apply(libraryOrderEntry)) return null;
          Library lib = libraryOrderEntry.getLibrary();
          if (lib == null) return null;
          //noinspection unchecked
          myChildren.add(new DependencyLibNode(lib));

          return null;
        }
      }, null);
      children = new Vector<DependencyNodeBase>(myChildren);
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

  private abstract class DependencyNodeBase extends DefaultMutableTreeNode {
    private boolean myIsRemoved;

    public boolean isRemoved() {
      return myIsRemoved;
    }

    public void setRemoved(boolean isRemoved) {
      myIsRemoved = isRemoved;
    }
  }

  private class DependencyModuleNode extends DependencyNodeBase implements ModuleHoldingNode {
    private final Module myModule;

    private DependencyModuleNode(@NotNull Module module) {
      myModule = module;
    }

    @NotNull
    public Module getModule() {
      return myModule;
    }
  }

  private class DependencyLibNode extends DependencyNodeBase implements LibraryHoldingNode {
    private final Library myLib;

    private DependencyLibNode(@NotNull Library myModule) {
      myLib = myModule;
    }

    @NotNull
    public Library getLibrary() {
      return myLib;
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

      if (value instanceof ModuleHoldingNode) {
        final ModuleHoldingNode node = (ModuleHoldingNode) value;
        Module module = node.getModule();
        setIcon(ModuleType.get(module).getIcon());
        append("[" + module.getName() + "] ", new SimpleTextAttributes(textType, UIUtil.getTreeForeground()));
      }

      if (value instanceof LibraryHoldingNode) {
        final LibraryHoldingNode node = (LibraryHoldingNode) value;
        final Library lib = node.getLibrary();

        setIcon(LibraryPresentationManager.getInstance().getNamedLibraryIcon(lib, null));
        append("[" + lib.getName() + "] ", new SimpleTextAttributes(textType, UIUtil.getTreeForeground()));
      }
    }
  }

  private final Comparator<Module> MODULE_COMPARATOR = new Comparator<Module>() {
    public int compare(Module o1, Module o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };
}
