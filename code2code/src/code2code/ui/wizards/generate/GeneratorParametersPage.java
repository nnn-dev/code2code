package code2code.ui.wizards.generate;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import code2code.core.generator.Generator;
import code2code.utils.EclipseGuiUtils;

public class GeneratorParametersPage extends WizardPage {

	private static final String TOOLTIP = "tooltip=";
	private Composite container;
	private Composite paramsContainer;
	List<Text> paramsTexts;
	private final GeneratorSelectionPage generatorSelectionPage;
	private Generator selectedGenerator;

	public GeneratorParametersPage(GeneratorSelectionPage generatorSelectionPage) {
		super("Generator Parameters", "Configure Params", null);
		this.generatorSelectionPage = generatorSelectionPage;
		setPageComplete(false);
	}

	public void createControl(Composite parent) {

		ScrolledComposite scrolledComposite = new ScrolledComposite(parent,
				SWT.H_SCROLL | SWT.V_SCROLL);
		container = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(container);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);

		setControl(scrolledComposite);
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			try {
				recreatePageIfNecessary();
			} catch (Exception e) {
				EclipseGuiUtils.showErrorDialog(container.getShell(), e);
				throw new RuntimeException(e);
			}
		}

		super.setVisible(visible);
	}

	private void recreatePageIfNecessary() throws Exception {

		if (!pageIsCreated() || hasGeneratorChanged()) {

			selectedGenerator = generatorSelectionPage.getSelectedGenerator();

			if (paramsContainer != null) {
				paramsContainer.dispose();
			}

			createPage();
		}

	}

	private boolean hasGeneratorChanged() {
		return selectedGenerator != generatorSelectionPage
				.getSelectedGenerator();
	}

	private boolean pageIsCreated() {
		return paramsContainer != null;
	}

	private void createPage() throws Exception {

		paramsTexts = new ArrayList<Text>();

		paramsContainer = new Composite(container, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		paramsContainer.setLayout(layout);

		Map<String, String> params = generatorSelectionPage
				.getSelectedGenerator().calculateRequiredParams();

		setDescription(generatorSelectionPage.getSelectedGenerator()
				.getDescription());

		if (params.size() > 0) {
			createParamsTexts(params);
		} else {

			Label label = new Label(paramsContainer, SWT.NONE);
			label.setText("This Generator has no params to configure");

		}

		Button load = new Button(paramsContainer, SWT.NONE);
		load.setText("Load");
		load.setToolTipText("Load from previous save");
		load.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				String filename;
				try {
					filename = EclipseGuiUtils.openFileDialog(
							GeneratorParametersPage.this.getShell(),
							"Generator params", "*.generatorparams");
					if (filename != null) {
						java.util.Properties p = new java.util.Properties();
						p.load(new FileReader(filename));
						for (Text text : paramsTexts) {
							final String key = (String) text
									.getData("paramName");
							if (p.containsKey(key)) {
								text.setText(p.getProperty(key));
							}
						}
					}
				} catch (Exception e1) {
					EclipseGuiUtils.showErrorDialog(
							GeneratorParametersPage.this.getShell(), e1);
				}

			}
		});

		Button save = new Button(paramsContainer, SWT.NONE);
		save.setText("Save");
		save.setToolTipText("Save for later use");
		save.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				String filename;
				try {
					filename = EclipseGuiUtils.saveFileDialog(
							GeneratorParametersPage.this.getShell(),
							"Generator params", "*.generatorparams");
					if (filename != null) {
						java.util.Properties p = new java.util.Properties();
						for (Text text : paramsTexts) {
							p.put((String) text.getData("paramName"),
									text.getText());
						}
						p.store(new FileWriter(filename),
								"code2code generator params");
					}
				} catch (Exception e1) {
					EclipseGuiUtils.showErrorDialog(
							GeneratorParametersPage.this.getShell(), e1);
				}

			}
		});

		paramsContainer.pack();
		paramsContainer.setVisible(true);

		setPageComplete(true);

		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

	}

	private void createParamsTexts(Map<String, String> params) {
		for (Entry<String, String> entry : params.entrySet()) {

			String paramName = entry.getKey();

			Label label = new Label(paramsContainer, SWT.NULL);
			label.setText(paramName);

			GridData data = new GridData();
			data.verticalAlignment = SWT.TOP;
			label.setLayoutData(data);

			Text text = new Text(paramsContainer, SWT.BORDER | SWT.MULTI);

			text.setData("paramName", paramName);
			
			String value=entry.getValue();
			if (value!=null&&value.startsWith(TOOLTIP)){
				int p=value.indexOf("=", TOOLTIP.length()+1);
				final String description =value.substring(TOOLTIP.length(),p);
				text.setToolTipText(description);
				text.setToolTipText(String.format("%s (%s):",paramName,description));
				value=value.substring(p+1);
			}
			text.setText(value);
			

			text.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					try {
						getSelectedGenerator().setUserConfiguredParams(
								createParamsMap());
					} catch (Exception e1) {
						EclipseGuiUtils.showErrorDialog(container.getShell(),
								e1);
						throw new RuntimeException(e1);
					}
				}

			});

			paramsTexts.add(text);

			GridData data2 = new GridData();
			data2.widthHint = 600;
			data2.heightHint = 50;
			text.setLayoutData(data2);
		}
	}

	private Map<String, String> createParamsMap() {

		Map<String, String> paramsMap = new LinkedHashMap<String, String>();

		for (Text text : paramsTexts) {
			paramsMap.put((String) text.getData("paramName"), text.getText());
		}

		return paramsMap;
	}

	public Generator getSelectedGenerator() {
		return generatorSelectionPage.getSelectedGenerator();
	}

}
