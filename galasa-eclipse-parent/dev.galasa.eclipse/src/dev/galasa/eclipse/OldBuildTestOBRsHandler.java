/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse;

/**
 * UI handler for the Build Test OBRs extension point.
 * 
 * Finds all Galasa projects (Maven project with a packaging type of bundle and
 * a dependency on dev.galasa:dev.galasa) <br>
 * and builds the OSGi Bundle Repository resource. OBR files are written to
 * [workspace]/.metadata/.plugin/[plugin-name].
 * 
 */
public class OldBuildTestOBRsHandler /* extends AbstractHandler */ {

//	private static String productName;
//	public static void setProductName(String productName) {
//		OldBuildTestOBRsHandler.productName = productName;
//	}
//
//	private static String processTitle;	
//	public static void setProcessTitle(String processTitle) {
//		OldBuildTestOBRsHandler.processTitle = processTitle;
//	}
//
//	private static final String OUTPUT_DIR_PROPERTY = "outputDir";
//
//	private static final String INCLUDE_SELF_PROPERTY = "includeSelf";
//
//	private static final String GALASA_GROUPID = "dev.galasa";
//
//	private static final String GALASA_ARTIFACT_ID = "dev.galasa";
//
//	private static final String MAVEN_BUILD_TEST_OBR_GOAL = "package " + GALASA_GROUPID + ":galasa-maven-plugin::obrresources";
//
//	private static final String MAVEN_NATURE = "org.eclipse.m2e.core.maven2Nature";
//
//	private static final String BUILD_FAILURE = "BUILD FAILURE";
//
//	private static final String BUILD_SUCCESS = "BUILD SUCCESS";
//
//	private static final String PROJECT = "Project";
//
//	private static final String MESSAGE_INFO = "[INFO] ";
//
//	private static final String MESSAGE_DEBUG = "[DEBUG] ";
//
//	private static final String MESSAGE_ERROR = "[ERROR] ";
//	
//	@Inject 
//	private Shell activeShell;
//
//	private static boolean debug;
//	public static void setDebug(boolean debugState) {
//		debug = debugState;
//	}
//	public static boolean isDebug() {
//		return debug;
//	}
//
//	private static PrintStream consoleOut;
//	public static void setConsoleOut(PrintStream consoleOut) {
//		OldBuildTestOBRsHandler.consoleOut = consoleOut;
//	}
//
//	private static PrintStream consoleOutRed;
//	public static void setConsoleOutRed(PrintStream consoleOutRed) {
//		OldBuildTestOBRsHandler.consoleOutRed = consoleOutRed;
//	}
//	
//	private static PrintStream consoleOutBlue;
//	public static void setConsoleOutBlue(PrintStream consoleOutBlue) {
//		OldBuildTestOBRsHandler.consoleOutBlue = consoleOutBlue;
//	}
//
//	private int maxProjectNameLength;	
//	public void setMaxProjectNameLength(int length) {
//		if (length == 0 || length > maxProjectNameLength) {
//			maxProjectNameLength = length;
//		}
//	}
//	
//	private static String jobState;
//	public static void setJobState(String jobState) {
//		OldBuildTestOBRsHandler.jobState = jobState;
//	}
//	public static String getJobState() {
//		return jobState;
//	}
//
//	private static boolean buildSuccess;
//	public static void setBuildSuccess(boolean buildSuccessState) {
//		buildSuccess = buildSuccessState;
//	}
//	public static boolean isBuildSuccess() {
//		return buildSuccess;
//	}
//
//	private String statusText = "";
//
//	private boolean isHeadless = false;
//
//	@Override
//	public Object execute(ExecutionEvent event) throws ExecutionException {
//		setBuildSuccess(true);
//		statusText = "";
//		
//		enableDebug(event);
//		
//		setHeadless(event);
//		
//		try {
//			setProductName(event.getCommand().getCategory().getName());
//			setProcessTitle(event.getCommand().getName());
//		} catch (NotDefinedException e) {
//			throw new ExecutionException("Error getting event command or category", e);
//		}
//		
//		activateMessageConsole();
//		
//		runBuildJob();
//		
//		return null;
//	}
//
//	/**
//	 * Run the request as a Job
//	 */
//	private void runBuildJob() {
//		Job job = new Job(processTitle) {
//
//			@Override
//			protected IStatus run(IProgressMonitor progressMonitor) {
//				IStatus runStatus = doMavenBuild(progressMonitor);
//				if (!isHeadless) {
//					syncWithUi(runStatus);
//				}
//
//				return runStatus;
//			}
//
//		};
//		job.setUser(true);
//		setJobState(null);
//		IJobChangeListener listener = new IJobChangeListener() {
//			
//			@Override
//			public void sleeping(IJobChangeEvent event) {
//				setJobState("sleeping");
//			}
//			
//			@Override
//			public void scheduled(IJobChangeEvent event) {
//				setJobState("scheduled");
//			}
//			
//			@Override
//			public void running(IJobChangeEvent event) {
//				setJobState("running");
//			}
//			
//			@Override
//			public void done(IJobChangeEvent event) {
//				setJobState("done");
//			}
//			
//			@Override
//			public void awake(IJobChangeEvent event) {
//				setJobState("awake");
//			}
//			
//			@Override
//			public void aboutToRun(IJobChangeEvent event) {
//				setJobState("aboutToRun");
//			}
//		};
//		job.addJobChangeListener(listener);
//		job.schedule();
//	}
//
//	/**
//	 * Run the Maven OBR builds
//	 * @param progressMonitor
//	 * @return
//	 */
//	private IStatus doMavenBuild(IProgressMonitor progressMonitor) {
//
//		List<IProject> projects = getProjectList(progressMonitor);
//		Map<String, String> results = new LinkedHashMap<>();
//
//		for (IProject project : projects) {
//			if (progressMonitor.isCanceled()) {
//				setBuildSuccess(false);
//				writeError(productName + " " + processTitle + " Cancelled");
//				return Status.CANCEL_STATUS;
//			}
//
//			writeInfo("Building project \"" + project.getName() + "\" ...\n");
//			try {	
//				// Create Maven request 
//				InvocationRequest invocationRequest = new DefaultInvocationRequest();
//				File baseDirectory = new File(project.getLocationURI());
//				invocationRequest.setBaseDirectory(baseDirectory);
//				String outputDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/.metadata/.plugin/" + project.getProject().getName();
//				Properties invocationRequestProperties = new Properties();
//				invocationRequestProperties.put(OUTPUT_DIR_PROPERTY, outputDirectory);
//				invocationRequestProperties.put(INCLUDE_SELF_PROPERTY, "true");
//				invocationRequest.setProperties(invocationRequestProperties);
//				Boolean batchMode = true;
//				invocationRequest.setBatchMode(batchMode);
//				invocationRequest.setDebug(debug);
//				invocationRequest.setShowErrors(debug);
//				List<String> goals = new ArrayList<>();
//				goals.add(MAVEN_BUILD_TEST_OBR_GOAL);
//				invocationRequest.setGoals(goals);
//				writeDebug("  Maven configuration:");						
//				writeDebug("    base directory set to " + baseDirectory.getAbsolutePath());
//				writeDebug("    outputDirectory set to " + new File(outputDirectory).getAbsolutePath());
//				writeDebug("    batch mode set to " + batchMode);
//				writeDebug("    show errors set to " + batchMode);
//				writeDebug("    goals set to " + goals);
//
//				Invoker invoker = new DefaultInvoker();
//				invoker.setOutputHandler(new PrintStreamHandler(consoleOutBlue, true));
//				InvocationResult invocationResult = invoker.execute(invocationRequest);
//				CommandLineException commandLineException = invocationResult.getExecutionException();
//				if (commandLineException != null) {
//					setBuildSuccess(false);
//					commandLineException.printStackTrace(consoleOutRed);
//					results.put(project.getName(), BUILD_FAILURE + " - " + commandLineException.getMessage());
//				} else {
//					int exitCode = invocationResult.getExitCode();
//					if (exitCode == 0) {
//						writeInfo(BUILD_SUCCESS + ": " + PROJECT + " \"" + project.getName() + "\"\n");
//						results.put(project.getName(), BUILD_SUCCESS);
//					} else {
//						setBuildSuccess(false);
//						writeError(BUILD_FAILURE + ": " + PROJECT + " \"" + project.getName() + "\"" + " exit code " + exitCode + "\n");
//						results.put(project.getName(), BUILD_FAILURE + " - exit code " + exitCode);
//					}
//				}
//			} catch (MavenInvocationException e) {
//				setBuildSuccess(false);
//				writeError(BUILD_FAILURE + ": " + PROJECT + " \"" + project.getName() + "\"", e);
//				results.put(project.getName(), BUILD_FAILURE + " - " + e.getMessage());
//			}
//		}
//		
//		return reportResults(results);
//	}
//
//	/**
//	 * Report the build results
//	 * @param results
//	 * @return
//	 */
//	private IStatus reportResults(Map<String, String> results) {
//		if (results.size() > 0) {
//			writeInfo(StringUtils.rightPad("", 100, "-"));
//			writeInfo(" " + productName + " " + processTitle + " results:");
//			writeInfo("");
//			writeInfo(" " + StringUtils.rightPad(PROJECT, maxProjectNameLength + 1) + "Result");
//			writeInfo(" " + StringUtils.rightPad("", 100, "-"));
//			for (Map.Entry<String, String> resultsEntry : results.entrySet()) {
//				if (resultsEntry.getValue().startsWith(BUILD_SUCCESS)) {
//					writeInfo(" " + StringUtils.rightPad(resultsEntry.getKey(), maxProjectNameLength + 1) + resultsEntry.getValue());
//				} else {
//					writeError(StringUtils.rightPad(resultsEntry.getKey(), maxProjectNameLength + 1) + resultsEntry.getValue());
//				}
//			}
//		} else {
//			writeError(productName + " " + processTitle + " Cancelled - No " + productName + " projects found");
//			statusText = "No " + productName + " projects found";
//			setBuildSuccess(false);
//			return Status.CANCEL_STATUS;
//		}
//		return Status.OK_STATUS;		
//	}
//
//	/**
//	 * Return a list of Galasa Test projects
//	 * @param progressMonitor
//	 * @return project list
//	 */
//	private List<IProject> getProjectList(IProgressMonitor progressMonitor) {
//		setMaxProjectNameLength(0);
//		writeInfo("Finding " + productName + " test projects ...\n");
//		
//		// Get the list of workspace projects
//		List<IProject> projectList = new LinkedList<>();
//		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
//		IProject[] workspaceProjects = workspaceRoot.getProjects();
//		for (IProject workspaceProject : workspaceProjects) {
//			writeDebug("Checking project \"" + workspaceProject.getName() + "\"");
//			
//			if (!workspaceProject.isOpen()) {
//				writeInfo(PROJECT + " \"" + workspaceProject.getName() + "\" is closed and will be not built.");
//			} else if (!isMavenBundle(workspaceProject)) {
//				writeInfo(PROJECT + " \"" + workspaceProject.getName() + "\" is not a Maven Bundle project and will not be built.");
//			} else if (!hasGalasaDependency(workspaceProject, progressMonitor)) {
//				writeInfo(PROJECT + " \"" + workspaceProject.getName() + "\" does not have a Maven dependency on " + GALASA_GROUPID + ":" + GALASA_ARTIFACT_ID + " and will not be built.");
//			} else {
//				projectList.add(workspaceProject);
//				setMaxProjectNameLength(workspaceProject.getName().length());
//				writeInfo(PROJECT + " \"" + workspaceProject.getName() + "\" is a " + productName + " test project and will be built.");
//			}
//		}	
//		return projectList;
//	}
//
//	/**
//	 * Returns true if project is a Maven project
//	 * @param workspaceProject
//	 * @return
//	 */
//	private static boolean isMavenBundle(IProject workspaceProject) {
//		try {
//			return (workspaceProject.hasNature(MAVEN_NATURE) && workspaceProject.hasNature(IBundleProjectDescription.PLUGIN_NATURE));
//		} catch (CoreException e) {
//			writeError("Unable to confirm project natures", e);
//			return false;
//		}
//	}
//
//	/**
//	 * Returns true if project has Galasa Maven Dependency
//	 * @param workspaceProject
//	 * @param progressMonitor
//	 * @return
//	 */
//	private boolean hasGalasaDependency(IProject workspaceProject, IProgressMonitor progressMonitor) {
//
//		IMavenProjectFacade mavenProjectFacade = MavenPlugin.getMavenProjectRegistry().getProject(workspaceProject);
//		MavenProject mavenProject = null;
//		if (mavenProjectFacade != null) {
//			try {
//				mavenProject = mavenProjectFacade.getMavenProject(progressMonitor);
//			} catch (CoreException e) {
//				writeError("Unable to get MavenProject object for project \"" + workspaceProject.getName() + "\". Does project need rebuilding? OBR for Project will not be built.", e);
//			}
//		}
//		if (mavenProjectFacade == null || mavenProject == null) {
//			writeError("Unable to get MavenProject object of \"" + workspaceProject.getName() + "\". Does project need rebuilding? OBR for Project will not be built.");
//			return false;
//		}
//		
//		boolean hasGalasaDependency = false;
//		Model mavenModel = mavenProject.getModel();
//		List<Dependency> dependancies = mavenModel.getDependencies();
//		writeDebug("    Dependencies:");
//		for (Dependency dependecy : dependancies) {
//			writeDebug("      " + dependecy.getGroupId() + ":" + dependecy.getArtifactId() + ":" + dependecy.getVersion());
//			if (dependecy.getGroupId().equals(GALASA_GROUPID) && dependecy.getArtifactId().equals(GALASA_ARTIFACT_ID)) {
//				hasGalasaDependency = true;
//			}
//		}
//		return hasGalasaDependency;
//	}
//
//	/**
//	 * Notify user at job end
//	 * @param runStatus
//	 */
//	private void syncWithUi(IStatus runStatus) {
//		Runnable runnable = () -> {
//			if (runStatus.getSeverity() == IStatus.CANCEL) {
//				if (statusText.isEmpty()) {
//					MessageDialog.openError(activeShell, productName + " - " + processTitle, processTitle + " cancelled");
//				} else {
//					MessageDialog.openError(activeShell, productName + " - " + processTitle, processTitle + " cancelled - " + statusText);
//				}
//				return;
//			}
//			if (buildSuccess) {
//				MessageDialog.openInformation(activeShell, productName + " - " + processTitle, processTitle + " complete - " + BUILD_SUCCESS);
//			} else {
//				MessageDialog.openError(activeShell, productName + " - " + processTitle, processTitle + " complete - " + BUILD_FAILURE + "\n\nSee console log for details");
//			}
//		};
//		Display.getDefault().asyncExec(runnable);
//	}
//
//	/**
//	 * Activate message console
//	 */
//	private void activateMessageConsole() {
//		MessageConsole messageConsole = null;
//		String consoleName = productName + " " + processTitle;
//		
//		// Look for existing console
//		ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
//		IConsoleManager consoleManager = consolePlugin.getConsoleManager();
//		IConsole[] existingConsoles = consoleManager.getConsoles();
//		for (IConsole existingConsole : existingConsoles) {
//			if (existingConsole.getName().equals(consoleName)) {
//				messageConsole = (MessageConsole) existingConsole;
//				break;
//			}
//		}
//		
//		// Not found, create a new one
//		if (messageConsole == null) {
//			messageConsole = new MessageConsole(consoleName, null);
//			consoleManager.addConsoles(new IConsole[] { messageConsole });
//		}
//	
//		// Clear and activate console
//		messageConsole.clearConsole();
//		messageConsole.activate();
//		
//		// Create the default PrintStream
//		MessageConsoleStream  messageConsoleStreamDefault = messageConsole.newMessageStream();
//		messageConsoleStreamDefault.setColor(null);
//		setConsoleOut(new PrintStream(messageConsoleStreamDefault, true));
//	
//		// Create a PrintStream for Red text
//		MessageConsoleStream  messageConsoleStreamRed = messageConsole.newMessageStream();
//		messageConsoleStreamRed.setColor(new Color(null, new RGB(255, 0, 0)));
//		setConsoleOutRed(new PrintStream(messageConsoleStreamRed, true));
//	
//		// Create a PrintStream for Blue text
//		MessageConsoleStream  messageConsoleStreamBlue = messageConsole.newMessageStream();
//		messageConsoleStreamBlue.setColor(new Color(null, new RGB(0, 0, 255)));
//		setConsoleOutBlue(new PrintStream(messageConsoleStreamBlue, true));
//	}
//
//	/**
//	 * Has debug been requested
//	 * @return
//	 */
//	private void enableDebug(ExecutionEvent event) {		
//		if (event.getCommand().getId().endsWith("Debug")) {
//			setDebug(true);
//			return;
//		}
//		setDebug(false);
//	}
//
//	/**
//	 * Set headless
//	 * @return
//	 */
//	private void setHeadless(ExecutionEvent event) {		
//		if (event.getCommand().getId().endsWith("Headless")) {
//			isHeadless  = true;
//		}
//	}
//
//	/**
//	 * Write an info message to the console
//	 * @param message
//	 */
//	private static void writeInfo(String message) {
//		consoleOut.println(MESSAGE_INFO + message);
//	}
//
//	/**
//	 * Write a debug message to the console
//	 * @param message
//	 */
//	private void writeDebug(String message) {
//		if (isDebug()) {
//			consoleOut.println(MESSAGE_DEBUG + message);
//		}
//	}
//
//	/**
//	 * Write an error message to the console
//	 * @param message
//	 */
//	private static void writeError(String message) {
//		consoleOutRed.println(MESSAGE_ERROR + message);
//	}
//
//	/**
//	 * Write an error message to the console
//	 * @param message
//	 * @param e
//	 */
//	private static void writeError(String message, Exception e) {
//		consoleOutRed.println(MESSAGE_ERROR + message);
//		e.printStackTrace(consoleOutRed);
//	}
}
