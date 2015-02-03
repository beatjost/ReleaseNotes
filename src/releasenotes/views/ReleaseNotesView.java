package releasenotes.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import releasenotes.Activator;
import releasenotes.preferences.PreferenceConstants;
import releasenotes.preferences.ReleaseNotePreferencePage;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be presented in the view. Each view can present the
 * same model objects using different labels and icons, if needed. Alternatively, a single label provider can be shared
 * between views in order to ensure that objects of the same type are presented in the same way everywhere.
 * <p>
 */

public class ReleaseNotesView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "releasenotes.views.ReleaseNotesView";

	private TableViewer viewer;
	private Action actionNew;
	private Action actionRefresh;
	private Action actionPreference;
	private Action doubleClickAction;
	private Action actionEditFileName;
	private Action actionDeleteFile;

	/*
	 * The content provider class is responsible for providing objects to the view. It can wrap existing
	 * objects in adapters or simply return objects as-is. These objects may be sensitive to the current
	 * input of the view, or ignore it and always show the same content (like Task List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return getFileNames();
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		}
	}

	class NameSorter extends ViewerSorter {
	}

	public ReleaseNotesView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionNew);
		manager.add(actionRefresh);
		manager.add(actionPreference);
	}

	private void fillContextMenu(IMenuManager mgr) {
		mgr.add(actionEditFileName);
		mgr.add(actionDeleteFile);
		//mgr.add(new Separator());
	}

	private void makeActions() {

		// -------------------------------------------------------------------------------------------------------

		actionNew = new Action() {
			public void run() {
				InputDialog dialog = new InputDialog(getViewSite().getShell(), "New file",
						"Enter a release note file name (e.g. 'projectname_release#')", "", null);
				if (dialog.open() == IStatus.OK) {
					StringBuilder filePath = new StringBuilder();
					String path = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH);
					String fileName = dialog.getValue();
					filePath.append(path).append("\\").append(fileName).append(".txt");
					File file = new File(filePath.toString());
					try {
						file.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
					viewer.refresh();
				}
			}
		};
		actionNew.setText("New");
		actionNew.setToolTipText("Create new releasenote");
		actionNew.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_ADD));

		// -------------------------------------------------------------------------------------------------------

		actionRefresh = new Action() {
			public void run() {
				viewer.refresh();
			}
		};
		actionRefresh.setText("Refresh");
		actionRefresh.setToolTipText("Refresh the list.");
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin("ReleaseNotes", "icons/refresh.gif");
		actionRefresh.setImageDescriptor(icon);

		// -------------------------------------------------------------------------------------------------------

		actionPreference = new Action() {
			public void run() {
				openPreferencePage();
			}
		};
		actionPreference.setText("Preferences");
		actionPreference.setToolTipText("Set preferences.");
		actionPreference.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		// -------------------------------------------------------------------------------------------------------

		doubleClickAction = new Action() {
			public void run() {
				openFile();
			}
		};

		// -------------------------------------------------------------------------------------------------------

		actionEditFileName = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				String fileName = obj.toString();
				InputDialog dialog = new InputDialog(getViewSite().getShell(), "Edit filename", "", fileName, null);
				if (dialog.open() == IStatus.OK) {
					StringBuilder filePath = new StringBuilder();
					String path = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH);
					filePath.append(path).append("\\");
					File file = new File(filePath.toString().concat(fileName));
					if (file.exists()) {
						file.renameTo(new File(filePath.toString().concat(dialog.getValue())));
					}
					viewer.refresh();
				}
			}
		};
		actionEditFileName.setText("Rename");
		actionEditFileName.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		// -------------------------------------------------------------------------------------------------------

		actionDeleteFile = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				String fileName = obj.toString();
				MessageDialog dialog = new MessageDialog(getViewSite().getShell(), "Delete file", null,
						"Do you really want to delete file '" + fileName + "'", MessageDialog.CONFIRM, new String[] {
								"Delete", "Cancel" }, 1);
				if (dialog.open() == IStatus.OK) {
					String path = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH);
					File file = new File(path.concat("\\" + fileName));
					file.delete();
					viewer.refresh();
				}
			}
		};
		actionDeleteFile.setText("Delete");
		actionDeleteFile.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}

	private void openFile() {
		ISelection selection = viewer.getSelection();
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		StringBuilder filePath = new StringBuilder();
		String path = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH);
		String fileName = obj.toString();
		filePath.append(path).append("\\").append(fileName);
		File fileToOpen = new File(filePath.toString());

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		IPath location = Path.fromOSString(fileToOpen.getAbsolutePath());
		IFileStore ifile = EFS.getLocalFileSystem().getStore(location);
		try {
			IDE.openEditorOnFileStore(page, ifile);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void openPreferencePage() {
		IPreferencePage page = new ReleaseNotePreferencePage();
		PreferenceManager mgr = new PreferenceManager();
		IPreferenceNode node = new PreferenceNode("1", page);
		mgr.addToRoot(node);
		PreferenceDialog dialog = new PreferenceDialog(getViewSite().getShell(), mgr);
		dialog.create();
		dialog.setMessage(page.getTitle());
		dialog.open();
	}

	private File[] getFilesOfFolder(String path) {
		File folder = new File(path);
		return folder.listFiles() == null ? new File[0] : folder.listFiles();
	}

	private String[] getFileNames() {
		String path = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH);
		File[] listOfFiles = getFilesOfFolder(path);
		String files;
		List<String> fileNames = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {
				files = listOfFiles[i].getName();
				if (files.endsWith(".txt") || files.endsWith(".TXT")) {
					fileNames.add(files);
				}
			}
		}
		int size = fileNames == null ? 0 : fileNames.size();
		return (String[]) fileNames.toArray(new String[size]);
	}
}