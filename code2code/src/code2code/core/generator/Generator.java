package code2code.core.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class Generator {

	final private IFolder generatorFolder;

	final private DescriptionConfig descriptionConfig;
	final private GlobalParamsConfig globalParamsConfig;
	final private ParamsConfig paramsConfig;
	final private TemplatesConfig templatesConfig;

	final private List<Generator> nestedGenerators;

	final private List<Template> templates;
	final private List<Template> launchers;

	final private UserParams userParams;

	private TemplatesConfig launchersConfig;

	private Map<String, String> eclipseParams;

	private Generator(IFolder folder) throws Exception {
		generatorFolder = folder;

		globalParamsConfig = new GlobalParamsConfig(this);
		descriptionConfig = new DescriptionConfig(this, globalParamsConfig);
		paramsConfig = new ParamsConfig(this);
		templatesConfig = new TemplatesConfig(this);
		launchersConfig = new TemplatesConfig(this, "postactions");

		nestedGenerators = templatesConfig.getNestedGenerators();

		templates = calculateGeneratorTemplates();

		launchers = calculateGeneratorLaunchers();

		userParams = new UserParams(this, globalParamsConfig, paramsConfig,
				nestedGenerators);

	}

	public static Generator fromFolder(IFolder folder) throws Exception {
		return new Generator(folder);
	}

	private List<Template> calculateGeneratorTemplates() throws Exception {

		List<Template> templates = templatesConfig.getTemplates();

		for (Generator nestedGenerator : nestedGenerators) {
			templates.addAll(nestedGenerator.calculateGeneratorTemplates());
		}

		return templates;
	}

	private List<Template> calculateGeneratorLaunchers() throws Exception {

		List<Template> templates = launchersConfig.getTemplates();

		for (Generator nestedGenerator : nestedGenerators) {
			templates.addAll(nestedGenerator.calculateGeneratorLaunchers());
		}

		return templates;
	}

	public List<Template> calculateChoosenTemplatesToGenerate()
			throws Exception {

		List<Template> templatesToGenerate = new ArrayList<Template>();

		for (Template template : templates) {
			if (template.isSelectedToGenerate()) {
				templatesToGenerate.add(template);
			}
		}

		for (Template template : launchers) {
			if (template.isSelectedToGenerate()) {
				templatesToGenerate.add(template);
			}
		}

		return templatesToGenerate;
	}

	public IFolder getGeneratorFolder() {
		return generatorFolder;
	}

	public String getName() {
		return generatorFolder.getFullPath().removeFirstSegments(2)
				.removeFileExtension().toString();
	}

	public String getDescription() {
		return descriptionConfig.getDescription();
	}

	public Map<String, String> calculateRequiredParams() {
		return userParams.calculateGeneratorRequiredParams();
	}

	public Map<String, String> calculateContext() throws Exception {
		//3Zen
		Map<String,String> result = new HashMap<String,String>();
		result.putAll(calculateEclipseParams());
		result.putAll(userParams.translated());
		return result; //userParams.translated();
	}

	public void setUserConfiguredParams(Map<String, String> userConfiguredParams) {
		userParams.setUserConfiguredParams(userConfiguredParams);
	}

	public List<Template> getTemplates() {
		return templates;
	}

	public List<Template> getPostActions() {
		return launchers;
	}

	public Map<String, String> calculateEclipseParams() {
		if (eclipseParams == null) {
			eclipseParams = new HashMap<String, String>();
			IProject eclipse = getCurrentEclipseProject();
			if (eclipse != null) {
				final Map<String, String> params = (paramsConfig.getParams());
				if (!params.containsKey("eclipse_project_name")) {
					eclipseParams
							.put("eclipse_project_name", eclipse.getName());
				}
				if (!params.containsKey("eclipse_project_location")) {
					eclipseParams
							.put("eclipse_project_path", eclipse.getLocation().toFile().getAbsolutePath());
				}

			}
		}
		return eclipseParams;
	}

	private IProject getCurrentEclipseProject() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window
					.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IAdaptable) {
				return (IProject) ((IAdaptable) firstElement)
						.getAdapter(IProject.class);
			}
		}
		return null;
	}

}
