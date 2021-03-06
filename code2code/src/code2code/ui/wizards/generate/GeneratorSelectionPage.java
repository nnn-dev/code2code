package code2code.ui.wizards.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import code2code.Activator;
import code2code.core.generator.Generator;
import code2code.core.generator.GeneratorFactory;
import code2code.preferences.PreferenceConstants;
import code2code.utils.EclipseGuiUtils;

public class GeneratorSelectionPage extends WizardPage {

	private Generator selectedGenerator;
	private final IProject project;

	@Override
	public boolean isPageComplete() {
		return getSelectedGenerator() != null;
	}

	public GeneratorSelectionPage(IProject project) {
		super("Generator Selection", "Select a Generator", null);
		this.project = project;
		setPageComplete(false);
	}

	public void createControl(Composite parent) {

		Composite container = null;
		ScrolledComposite scrolledComposite = null;
		
		Set<Generator> generators;
		try {
			generators = GeneratorFactory.fromProject(project);
		} catch (Exception e1) {
			EclipseGuiUtils.showErrorDialog(parent.getShell(), e1);
			throw new RuntimeException(e1);
		}

		if (generators.isEmpty()) {
			scrolledComposite = new ScrolledComposite(parent,
					SWT.H_SCROLL | SWT.V_SCROLL);
			container = new Composite(scrolledComposite, SWT.NONE);
			scrolledComposite.setContent(container);

			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			container.setLayout(layout);
			Label label = new Label(container, SWT.NONE);
			label.setText("No generators found.");
		} else

		if ("tree".equals(Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.GUI_STYLE))) {
			container = new Composite(parent, SWT.NONE);

			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			container.setLayout(layout);

			final Map<String, TreeItem> generatorItems = new HashMap<String, TreeItem>();
			final Tree tree = new Tree(container, SWT.BORDER | SWT.V_SCROLL
					| SWT.H_SCROLL);
			tree.setLayoutData(new GridData(GridData.FILL_BOTH));
			tree.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					if (event.detail != SWT.CHECK) {
						selectedGenerator = (Generator) ((TreeItem) event.item)
								.getData("generator");
						setPageComplete(true);
					}
				}
			});
			tree.setHeaderVisible(true);
			TreeColumn column1 = new TreeColumn(tree, SWT.CENTER);
			column1.setText("Generator");
			column1.setWidth(300);
			
			TreeColumn column2 = new TreeColumn(tree, SWT.RIGHT);
			column2.setText("Description");
			column2.setWidth(500);
			
			for (Generator generator : generators) {
				TreeItem tparent = null;
				String name = generator.getName();
				String[] namep = name.split(Pattern.quote("/"));
				for (int i = 0; i < namep.length - 1; i++) {
					String nameps = namep[i];
					if (generatorItems.containsKey(nameps)) {
						tparent = generatorItems.get(nameps);
					} else {
						tparent = createTreeItem(tree, tparent, nameps,"");
						generatorItems.put(nameps, tparent);
					}
				}
				TreeItem item = createTreeItem(tree, tparent, namep[namep.length-1],generator.getDescription());
				item.setData("generator", generator);
				generatorItems.put(name, item);
				if (tparent != null) {
					tparent.setExpanded(true);
				}
			}
		} else {
			scrolledComposite = new ScrolledComposite(parent,
					SWT.H_SCROLL | SWT.V_SCROLL);
			container = new Composite(scrolledComposite, SWT.NONE);
			scrolledComposite.setContent(container);

			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			container.setLayout(layout);

			final List<Button> generatorButtons = new ArrayList<Button>();

			for (Generator generator : generators) {

				Button button = new Button(container, SWT.RADIO);
				button.setText(generator.getName());
				button.setData("generator", generator);
				button.setToolTipText(generator.getDescription());

				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						selectedGenerator = (Generator) ((Button) e.getSource())
								.getData("generator");
						setPageComplete(true);
					}
				});

				generatorButtons.add(button);
			}
		}
		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (scrolledComposite != null) {
			setControl(scrolledComposite);
		} else {
			setControl(container);
		}
	}

	private TreeItem createTreeItem(Tree t, TreeItem tparent, String name, String description) {
		TreeItem res;
		if (tparent == null) {
			res = new TreeItem(t, 0);
		} else {
			res = new TreeItem(tparent, 0);
		}
		res.setText(new String[]{name,description});
		return res;
	}

	public Generator getSelectedGenerator() {
		return selectedGenerator;
	}

}
