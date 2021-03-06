package code2code.ui.wizards.generate;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import code2code.core.generator.Generator;
import code2code.core.generator.Template;
import code2code.utils.Console;
import code2code.utils.EclipseGuiUtils;
import code2code.utils.FileUtils;

public class GenerateFilesWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	private GeneratorSelectionPage generatorSelectionPage;
	private GeneratorParametersPage generatorParametersPage;
	private IWorkbench workbench;
	private GenerationCustomizationPage generationCustomizationPage;
	private GenerationPostActionsPage generationPostActionsPage;

	@Override
	public boolean performFinish() {

		Generator selectedGenerator = generatorSelectionPage
				.getSelectedGenerator();

		if (selectedGenerator == null) {
			return false;
		}

		try {
			Console.write("Processing generator: "
					+ selectedGenerator.getName());

			IProject project = selectedGenerator.getGeneratorFolder()
					.getProject();

			List<IFile> projectfiles = new ArrayList<IFile>();
			for (Template template : selectedGenerator
					.calculateChoosenTemplatesToGenerate()) {

				String destination = template.calculateDestination();

				if (destination.equals("")) {
					Console.write("Generating " + template.getTemplateName()
							+ " to console:");
					Console.write("-------------------------------------------------------------");
					Console.write(FileUtils.read(template.calculateResult()));
					Console.write("-------------------------------------------------------------");
				} else {
					Path destinationPath = new Path(destination);

					IFile file = project.getFile(destinationPath);

					if (project.exists(destinationPath)) {
						if (!template.isOverwriteAllowed()
								&& !projectfiles.contains(file)) {

							Console.write("File already exists. Skipping: "
									+ destinationPath);
							continue;
						} else {
							Console.write("File already exists. Overwriting: "
									+ destinationPath);

						}
					}

					Console.write("Generating: " + template.getTemplateName()
							+ " to " + destination);

					IContainer parent = file.getParent();
					while (parent != null) {
						if (parent instanceof IProject) {
							IProject p = (IProject) parent;
							boolean exists = p.exists();
							if (!exists) {
								p.create(null);
							}
							if (!p.isOpen()) {
								p.open(null);
							}
							if (!exists) {
								// we must indicate if .project must be
								// overwrited
								IFile f = p.getFile(".project");
								projectfiles.add(f);
							}
						}
						parent = parent.getParent();
					}
					FileUtils.createParentFolders(file);

					if (file.exists()) {
						file.setContents(template.calculateResult(), false,
								true, null);
					} else {
						file.create(template.calculateResult(), false, null);
					}

				}

			}

			Console.write("Done");

		} catch (Exception e) {
			try {
				Console.write("An error ocurred. See Error Log for details");
			} catch (Exception e2) {
				EclipseGuiUtils.showErrorDialog(workbench
						.getActiveWorkbenchWindow().getShell(), e2);
				throw new RuntimeException(e2);
			}
			EclipseGuiUtils.showErrorDialog(workbench
					.getActiveWorkbenchWindow().getShell(), e);
			throw new RuntimeException(e);
		} finally {
			Console.disposeConsole();
		}

		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public void addPages() {

		if (selection.size() == 0) {
			MessageDialog.openError(workbench.getActiveWorkbenchWindow()
					.getShell(), "Ops", "Ops... No Project Selected");
			throw new IllegalStateException(
					"Ops... a project should be selected");
		}

		IProject project = ((IResource) ((IAdaptable) selection
				.getFirstElement()).getAdapter(IResource.class)).getProject();

		generatorSelectionPage = new GeneratorSelectionPage(project);
		generatorParametersPage = new GeneratorParametersPage(
				generatorSelectionPage);
		generationCustomizationPage = new GenerationCustomizationPage(
				generatorParametersPage);
		generationPostActionsPage = new GenerationPostActionsPage(generatorParametersPage);

		addPage(generatorSelectionPage);
		addPage(generatorParametersPage);
		addPage(generationCustomizationPage);
		addPage(generationPostActionsPage);

	}

}
